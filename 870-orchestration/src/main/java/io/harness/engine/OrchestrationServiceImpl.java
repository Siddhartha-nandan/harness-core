package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.HashGenerator;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.engine.utils.TransactionUtils;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.interrupts.Interrupt;
import io.harness.observer.Subject;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.repositories.PlanRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private InterruptManager interruptManager;
  @Inject private PlanRepository planRepository;
  @Inject private PlanExecutionMetadataService planExecutionMetadataService;
  @Inject private TransactionUtils transactionUtils;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Getter private final Subject<OrchestrationStartObserver> orchestrationStartSubject = new Subject<>();

  @Override
  public PlanExecution startExecution(Plan plan, ExecutionMetadata metadata) {
    return startExecution(plan, new HashMap<>(), metadata, PlanExecutionMetadata.builder().build());
  }

  @Override
  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) {
    Plan savedPlan = planRepository.save(plan);
    return executePlan(savedPlan, setupAbstractions, metadata, planExecutionMetadata);
  }

  private PlanExecution executePlan(@Valid Plan plan, Map<String, String> setupAbstractions, ExecutionMetadata metadata,
      PlanExecutionMetadata planExecutionMetadata) {
    PlanExecution savedPlanExecution = createPlanExecution(plan, setupAbstractions, metadata, planExecutionMetadata);

    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions)
                            .setPlanExecutionId(savedPlanExecution.getUuid())
                            .setPlanId(plan.getUuid())
                            .setMetadata(metadata)
                            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
                            .build();
    eventEmitter.emitEvent(
        OrchestrationEvent.builder().ambiance(ambiance).eventType(OrchestrationEventType.ORCHESTRATION_START).build());
    orchestrationStartSubject.fireInform(OrchestrationStartObserver::onStart,
        OrchestrationStartInfo.builder().ambiance(ambiance).planExecutionMetadata(planExecutionMetadata).build());

    PlanNodeProto planNode = plan.fetchStartingNode();
    if (planNode == null) {
      log.error("Cannot Start Execution for empty plan");
      return null;
    }
    submitToEngine(ambiance, planNode);
    return savedPlanExecution;
  }

  private void submitToEngine(Ambiance ambiance, PlanNodeProto planNode) {
    executorService.submit(() -> orchestrationEngine.triggerExecution(ambiance, planNode));
  }

  private PlanExecution createPlanExecution(@Valid Plan plan, Map<String, String> setupAbstractions,
      ExecutionMetadata metadata, PlanExecutionMetadata planExecutionMetadata) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(metadata.getExecutionUuid())
                                      .planId(plan.getUuid())
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .startTs(System.currentTimeMillis())
                                      .metadata(metadata)
                                      .build();

    return transactionUtils.performTransaction(() -> {
      planExecutionMetadataService.save(planExecutionMetadata);
      return planExecutionService.save(planExecution);
    });
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }
}
