/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.barriers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.distribution.barrier.Barrier.State;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.distribution.barrier.Barrier.builder;
import static io.harness.distribution.barrier.Forcer.State.ABANDONED;
import static io.harness.distribution.barrier.Forcer.State.APPROACHING;
import static io.harness.distribution.barrier.Forcer.State.ARRIVED;
import static io.harness.distribution.barrier.Forcer.State.TIMED_OUT;
import static io.harness.govern.Switch.unhandled;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.ASYNC_WAITING;
import static io.harness.pms.contracts.execution.Status.EXPIRED;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.distribution.barrier.Barrier;
import io.harness.distribution.barrier.BarrierId;
import io.harness.distribution.barrier.ForceProctor;
import io.harness.distribution.barrier.Forcer;
import io.harness.distribution.barrier.ForcerId;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.IteratorConfig;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.NodeProjectionUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.BarrierNodeRepository;
import io.harness.springdata.HMongoTemplate;
import io.harness.springdata.PersistenceUtils;
import io.harness.steps.barriers.BarrierSpecParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierExecutionInstance.BarrierExecutionInstanceKeys;
import io.harness.steps.barriers.beans.BarrierPositionInfo;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionKeys;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionType;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.barriers.beans.BarrierResponseData.BarrierError;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.beans.StageDetail.StageDetailKeys;
import io.harness.steps.barriers.service.visitor.BarrierVisitor;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
@Slf4j
public class BarrierServiceImpl implements BarrierService, ForceProctor {
  private static final String LEVEL = "level";
  private static final String PLAN = "plan";
  private static final String STAGE = "stage";
  private static final String STEP_GROUP = "stepGroup";
  private static final String STEP = "step";
  private static final String PLAN_EXECUTION_ID = "planExecutionId";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;

