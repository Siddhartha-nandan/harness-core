/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
import io.harness.advisers.manualIntervention.ManualInterventionAdviserWithRollback;
import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.advisers.pipelinerollback.OnFailPipelineRollbackAdviser;
import io.harness.advisers.pipelinerollback.OnFailPipelineRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackAdviser;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.govern.Switch;
import io.harness.plancreator.steps.v1.FailureStrategiesUtilsV1;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviser;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.markFailure.OnMarkFailureAdviser;
import io.harness.pms.sdk.core.adviser.markFailure.OnMarkFailureAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.failurestrategy.manualintervention.v1.ManualInterventionFailureConfigV1;
import io.harness.yaml.core.failurestrategy.retry.v1.RetryFailureConfigV1;
import io.harness.yaml.core.failurestrategy.v1.FailureConfigV1;
import io.harness.yaml.core.failurestrategy.v1.NGFailureActionTypeV1;
import io.harness.yaml.core.failurestrategy.v1.OnConfigV1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PlanCreatorUtilsV1 {
  public List<AdviserObtainment> getAdviserObtainmentsForStage(KryoSerializer kryoSerializer, Dependency dependency) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    AdviserObtainment nextStepAdviser = getNextStepAdviser(kryoSerializer, dependency);
    // The case of parallel doesn't need to be handled explicitly as nextId will not be present in dependency in that
    // case.
    // TODO(shalini): handle the case of strategy and rollback
    if (nextStepAdviser != null) {
      adviserObtainments.add(nextStepAdviser);
    }
    return adviserObtainments;
  }

  // TODO(shalini): stage rollback, pipeline rollback, and proceedWithDefaultValues not handled yet
  public List<AdviserObtainment> getAdviserObtainmentsForStep(
      KryoSerializer kryoSerializer, Dependency dependency, YamlNode node) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    List<AdviserObtainment> failureStrategyAdvisers = getFailureStrategiesAdvisers(
        kryoSerializer, dependency, node, getNextNodeUuid(kryoSerializer, dependency), false);
    adviserObtainments.addAll(failureStrategyAdvisers);
    AdviserObtainment nextStepAdviser = getNextStepAdviser(kryoSerializer, dependency);
    if (nextStepAdviser != null) {
      adviserObtainments.add(nextStepAdviser);
    }
    return adviserObtainments;
  }

  public String getNextNodeUuid(KryoSerializer kryoSerializer, Dependency dependency) {
    Optional<Object> nextNodeIdOptional =
        getDeserializedObjectFromDependency(dependency, kryoSerializer, PlanCreatorConstants.NEXT_ID, false);
    if (nextNodeIdOptional.isPresent() && nextNodeIdOptional.get() instanceof String) {
      return (String) nextNodeIdOptional.get();
    }
    return null;
  }

  public AdviserObtainment getNextStepAdviser(KryoSerializer kryoSerializer, Dependency dependency) {
    if (dependency == null) {
      return null;
    }
    String nextId = getNextNodeUuid(kryoSerializer, dependency);
    if (nextId != null) {
      return AdviserObtainment.newBuilder()
          .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STAGE.name()).build())
          .setParameters(ByteString.copyFrom(
              kryoSerializer.asBytes(NextStepAdviserParameters.builder().nextNodeId(nextId).build())))
          .build();
    }
    return null;
  }

  public HarnessValue getNodeMetadataValueFromDependency(Dependency dependency, String key) {
    if (isNotEmpty(dependency.getNodeMetadata().getDataMap())
        && dependency.getNodeMetadata().getDataMap().containsKey(key)) {
      return dependency.getNodeMetadata().getDataMap().get(key);
    }
    return null;
  }

  public ByteString getMetadataValueFromDependency(Dependency dependency, String key) {
    if (isNotEmpty(dependency.getMetadataMap()) && dependency.getMetadataMap().containsKey(key)) {
      return dependency.getMetadataMap().get(key);
    }
    return null;
  }

  public Optional<Object> getDeserializedObjectFromDependency(
      Dependency dependency, KryoSerializer kryoSerializer, String key, boolean asInflatedObject) {
    if (dependency == null) {
      return Optional.empty();
    }
    HarnessValue harnessValue = getNodeMetadataValueFromDependency(dependency, key);
    if (harnessValue != null) {
      if (harnessValue.hasStringValue()) {
        return Optional.of(harnessValue.getStringValue());
      }
      if (harnessValue.hasBytesValue()) {
        ByteString bytes = harnessValue.getBytesValue();
        Optional<Object> objectOptional = getObjectFromBytes(bytes, kryoSerializer, asInflatedObject);
        if (objectOptional.isPresent()) {
          return objectOptional;
        }
      }
    }
    ByteString bytes = getMetadataValueFromDependency(dependency, key);
    return getObjectFromBytes(bytes, kryoSerializer, asInflatedObject);
  }

  public RepairActionCode toRepairAction(FailureConfigV1 action) {
    switch (action.getType()) {
      case IGNORE:
        return RepairActionCode.IGNORE;
      case MARK_AS_SUCCESS:
        return RepairActionCode.MARK_AS_SUCCESS;
      case ABORT:
        return RepairActionCode.END_EXECUTION;
      case STAGE_ROLLBACK:
        return RepairActionCode.STAGE_ROLLBACK;
      case MANUAL_INTERVENTION:
        return RepairActionCode.MANUAL_INTERVENTION;
      case RETRY:
        return RepairActionCode.RETRY;
      case MARK_AS_FAILURE:
        return RepairActionCode.MARK_AS_FAILURE;
      case PIPELINE_ROLLBACK:
        return RepairActionCode.PIPELINE_ROLLBACK;
      default:
        throw new InvalidRequestException(
            action.toString() + " Failure action doesn't have corresponding RepairAction Code.");
    }
  }

  private AdviserObtainment getManualInterventionAdviserObtainment(KryoSerializer kryoSerializer,
      Set<FailureType> failureTypes, AdviserObtainment.Builder adviserObtainmentBuilder,
      ManualInterventionFailureConfigV1 actionConfig, FailureConfigV1 actionUnderManualIntervention) {
    return adviserObtainmentBuilder.setType(ManualInterventionAdviserWithRollback.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
            ManualInterventionAdviserRollbackParameters.builder()
                .applicableFailureTypes(failureTypes)
                .timeoutAction(toRepairAction(actionUnderManualIntervention))
                .timeout((int) TimeoutUtils.getTimeoutInSeconds(actionConfig.getSpec().getTimeout(), 0))
                .build())))
        .build();
  }

  private List<AdviserObtainment> getFailureStrategiesAdvisers(KryoSerializer kryoSerializer, Dependency dependency,
      YamlNode yamlNode, String nextNodeUuid, boolean isStepInsideRollback) {
    OnConfigV1 stepFailureStrategies = getFailureStrategies(yamlNode);
    OnConfigV1 stageFailureStrategies = getStageFailureStrategies(kryoSerializer, dependency);
    OnConfigV1 stepGroupFailureStrategies = getStepGroupFailureStrategies(kryoSerializer, dependency);
    Map<FailureConfigV1, Collection<FailureType>> actionMap = FailureStrategiesUtilsV1.priorityMergeFailureStrategies(
        stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);
    return getFailureStrategiesAdvisers(kryoSerializer, actionMap, isStepInsideRollback, nextNodeUuid);
  }

  Optional<Object> getDeserializedObjectFromParentInfo(
      KryoSerializer kryoSerializer, Dependency dependency, String key, boolean asInflatedObject) {
    if (dependency != null && dependency.getParentInfo().getDataMap().containsKey(key)) {
      ByteString bytes = dependency.getParentInfo().getDataMap().get(key).getBytesValue();
      return getObjectFromBytes(bytes, kryoSerializer, asInflatedObject);
    }
    return Optional.empty();
  }

  Optional<Object> getObjectFromBytes(ByteString bytes, KryoSerializer kryoSerializer, boolean asInflatedObject) {
    if (isNotEmpty(bytes)) {
      if (asInflatedObject) {
        return Optional.of(kryoSerializer.asInflatedObject(bytes.toByteArray()));
      }
      return Optional.of(kryoSerializer.asObject(bytes.toByteArray()));
    }
    return Optional.empty();
  }

  OnConfigV1 getStageFailureStrategies(KryoSerializer kryoSerializer, Dependency dependency) {
    Optional<Object> stageFailureStrategiesOptional = getDeserializedObjectFromParentInfo(
        kryoSerializer, dependency, PlanCreatorConstants.STAGE_FAILURE_STRATEGIES, true);
    OnConfigV1 stageFailureStrategies = null;
    if (stageFailureStrategiesOptional.isPresent()) {
      stageFailureStrategies = (OnConfigV1) stageFailureStrategiesOptional.get();
    }
    return stageFailureStrategies;
  }

  OnConfigV1 getStepGroupFailureStrategies(KryoSerializer kryoSerializer, Dependency dependency) {
    Optional<Object> stepGroupFailureStrategiesOptional = getDeserializedObjectFromParentInfo(
        kryoSerializer, dependency, PlanCreatorConstants.STEP_GROUP_FAILURE_STRATEGIES, true);
    OnConfigV1 stepGroupFailureStrategies = null;
    if (stepGroupFailureStrategiesOptional.isPresent()) {
      stepGroupFailureStrategies = (OnConfigV1) stepGroupFailureStrategiesOptional.get();
    }
    return stepGroupFailureStrategies;
  }

  private List<AdviserObtainment> getFailureStrategiesAdvisers(KryoSerializer kryoSerializer,
      Map<FailureConfigV1, Collection<FailureType>> actionMap, boolean isStepInsideRollback, String nextNodeUuid) {
    List<AdviserObtainment> adviserObtainmentList = new ArrayList<>();
    for (Map.Entry<FailureConfigV1, Collection<FailureType>> entry : actionMap.entrySet()) {
      FailureConfigV1 action = entry.getKey();
      Set<FailureType> failureTypes = new HashSet<>(entry.getValue());
      NGFailureActionTypeV1 actionType = action.getType();

      if (isStepInsideRollback) {
        if (actionType == io.harness.yaml.core.failurestrategy.v1.NGFailureActionTypeV1.STAGE_ROLLBACK) {
          throw new InvalidRequestException("Step inside rollback section cannot have Rollback as failure strategy.");
        }
      }
      AdviserObtainment adviserObtainment =
          getAdviserObtainmentForActionType(actionType, failureTypes, kryoSerializer, nextNodeUuid, action);
      if (adviserObtainment != null) {
        adviserObtainmentList.add(adviserObtainment);
      }
    }
    return adviserObtainmentList;
  }

  AdviserObtainment getAdviserObtainmentForActionType(NGFailureActionTypeV1 actionType, Set<FailureType> failureTypes,
      KryoSerializer kryoSerializer, String nextNodeUuid, FailureConfigV1 action) {
    AdviserObtainment.Builder adviserObtainmentBuilder = AdviserObtainment.newBuilder();
    switch (actionType) {
      case IGNORE:
        return adviserObtainmentBuilder.setType(IgnoreAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(IgnoreAdviserParameters.builder()
                                                                          .applicableFailureTypes(failureTypes)
                                                                          .nextNodeId(nextNodeUuid)
                                                                          .build())))
            .build();
      case RETRY:
        RetryFailureConfigV1 retryAction = (RetryFailureConfigV1) action;
        FailureStrategiesUtilsV1.validateRetryFailureAction(retryAction);
        ParameterField<Integer> retryCount = retryAction.getSpec().getAttempts();
        FailureConfigV1 actionUnderRetry = retryAction.getSpec().getOn_failure();
        // TODO(Shalini): Add method to get RetryAdviserObtainment and add it into adviserObtainmentList
        break;
      case MARK_AS_SUCCESS:
        return adviserObtainmentBuilder.setType(OnMarkSuccessAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(OnMarkSuccessAdviserParameters.builder()
                                                                          .applicableFailureTypes(failureTypes)
                                                                          .nextNodeId(nextNodeUuid)
                                                                          .build())))
            .build();
      case ABORT:
        return adviserObtainmentBuilder.setType(OnAbortAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                OnAbortAdviserParameters.builder().applicableFailureTypes(failureTypes).build())))
            .build();
      case STAGE_ROLLBACK:
        // TODO(Shalini): Add methd to get rollback parameters and set it below in parameters
        return adviserObtainmentBuilder.setType(OnFailRollbackAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(null)))
            .build();
      case MANUAL_INTERVENTION:
        ManualInterventionFailureConfigV1 actionConfig = (ManualInterventionFailureConfigV1) action;
        FailureStrategiesUtilsV1.validateManualInterventionFailureAction(actionConfig);
        FailureConfigV1 actionUnderManualIntervention = actionConfig.getSpec().getTimeout_action();
        return getManualInterventionAdviserObtainment(
            kryoSerializer, failureTypes, adviserObtainmentBuilder, actionConfig, actionUnderManualIntervention);
      case PIPELINE_ROLLBACK:
        OnFailPipelineRollbackParameters onFailPipelineRollbackParameters =
            FailureStrategiesUtilsV1.buildOnFailPipelineRollbackParameters(failureTypes);
        return adviserObtainmentBuilder.setType(OnFailPipelineRollbackAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(onFailPipelineRollbackParameters)))
            .build();
      case MARK_AS_FAILURE:
        return adviserObtainmentBuilder.setType(OnMarkFailureAdviser.ADVISER_TYPE)
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(OnMarkFailureAdviserParameters.builder()
                                                                          .applicableFailureTypes(failureTypes)
                                                                          .nextNodeId(nextNodeUuid)
                                                                          .build())))
            .build();
      default:
        Switch.unhandled(actionType);
    }
    return null;
  }

  public OnConfigV1 getFailureStrategies(YamlNode node) {
    YamlField onConfigV1 = node.getField("on");
    ParameterField<OnConfigV1> onConfigV1ParameterField = null;

    try {
      if (onConfigV1 != null) {
        onConfigV1ParameterField =
            YamlUtils.read(onConfigV1.getNode().toString(), new TypeReference<ParameterField<OnConfigV1>>() {});
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    // If failureStrategies configured as <+input> and no value is given, failureStrategyConfigs.getValue() will still
    // be null and handled as empty list
    if (ParameterField.isNotNull(onConfigV1ParameterField)) {
      return onConfigV1ParameterField.getValue();
    } else {
      return null;
    }
  }

  // TODO: Get isStepInsideRollback from dependency metadata map
  public boolean isStepInsideRollback(Dependency dependency) {
    return false;
  }
}
