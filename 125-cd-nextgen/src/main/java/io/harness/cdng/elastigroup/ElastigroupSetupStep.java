/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.elastigroup;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.ecs.EcsStepCommonHelper;
import io.harness.cdng.ecs.EcsStepExecutor;
import io.harness.cdng.ecs.EcsStepHelperImpl;
import io.harness.cdng.ecs.beans.EcsBlueGreenCreateServiceDataOutcome;
import io.harness.cdng.ecs.beans.EcsExecutionPassThroughData;
import io.harness.cdng.ecs.beans.EcsGitFetchFailurePassThroughData;
import io.harness.cdng.ecs.beans.EcsPrepareRollbackDataPassThroughData;
import io.harness.cdng.ecs.beans.EcsStepExceptionPassThroughData;
import io.harness.cdng.ecs.beans.EcsStepExecutorParams;
import io.harness.cdng.elastigroup.beans.ElastigroupExecutionPassThroughData;
import io.harness.cdng.elastigroup.beans.ElastigroupSetupDataOutcome;
import io.harness.cdng.elastigroup.beans.ElastigroupStartupScriptFetchFailurePassThroughData;
import io.harness.cdng.elastigroup.beans.ElastigroupStepExceptionPassThroughData;
import io.harness.cdng.elastigroup.beans.ElastigroupStepExecutorParams;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.ecs.EcsBlueGreenCreateServiceResult;
import io.harness.delegate.beans.elastigroup.ElastigroupSetupResult;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.ecs.EcsCommandTypeNG;
import io.harness.delegate.task.ecs.EcsLoadBalancerConfig;
import io.harness.delegate.task.ecs.request.EcsBlueGreenCreateServiceRequest;
import io.harness.delegate.task.ecs.request.EcsBlueGreenPrepareRollbackRequest;
import io.harness.delegate.task.ecs.response.EcsBlueGreenCreateServiceResponse;
import io.harness.delegate.task.elastigroup.request.ElastigroupSetupCommandRequest;
import io.harness.delegate.task.elastigroup.response.ElastigroupCommandTypeNG;
import io.harness.delegate.task.elastigroup.response.ElastigroupSetupResponse;
import io.harness.delegate.task.elastigroup.response.SpotInstConfig;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;
import software.wings.utils.ServiceVersionConvention;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class ElastigroupSetupStep extends TaskChainExecutableWithRollbackAndRbac implements ElastigroupStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.ELASTIGROUP_SETUP.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  private final String ELASTIGROUP_SETUP_COMMAND_NAME = "ElastigroupSetup";

  @Inject private ElastigroupStepCommonHelper elastigroupStepCommonHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public TaskChainResponse executeElastigroupTask(Ambiance ambiance, StepElementParameters stepParameters,
                                                  ElastigroupExecutionPassThroughData executionPassThroughData, UnitProgressData unitProgressData,
                                                  ElastigroupStepExecutorParams elastigroupStepExecutorParams) {
    InfrastructureOutcome infrastructureOutcome = executionPassThroughData.getInfrastructure();
    final String accountId = AmbianceUtils.getAccountId(ambiance);

    SpotInstConfig spotInstConfig = elastigroupStepCommonHelper.getSpotInstConfig(infrastructureOutcome, ambiance);

    ElastigroupSetupStepParameters elastigroupSetupStepParameters =
        (ElastigroupSetupStepParameters) stepParameters.getSpec();

    String elastigroupNamePrefix = elastigroupSetupStepParameters.getName().getValue();
    String elastigroupJson = "asf";

    ElastigroupSetupCommandRequest elastigroupSetupCommandRequest =
            ElastigroupSetupCommandRequest.builder()
                    .blueGreen(false)
                    .elastigroupNamePrefix(elastigroupNamePrefix)
            .accountId(accountId)
            .spotInstConfig(spotInstConfig)
                    .elastiGroupJson(elastigroupJson)
            .ecsCommandType(ElastigroupCommandTypeNG.ELASTIGROUP_SETUP)
                    .startupScript(elastigroupStepExecutorParams.getStartupScript())
            .commandName(ELASTIGROUP_SETUP_COMMAND_NAME)
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepParameters))
            .build();

    return elastigroupStepCommonHelper.queueElastigroupTask(
        stepParameters, elastigroupSetupCommandRequest, ambiance, executionPassThroughData, true);
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // nothing
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    log.info("Calling executeNextLink");
    return elastigroupStepCommonHelper.executeNextLink(
        this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof ElastigroupStartupScriptFetchFailurePassThroughData) {
      return elastigroupStepCommonHelper.handleStartupScriptTaskFailure((ElastigroupStartupScriptFetchFailurePassThroughData) passThroughData);
    } else if (passThroughData instanceof ElastigroupStepExceptionPassThroughData) {
      return elastigroupStepCommonHelper.handleStepExceptionFailure((ElastigroupStepExceptionPassThroughData) passThroughData);
    }

    log.info("Finalizing execution with passThroughData: " + passThroughData.getClass().getName());
    ElastigroupExecutionPassThroughData elastigroupExecutionPassThroughData = (ElastigroupExecutionPassThroughData) passThroughData;
    ElastigroupSetupResponse elastigroupSetupResponse;
    try {
      elastigroupSetupResponse = (ElastigroupSetupResponse) responseDataSupplier.get();
    } catch (Exception e) {
      log.error("Error while processing ecs task response: {}", e.getMessage(), e);
      return elastigroupStepCommonHelper.handleTaskException(ambiance, elastigroupExecutionPassThroughData, e);
    }
    StepResponseBuilder stepResponseBuilder = StepResponse.builder().unitProgressList(
            elastigroupSetupResponse.getUnitProgressData().getUnitProgresses());
    if (elastigroupSetupResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return ElastigroupStepCommonHelper.getFailureResponseBuilder(elastigroupSetupResponse, stepResponseBuilder)
          .build();
    }

//    ElastigroupSetupResult elastigroupSetupResult =
//            elastigroupSetupResponse.getElastigroupSetupResult();
    ElastigroupSetupDataOutcome elastigroupSetupDataOutcome =
            ElastigroupSetupDataOutcome.builder()
            .build();

    executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.ELASTIGROUP_SETUP_OUTCOME,
            elastigroupSetupDataOutcome, StepOutcomeGroup.STEP.name());

    return stepResponseBuilder.status(Status.SUCCEEDED).build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return elastigroupStepCommonHelper.startChainLink(this, ambiance, stepParameters);
  }
}