  public void registerIterators(IteratorConfig config) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("PmsBarrierExecutionInstanceMonitor")
            .poolSize(config.getThreadPoolCount())
            .interval(ofSeconds(config.getTargetIntervalInSeconds()))
            .build(),
        BarrierService.class,
        MongoPersistenceIterator.<BarrierExecutionInstance, SpringFilterExpander>builder()
            .clazz(BarrierExecutionInstance.class)
            .fieldName(BarrierExecutionInstanceKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this::update)
            .filterExpander(
                query -> query.addCriteria(Criteria.where(BarrierExecutionInstanceKeys.barrierState).in(STANDING)))
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance) {
    return barrierNodeRepository.save(barrierExecutionInstance);
  }

  @Override
  public List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances) {
    return (List<BarrierExecutionInstance>) barrierNodeRepository.saveAll(barrierExecutionInstances);
  }

  @Override
  public BarrierExecutionInstance get(String barrierUuid) {
    return barrierNodeRepository.findById(barrierUuid)
        .orElseThrow(() -> new InvalidRequestException("Barrier not found for id: " + barrierUuid));
  }

  @Override
  public BarrierExecutionInstance findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId) {
    return barrierNodeRepository.findByIdentifierAndPlanExecutionId(identifier, planExecutionId);
  }

  @Override
  public BarrierExecutionInstance findByIdentifierAndPlanExecutionIdAndStrategyExecutionId(String identifier, String planExecutionId, String strategyExecutionId) {
    return barrierNodeRepository.findByIdentifierAndPlanExecutionIdAndStrategyExecutionId(identifier, planExecutionId, strategyExecutionId);
  }

  @Override
  public List<BarrierExecutionInstance> findManyByPlanExecutionIdAndSetupInfo_StrategySetupId(String planExecutionId, String strategySetupId) {
    return barrierNodeRepository.findManyByPlanExecutionIdAndSetupInfo_StrategySetupId(planExecutionId, strategySetupId);
  }

  @Override
  public BarrierExecutionInstance findByPlanNodeIdAndPlanExecutionId(String planNodeId, String planExecutionId) {
    Criteria positionCriteria = Criteria.where(BarrierExecutionInstanceKeys.positions)
                                    .elemMatch(Criteria.where(BarrierPositionKeys.stepSetupId).is(planNodeId));
    Criteria planExecutionIdCriteria = Criteria.where(BarrierExecutionInstanceKeys.planExecutionId).is(planExecutionId);

    Query query = query(planExecutionIdCriteria.andOperator(positionCriteria));
    return mongoTemplate.findOne(query, BarrierExecutionInstance.class);
  }

  @Override
  public BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance) {
    if (barrierExecutionInstance.getBarrierState() != STANDING) {
      return barrierExecutionInstance;
    }

//    if (Boolean.TRUE.equals(barrierExecutionInstance.getSetupInfo().getIsWithinMatrix())) {
//      barrierExecutionInstance.getPositionInfo().setBarrierPositionList(new ArrayList<>());
//      // maybe need to update barrier position list
//      List<NodeExecution> barrierNodes = nodeExecutionService.getAllBarriersNodes(barrierExecutionInstance.getPlanExecutionId());
//      for (NodeExecution barrierNode : barrierNodes) {
//        StepElementParameters stepElementParameters =
//                RecastOrchestrationUtils.fromMap(barrierNode.getResolvedStepParameters(), StepElementParameters.class);
//        BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();
//        if (barrierExecutionInstance.getIdentifier().equals(barrierSpecParameters.getBarrierRef())) {
//          BarrierPositionInfo.BarrierPosition.BarrierPositionBuilder  barrierPositionBuilder =  BarrierPositionInfo.BarrierPosition.builder();
//          for (io.harness.pms.contracts.ambiance.Level level : barrierNode.getAmbiance().getLevelsList()) {
//            if ("STAGE".equals(level.getStepType().getStepCategory().toString())) {
//              barrierPositionBuilder.stageSetupId(level.getSetupId());
//              barrierPositionBuilder.stageRuntimeId(level.getRuntimeId());
//              barrierPositionBuilder.stepGroupRollback(false);
//            }
//            if ("STEP_GROUP".equals(level.getStepType().getType())) {
//              barrierPositionBuilder.stepGroupSetupId(level.getSetupId());
//              barrierPositionBuilder.stepGroupRuntimeId(level.getRuntimeId());
//            }
//            if ("Barrier".equals(level.getStepType().getType())) {
//              barrierPositionBuilder.stepSetupId(level.getSetupId());
//              barrierPositionBuilder.stepRuntimeId(level.getRuntimeId());
//            }
//          }
//          barrierExecutionInstance.getPositionInfo().getBarrierPositionList().add(barrierPositionBuilder.build());
//        }
//      }
//    }

    Forcer forcer = buildForcer(barrierExecutionInstance);

    Barrier barrier = builder().id(new BarrierId(barrierExecutionInstance.getUuid())).forcer(forcer).build();
    State state = barrier.pushDown(this);

    switch (state) {
      case STANDING:
        return barrierExecutionInstance;
      case DOWN:
        log.info("The barrier [{}] is down", barrierExecutionInstance.getUuid());
        waitNotifyEngine.doneWith(
            barrierExecutionInstance.getUuid(), BarrierResponseData.builder().failed(false).build());
        break;
      case ENDURE:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getUuid(),
            BarrierResponseData.builder()
                .failed(true)
                .barrierError(BarrierError.builder().timedOut(false).errorMessage("The barrier was abandoned").build())
                .build());
        break;
      case TIMED_OUT:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getUuid(),
            BarrierResponseData.builder()
                .failed(true)
                .barrierError(BarrierError.builder().timedOut(true).errorMessage("The barrier timed out").build())
                .build());
        break;
      default:
        unhandled(state);
    }

    return HMongoTemplate.retry(() -> updateState(barrierExecutionInstance.getUuid(), state));
  }

  @Override
  public BarrierExecutionInstance updateState(String uuid, State state) {
    Query query = new Query(Criteria.where(BarrierExecutionInstanceKeys.uuid).is(uuid));
    Update update = new Update().set(BarrierExecutionInstanceKeys.barrierState, state);

    return mongoTemplate.findAndModify(query, update, BarrierExecutionInstance.class);
  }

  @Override
  public List<BarrierExecutionInstance> updatePosition(
      String planExecutionId, BarrierPositionType positionType, String positionSetupId, String positionExecutionId, String stageExecutionId, String stepGroupExecutionId) {
    List<BarrierExecutionInstance> barrierExecutionInstances =
        findByPosition(planExecutionId, positionType, positionSetupId);

    Update update = obtainRuntimeIdUpdate(positionType, positionSetupId, positionExecutionId, stageExecutionId, stepGroupExecutionId);
    String strategyNodeTypeToExclude;
    switch (positionType) {
      case STAGE:
        strategyNodeTypeToExclude = "stage";
        break;
      case STEP_GROUP:
        strategyNodeTypeToExclude = "stepGroup";
        break;
      default:
        strategyNodeTypeToExclude = positionType.name();
        break;
    }

    // mongo does not support multiple documents atomic update, let's update one by one
    barrierExecutionInstances.forEach(instance
        -> HMongoTemplate.retry(
            ()
                -> mongoTemplate.findAndModify(
                    query(Criteria.where(BarrierExecutionInstanceKeys.uuid)
                              .is(instance.getUuid())
                              .andOperator(obtainBarrierPositionCriteria(positionType, positionSetupId))
                              .and("setupInfo.strategyNodeType").ne(strategyNodeTypeToExclude)),
                    update, BarrierExecutionInstance.class)));
    return barrierExecutionInstances;
  }

  @Override
  public void upsert(
          BarrierExecutionInstance barrierExecutionInstance) {
    HMongoTemplate.retry(
            () -> mongoTemplate.upsert(
                    query(Criteria
                            .where(BarrierExecutionInstanceKeys.identifier).is(barrierExecutionInstance.getIdentifier())
                            .and(BarrierExecutionInstanceKeys.planExecutionId).is(barrierExecutionInstance.getPlanExecutionId())
                            .and("setupInfo.strategySetupId").is(barrierExecutionInstance.getSetupInfo().getStrategySetupId())
                    ), new Update()
                            .set(BarrierExecutionInstanceKeys.name, barrierExecutionInstance.getName())
                            .set(BarrierExecutionInstanceKeys.identifier, barrierExecutionInstance.getIdentifier())
                            .set(BarrierExecutionInstanceKeys.planExecutionId, barrierExecutionInstance.getPlanExecutionId())
                            .set(BarrierExecutionInstanceKeys.barrierState, STANDING)
                            .set(BarrierExecutionInstanceKeys.strategyExecutionId, barrierExecutionInstance.getStrategyExecutionId())
                            .set(BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfo.BarrierSetupInfoKeys.name, barrierExecutionInstance.getSetupInfo().getName())
                            .set(BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfo.BarrierSetupInfoKeys.identifier, barrierExecutionInstance.getSetupInfo().getIdentifier())
                            .addToSet(BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfo.BarrierSetupInfoKeys.stages).each(barrierExecutionInstance.getSetupInfo().getStages())
                            .set(BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfo.BarrierSetupInfoKeys.strategySetupId, barrierExecutionInstance.getSetupInfo().getStrategySetupId())
                            .set(BarrierExecutionInstanceKeys.setupInfo + "." + BarrierSetupInfo.BarrierSetupInfoKeys.strategyNodeType, barrierExecutionInstance.getSetupInfo().getStrategyNodeType())
                            .set(BarrierExecutionInstanceKeys.positionInfo + "." + BarrierPositionInfo.BarrierPositionInfoKeys.planExecutionId, barrierExecutionInstance.getPositionInfo().getPlanExecutionId())
                            .addToSet(BarrierExecutionInstanceKeys.positionInfo + "." + BarrierPositionInfo.BarrierPositionInfoKeys.barrierPositionList).each(barrierExecutionInstance.getPositionInfo().getBarrierPositionList()),
                    BarrierExecutionInstance.class));
  }

  @Override
  public void updateManyPlanExecutionId(String setupId, String executionId) {
    HMongoTemplate.retry(
            () -> mongoTemplate.updateMulti(
                    query(Criteria.where(BarrierExecutionInstanceKeys.planExecutionId).is(setupId)
                    ), new Update()
                            .set(BarrierPositionInfo.BarrierPositionInfoKeys.planExecutionId, executionId)
                            .set(BarrierExecutionInstanceKeys.positionInfo + "." + BarrierPositionInfo.BarrierPositionInfoKeys.planExecutionId, executionId),
                    BarrierExecutionInstance.class));
  }

  @Override
  public void updateBarrierWithinStrategy(String barrierIdentifier, String planExecutionId, String strategySetupId, List<BarrierPositionInfo.BarrierPosition> barrierPositions, String strategyExecutionId) {
    HMongoTemplate.retry(
            () -> mongoTemplate.findAndModify(
                    query(Criteria
                            .where(BarrierExecutionInstanceKeys.identifier).is(barrierIdentifier)
                            .and(BarrierExecutionInstanceKeys.planExecutionId).is(planExecutionId)
                            .and("setupInfo.strategySetupId").is(strategySetupId)
                    ), new Update()
                            .set(BarrierExecutionInstanceKeys.positionInfo + "." + BarrierPositionInfo.BarrierPositionInfoKeys.barrierPositionList, barrierPositions)
                            .set(BarrierExecutionInstanceKeys.strategyExecutionId, strategyExecutionId),
                    BarrierExecutionInstance.class));
  }

  private Update obtainRuntimeIdUpdate(
      BarrierPositionType positionType, String positionSetupId, String positionExecutionId, String stageExecutionId, String stepGroupExecutionId) {
    String position = "position";
    final String positions = BarrierExecutionInstanceKeys.positions + ".$[" + position + "].";
    Update update;
    switch (positionType) {
      case STAGE:
        update =
            new Update()
                .set(positions.concat(BarrierPositionKeys.stageRuntimeId), positionExecutionId)
                .filterArray(
                    Criteria.where(position.concat(".").concat(BarrierPositionKeys.stageSetupId)).is(positionSetupId));
        break;
      case STEP_GROUP:
        update = new Update()
                     .set(positions.concat(BarrierPositionKeys.stepGroupRuntimeId), positionExecutionId)
                     .filterArray(Criteria.where(position.concat(".").concat(BarrierPositionKeys.stepGroupSetupId))
                                      .is(positionSetupId));
        break;
      case STEP:
        update =
            new Update()
                .set(positions.concat(BarrierPositionKeys.stepRuntimeId), positionExecutionId)
                .filterArray(
                    Criteria.where(position.concat(".").concat(BarrierPositionKeys.stepSetupId)).is(positionSetupId)
                            .and(position.concat(".").concat(BarrierPositionKeys.stageRuntimeId)).is(stageExecutionId)
                            .and(position.concat(".").concat(BarrierPositionKeys.stepGroupRuntimeId)).is(stepGroupExecutionId));
        break;
      default:
        throw new InvalidRequestException(String.format("%s position type is not implemented", positionType));
    }

    return update;
  }

  /**
   * Barrier works with 4 forcers : Plan -> Stage -> Step Group -> Barrier Node
   */
  private Forcer buildForcer(BarrierExecutionInstance barrierExecutionInstance) {
    final String planExecutionId = barrierExecutionInstance.getPlanExecutionId();

    return Forcer.builder()
        .id(new ForcerId(barrierExecutionInstance.getPlanExecutionId()))
        .metadata(ImmutableMap.of(LEVEL, PLAN))
        .children(barrierExecutionInstance.getPositionInfo()
                      .getBarrierPositionList()
                      .stream()
                      .map(position -> {
                        final Forcer step =
                            Forcer.builder()
                                .id(new ForcerId(position.getStepRuntimeId()))
                                .metadata(ImmutableMap.of(LEVEL, STEP, PLAN_EXECUTION_ID, planExecutionId))
                                .build();
                        final Forcer stepGroup =
                            Forcer.builder()
                                .id(new ForcerId(position.getStepGroupRuntimeId()))
                                .metadata(ImmutableMap.of(LEVEL, STEP_GROUP, PLAN_EXECUTION_ID, planExecutionId))
                                .children(Collections.singletonList(step))
                                .build();
                        boolean isStepGroupPresent =
                            EmptyPredicate.isNotEmpty(stepGroup.getId().getValue()) && !position.isStepGroupRollback();
                        return Forcer.builder()
                            .id(new ForcerId(position.getStageRuntimeId()))
                            .metadata(ImmutableMap.of(LEVEL, STAGE, PLAN_EXECUTION_ID, planExecutionId))
                            .children(isStepGroupPresent ? Collections.singletonList(stepGroup)
                                                         : Collections.singletonList(step))
                            .build();
                      })
                      .collect(Collectors.toList()))
        .build();
  }

  @Override
  public Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata) {
    Status status;
    if (PLAN.equals(metadata.get(LEVEL))) {
      PlanExecution planExecution;
      try {
        status = planExecutionService.getStatus(forcerId.getValue());
      } catch (InvalidRequestException e) {
        log.warn("Plan Execution was not found. State set to APPROACHING", e);
        return APPROACHING;
      }

      if (StatusUtils.positiveStatuses().contains(status)) {
        return ARRIVED;
      } else if (StatusUtils.brokeStatuses().contains(status) || status == ABORTED) {
        return ABANDONED;
      }
    } else {
      NodeExecution forcerNode =
          nodeExecutionService.getWithFieldsIncluded(forcerId.getValue(), NodeProjectionUtils.withStatus);
      status = forcerNode.getStatus();
    }

    if (StatusUtils.positiveStatuses().contains(status)) {
      return ARRIVED;
    } else if (status == EXPIRED) {
      return TIMED_OUT;
    } else if (StatusUtils.finalStatuses().contains(status)) {
      return ABANDONED;
    }

    if (STEP.equals(metadata.get(LEVEL)) && (status == ASYNC_WAITING)) {
      return ARRIVED;
    }

    return APPROACHING;
  }

  @Override
  public List<BarrierExecutionInstance> findByStageIdentifierAndPlanExecutionIdAnsStateIn(
      String stageIdentifier, String planExecutionId, Set<State> stateSet) {
    Criteria planExecutionIdCriteria = Criteria.where(BarrierExecutionInstanceKeys.planExecutionId).is(planExecutionId);
    Criteria stageIdentifierCriteria = Criteria.where(BarrierExecutionInstanceKeys.stages)
                                           .elemMatch(Criteria.where(StageDetailKeys.identifier).is(stageIdentifier));

    Query query = query(new Criteria().andOperator(planExecutionIdCriteria, stageIdentifierCriteria));

    if (!stateSet.isEmpty()) {
      query.addCriteria(where(BarrierExecutionInstanceKeys.barrierState).in(stateSet));
    }

    return mongoTemplate.find(query, BarrierExecutionInstance.class);
  }

  @VisibleForTesting
  protected List<BarrierExecutionInstance> findByPosition(
      String planExecutionId, BarrierPositionType positionType, String positionSetupId) {
    Criteria planExecutionIdCriteria = Criteria.where(BarrierExecutionInstanceKeys.planExecutionId).is(planExecutionId);

    Query query = query(new Criteria().andOperator(
        planExecutionIdCriteria, obtainBarrierPositionCriteria(positionType, positionSetupId)));

    return mongoTemplate.find(query, BarrierExecutionInstance.class);
  }

  private Criteria obtainBarrierPositionCriteria(BarrierPositionType positionType, String positionSetupId) {
    Criteria positionCriteria;
    switch (positionType) {
      case STAGE:
        positionCriteria = Criteria.where(BarrierExecutionInstanceKeys.positions)
                               .elemMatch(Criteria.where(BarrierPositionKeys.stageSetupId).is(positionSetupId));
        break;
      case STEP_GROUP:
        positionCriteria = Criteria.where(BarrierExecutionInstanceKeys.positions)
                               .elemMatch(Criteria.where(BarrierPositionKeys.stepGroupSetupId).is(positionSetupId));
        break;
      case STEP:
        positionCriteria = Criteria.where(BarrierExecutionInstanceKeys.positions)
                               .elemMatch(Criteria.where(BarrierPositionKeys.stepSetupId).is(positionSetupId));
        break;
      default:
        throw new InvalidRequestException(String.format("%s position type is not implemented", positionType));
    }

    return positionCriteria;
  }

  @Override
  public BarrierVisitor getBarrierInfo(String yaml) {
    try {
      YamlNode yamlNode = YamlUtils.extractPipelineField(yaml).getNode();
      BarrierVisitor barrierVisitor = new BarrierVisitor(injector);
      barrierVisitor.walkElementTree(yamlNode);
      return barrierVisitor;
//      return new ArrayList<>(barrierVisitor.getBarrierIdentifierMap().values());
    } catch (IOException e) {
      log.error("Error while extracting yaml");
      throw new InvalidRequestException("Error while extracting yaml");
    } catch (InvalidRequestException e) {
      log.error("Error while processing yaml");
      throw e;
    }
  }

