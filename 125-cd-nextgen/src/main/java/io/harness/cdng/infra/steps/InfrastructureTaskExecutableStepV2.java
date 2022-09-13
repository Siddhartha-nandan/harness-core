/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.INFRA_TASK_EXECUTABLE_STEP_OUTPUT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogCallbackUtils.saveExecutionLogSafely;

import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.execution.ExecutionInfoKey;
import io.harness.cdng.execution.helper.ExecutionInfoKeyMapper;
import io.harness.cdng.execution.helper.StageExecutionHelper;
import io.harness.cdng.infra.InfrastructureMapper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sAzureInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.mapper.InfrastructureEntityConfigMapper;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.InfrastructureConfig;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.ssh.SshInfraDelegateConfig;
import io.harness.delegate.task.ssh.WinRmInfraDelegateConfig;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.steps.executable.TaskExecutableWithRbac;
import io.harness.steps.shellscript.HostsOutput;
import io.harness.steps.shellscript.K8sInfraDelegateConfigOutput;
import io.harness.steps.shellscript.SshInfraDelegateConfigOutput;
import io.harness.steps.shellscript.WinRmInfraDelegateConfigOutput;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class InfrastructureTaskExecutableStepV2 extends AbstractInfrastructureTaskExecutableStep
    implements TaskExecutableWithRbac<InfrastructureTaskExecutableStepV2Params, DelegateResponseData> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.INFRASTRUCTURE_TASKSTEP_V2.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject private InfrastructureEntityService infrastructureEntityService;
  @Inject private InfrastructureStepHelper infrastructureStepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private StageExecutionHelper stageExecutionHelper;
  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Inject private PipelineRbacHelper pipelineRbacHelper;

  @Override
  public Class<InfrastructureTaskExecutableStepV2Params> getStepParametersClass() {
    return InfrastructureTaskExecutableStepV2Params.class;
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, InfrastructureTaskExecutableStepV2Params stepParameters, StepInputPackage inputPackage) {
    validateStepParameters(stepParameters);

    final InfrastructureConfig infrastructureConfig = fetchInfraConfigFromDBorThrow(ambiance, stepParameters);
    final Infrastructure infraSpec = infrastructureConfig.getInfrastructureDefinitionConfig().getSpec();

    validateResources(ambiance, infraSpec);

    final NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance, true, "Execute");
    // Create delegate task for infra if needed
    if (isTaskStep(infraSpec.getKind())) {
      return obtainTaskInternal(ambiance, infraSpec, logCallback,
          !infrastructureConfig.getInfrastructureDefinitionConfig().isAllowSimultaneousDeployments());
    }

    // If delegate task is not needed, just validate the infra spec
    executeSync(ambiance, infrastructureConfig, logCallback);
    return null;
  }

  @Override
  public void validateResources(Ambiance ambiance, InfrastructureTaskExecutableStepV2Params stepParameters) {
    // Not validating here. Instead, validation is done in obtainTaskWithRBAC method to avoid unnecessary db calls of
    // fetching infrastructure/sweeping outputs
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance,
      InfrastructureTaskExecutableStepV2Params stepParameters,
      ThrowingSupplier<DelegateResponseData> responseDataSupplier) throws Exception {
    final InfrastructureTaskExecutableStepSweepingOutput infraOutput = fetchInfraStepOutputOrThrow(ambiance);

    final NGLogCallback logCallback = infrastructureStepHelper.getInfrastructureLogCallback(ambiance, "Execute");
    // handle response from delegate if task was created
    if (isTaskStep(infraOutput.getInfrastructureOutcome().getKind())) {
      return handleTaskResult(ambiance, infraOutput, responseDataSupplier, logCallback);
    }

    // just produce step response. Sync flow
    return produceStepResponseForNonTaskStepInfra(ambiance, infraOutput, logCallback);
  }

  private StepResponse produceStepResponseForNonTaskStepInfra(
      Ambiance ambiance, InfrastructureTaskExecutableStepSweepingOutput stepSweepingOutput, NGLogCallback logCallback) {
    final long startTime = System.currentTimeMillis();

    final OutcomeSet outcomeSet = fetchRequiredOutcomes(ambiance);
    final EnvironmentOutcome environmentOutcome = outcomeSet.getEnvironmentOutcome();
    final ServiceStepOutcome serviceOutcome = outcomeSet.getServiceStepOutcome();

    final InfrastructureOutcome infrastructureOutcome = stepSweepingOutput.getInfrastructureOutcome();

    publishInfraDelegateConfigOutput(logCallback, serviceOutcome, infrastructureOutcome, ambiance, environmentOutcome,
        stepSweepingOutput.isSkipInstances());

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    String infrastructureKind = infrastructureOutcome.getKind();
    if (stageExecutionHelper.shouldSaveStageExecutionInfo(infrastructureKind)) {
      ExecutionInfoKey executionInfoKey = ExecutionInfoKeyMapper.getExecutionInfoKey(
          ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
      stageExecutionHelper.saveStageExecutionInfoAndPublishExecutionInfoKey(
          ambiance, executionInfoKey, infrastructureKind);
      if (stageExecutionHelper.isRollbackArtifactRequiredPerInfrastructure(infrastructureKind)) {
        stageExecutionHelper.addRollbackArtifactToStageOutcomeIfPresent(
            ambiance, stepResponseBuilder, executionInfoKey, infrastructureKind);
      }
    }

    saveExecutionLog(
        logCallback, color("Completed infrastructure step", Green), LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    return stepResponseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .outcome(infrastructureOutcome)
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .group(OutcomeExpressionConstants.INFRASTRUCTURE_GROUP)
                         .build())

        .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                                                        .setUnitName("Execute")
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setStartTime(startTime)
                                                        .setEndTime(System.currentTimeMillis())
                                                        .build()))
        .build();
  }

  private void validateResources(Ambiance ambiance, Infrastructure infraSpec) {
    final ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    final String principal = executionPrincipalInfo.getPrincipal();

    if (isEmpty(principal)) {
      log.warn("no principal found while executing the infrastructure step. skipping resource validation");
      return;
    }
    final Set<EntityDetailProtoDTO> entityDetails =
        entityReferenceExtractorUtils.extractReferredEntities(ambiance, infraSpec);
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
  }

  private void executeSync(Ambiance ambiance, InfrastructureConfig infrastructure, NGLogCallback logCallback) {
    final Infrastructure spec = infrastructure.getInfrastructureDefinitionConfig().getSpec();
    validateConnector(spec, ambiance);
    saveExecutionLog(logCallback, "Fetching environment information...");
    validateInfrastructure(spec, ambiance);

    final OutcomeSet outcomeSet = fetchRequiredOutcomes(ambiance);
    final EnvironmentOutcome environmentOutcome = outcomeSet.getEnvironmentOutcome();
    final ServiceStepOutcome serviceOutcome = outcomeSet.getServiceStepOutcome();
    final InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(spec, environmentOutcome, serviceOutcome);

    // save spec sweeping output for further use within the step
    boolean skipInstances = infrastructureStepHelper.getSkipInstances(spec);
    executionSweepingOutputService.consume(ambiance, INFRA_TASK_EXECUTABLE_STEP_OUTPUT,
        InfrastructureTaskExecutableStepSweepingOutput.builder()
            .infrastructureOutcome(infrastructureOutcome)
            .skipInstances(skipInstances)
            .addRcStep(!infrastructure.getInfrastructureDefinitionConfig().isAllowSimultaneousDeployments())
            .build(),
        StepCategory.STAGE.name());
  }

  private boolean isTaskStep(String infraKind) {
    return InfrastructureKind.SSH_WINRM_AZURE.equals(infraKind) || InfrastructureKind.SSH_WINRM_AWS.equals(infraKind);
  }

  private InfrastructureConfig fetchInfraConfigFromDBorThrow(
      Ambiance ambiance, InfrastructureTaskExecutableStepV2Params stepParameters) {
    Optional<InfrastructureEntity> infrastructureEntityOpt =
        infrastructureEntityService.get(AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
            AmbianceUtils.getProjectIdentifier(ambiance), stepParameters.getEnvRef().getValue(),
            stepParameters.getInfraRef().getValue());
    if (infrastructureEntityOpt.isEmpty()) {
      throw new InvalidRequestException(String.format("Infrastructure definition %s not found in environment %s",
          stepParameters.getInfraRef().getValue(), stepParameters.getEnvRef().getValue()));
    }

    final InfrastructureEntity infrastructureEntity = infrastructureEntityOpt.get();
    if (ParameterField.isNotNull(stepParameters.getInfraInputs())
        && isNotEmpty(stepParameters.getInfraInputs().getValue())) {
      String mergedYaml = mergeInfraInputs(infrastructureEntity.getYaml(), stepParameters.getInfraInputs().getValue());
      infrastructureEntity.setYaml(mergedYaml);
    }

    return InfrastructureEntityConfigMapper.toInfrastructureConfig(infrastructureEntity);
  }

  private void publishInfraDelegateConfigOutput(NGLogCallback logCallback, ServiceStepOutcome serviceOutcome,
      InfrastructureOutcome infrastructureOutcome, Ambiance ambiance, EnvironmentOutcome environmentOutcome,
      boolean skipInstances) {
    if (serviceOutcome.getType() == null) {
      throw new InvalidRequestException("service type cannot be null");
    }
    if (ServiceSpecType.SSH.toLowerCase(Locale.ROOT).equals(serviceOutcome.getType().toLowerCase(Locale.ROOT))) {
      ExecutionInfoKey executionInfoKey = ExecutionInfoKeyMapper.getExecutionInfoKey(
          ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
      publishSshInfraDelegateConfigOutput(
          ambiance, logCallback, infrastructureOutcome, executionInfoKey, skipInstances);
      return;
    }

    if (ServiceSpecType.WINRM.toLowerCase(Locale.ROOT).equals(serviceOutcome.getType().toLowerCase(Locale.ROOT))) {
      ExecutionInfoKey executionInfoKey = ExecutionInfoKeyMapper.getExecutionInfoKey(
          ambiance, environmentOutcome, serviceOutcome, infrastructureOutcome);
      publishWinRmInfraDelegateConfigOutput(
          ambiance, logCallback, infrastructureOutcome, executionInfoKey, skipInstances);
      return;
    }

    if (infrastructureOutcome instanceof K8sGcpInfrastructureOutcome
        || infrastructureOutcome instanceof K8sDirectInfrastructureOutcome
        || infrastructureOutcome instanceof K8sAzureInfrastructureOutcome) {
      K8sInfraDelegateConfig k8sInfraDelegateConfig =
          cdStepHelper.getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);

      K8sInfraDelegateConfigOutput k8sInfraDelegateConfigOutput =
          K8sInfraDelegateConfigOutput.builder().k8sInfraDelegateConfig(k8sInfraDelegateConfig).build();
      executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.K8S_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
          k8sInfraDelegateConfigOutput, StepCategory.STAGE.name());
    }
  }

  private void publishSshInfraDelegateConfigOutput(Ambiance ambiance, NGLogCallback logCallback,
      InfrastructureOutcome infrastructureOutcome, ExecutionInfoKey executionInfoKey, boolean skipInstances) {
    SshInfraDelegateConfig sshInfraDelegateConfig =
        cdStepHelper.getSshInfraDelegateConfig(infrastructureOutcome, ambiance);

    SshInfraDelegateConfigOutput sshInfraDelegateConfigOutput =
        SshInfraDelegateConfigOutput.builder().sshInfraDelegateConfig(sshInfraDelegateConfig).build();
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.SSH_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
        sshInfraDelegateConfigOutput, StepCategory.STAGE.name());
    Set<String> hosts = sshInfraDelegateConfig.getHosts();
    if (EmptyPredicate.isEmpty(hosts)) {
      saveExecutionLogSafely(logCallback,
          color("No host(s) were provided for specified infrastructure or filter did not match any instance(s)", Red));
    } else {
      saveExecutionLogSafely(logCallback, color(format("Successfully fetched %s instance(s)", hosts.size()), Green));
      saveExecutionLogSafely(logCallback, color(format("Fetched following instance(s) %s)", hosts), Green));
    }

    Set<String> filteredHosts = stageExecutionHelper.saveAndExcludeHostsWithSameArtifactDeployedIfNeeded(
        ambiance, executionInfoKey, infrastructureOutcome, hosts, ServiceSpecType.SSH, skipInstances, logCallback);
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.OUTPUT,
        HostsOutput.builder().hosts(filteredHosts).build(), StepCategory.STAGE.name());
  }

  private void publishWinRmInfraDelegateConfigOutput(Ambiance ambiance, NGLogCallback logCallback,
      InfrastructureOutcome infrastructureOutcome, ExecutionInfoKey executionInfoKey, boolean skipInstances) {
    WinRmInfraDelegateConfig winRmInfraDelegateConfig =
        cdStepHelper.getWinRmInfraDelegateConfig(infrastructureOutcome, ambiance);

    WinRmInfraDelegateConfigOutput winRmInfraDelegateConfigOutput =
        WinRmInfraDelegateConfigOutput.builder().winRmInfraDelegateConfig(winRmInfraDelegateConfig).build();
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.WINRM_INFRA_DELEGATE_CONFIG_OUTPUT_NAME,
        winRmInfraDelegateConfigOutput, StepCategory.STAGE.name());
    Set<String> hosts = winRmInfraDelegateConfig.getHosts();
    if (EmptyPredicate.isEmpty(hosts)) {
      saveExecutionLogSafely(logCallback,
          color("No host(s) were provided for specified infrastructure or filter did not match any instance(s)", Red));
    } else {
      saveExecutionLogSafely(logCallback, color(format("Successfully fetched %s instance(s)", hosts.size()), Green));
      saveExecutionLogSafely(logCallback, color(format("Fetched following instance(s) %s)", hosts), Green));
    }

    Set<String> filteredHosts = stageExecutionHelper.saveAndExcludeHostsWithSameArtifactDeployedIfNeeded(
        ambiance, executionInfoKey, infrastructureOutcome, hosts, ServiceSpecType.WINRM, skipInstances, logCallback);
    executionSweepingOutputService.consume(ambiance, OutputExpressionConstants.OUTPUT,
        HostsOutput.builder().hosts(filteredHosts).build(), StepCategory.STAGE.name());
  }

  private void validateStepParameters(InfrastructureTaskExecutableStepV2Params stepParameters) {
    if (ParameterField.isNull(stepParameters.getEnvRef())) {
      throw new InvalidRequestException("environment reference is not specified");
    }

    if (ParameterField.isNull(stepParameters.getInfraRef())) {
      throw new InvalidRequestException("infrastructure definition reference is not specified");
    }

    if (stepParameters.getEnvRef().isExpression()) {
      throw new InvalidRequestException(
          "environment reference " + stepParameters.getEnvRef().getExpressionValue() + " not resolved");
    }

    if (stepParameters.getInfraRef().isExpression()) {
      throw new InvalidRequestException(
          "infrastructure definition reference" + stepParameters.getInfraRef().getExpressionValue() + " not resolved");
    }
  }

  private String mergeInfraInputs(String originalYaml, Map<String, Object> inputs) {
    if (isEmpty(inputs)) {
      return originalYaml;
    }
    Map<String, Object> inputMap = new HashMap<>();
    inputMap.put(YamlTypes.INFRASTRUCTURE_DEF, inputs);
    return MergeHelper.mergeInputSetFormatYamlToOriginYaml(originalYaml, YamlPipelineUtils.writeYamlString(inputMap));
  }
}