//  @Override
//  public Map<String, List<BarrierPositionInfo.BarrierPosition>> getBarrierPositionInfoList(String yaml) {
//    try {
//      YamlNode yamlNode = YamlUtils.extractPipelineField(yaml).getNode();
//      BarrierVisitor barrierVisitor = new BarrierVisitor(injector);
//      barrierVisitor.walkElementTree(yamlNode);
//      return barrierVisitor.getBarrierPositionInfoMap();
//    } catch (IOException e) {
//      log.error("Error while extracting yaml");
//      throw new InvalidRequestException("Error while extracting yaml");
//    } catch (InvalidRequestException e) {
//      log.error("Error while processing yaml");
//      throw e;
//    }
//  }

  @Override
  public void deleteAllForGivenPlanExecutionId(Set<String> planExecutionIds) {
    // Uses - planExecutionId_barrierState_stagesIdentifier_idx
    Criteria planExecutionIdCriteria =
        Criteria.where(BarrierExecutionInstanceKeys.planExecutionId).in(planExecutionIds);
    Query query = new Query(planExecutionIdCriteria);

    RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying]: Failed deleting BarrierExecutionInstance; attempt: {}",
            "[Failed]: Failed deleting BarrierExecutionInstance; attempt: {}");

    Failsafe.with(retryPolicy).get(() -> mongoTemplate.remove(query, BarrierExecutionInstance.class));
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return PersistenceUtils.getRetryPolicy(failedAttemptMessage, failureMessage);
  }
}
