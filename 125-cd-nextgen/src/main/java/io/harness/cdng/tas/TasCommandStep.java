/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static software.wings.beans.TaskType.CF_COMMAND_TASK_NG;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.FileData;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.pcf.CfCommandTypeNG;
import io.harness.delegate.task.pcf.request.CfRunPluginCommandRequestNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.pcf.CfCommandUnitConstants;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TasCommandStep extends TaskChainExecutableWithRollbackAndRbac implements TasStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
          .setType(ExecutionNodeType.TANZU_COMMAND.getYamlType())
          .setStepCategory(StepCategory.STEP)
          .build();
  @Inject private TasStepHelper tasStepHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private InstanceInfoService instanceInfoService;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private StepHelper stepHelper;
  @Inject private OutcomeService outcomeService;
  @Inject private TasEntityHelper tasEntityHelper;
  public static final String TANZU_COMMAND = "TanzuCommand";

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.CDS_TAS_NG)) {
      throw new AccessDeniedException(
              "CDS_TAS_NG FF is not enabled for this account. Please contact harness customer care.",
              ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
                                                              StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
          throws Exception {
    return tasStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
                                                           PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof StepExceptionPassThroughData) {
      StepExceptionPassThroughData stepExceptionPassThroughData = (StepExceptionPassThroughData) passThroughData;
      return StepResponse.builder()
              .status(Status.FAILED)
              .unitProgressList(stepExceptionPassThroughData.getUnitProgressData().getUnitProgresses())
              .failureInfo(FailureInfo.newBuilder().setErrorMessage(stepExceptionPassThroughData.getErrorMessage()).build())
              .build();
    }
    return StepResponse.builder()
            .status(Status.SUCCEEDED)
            .unitProgressList(Collections.singletonList(UnitProgress.newBuilder()
                    .setUnitName(CfCommandUnitConstants.Pcfplugin)
                    .setStatus(UnitStatus.SUCCESS)
                    .setStartTime(System.currentTimeMillis() - 5)
                    .setEndTime(System.currentTimeMillis())
                    .build()))
            .build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
          Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return tasStepHelper.startChainLinkForCommandStep(this, ambiance, stepParameters);
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  private List<FileData> prepareFilesForTransfer(Map<String, String> allFiles) {
    return allFiles.entrySet()
            .stream()
            .map(entry -> FileData.builder().filePath(entry.getKey()).fileContent(entry.getValue()).build())
            .collect(Collectors.toList());
  }

  @Override
  public TaskChainResponse executeTasTask(ManifestOutcome tasManifestOutcome, Ambiance ambiance,
                                          StepElementParameters stepParameters, TasExecutionPassThroughData executionPassThroughData,
                                          boolean shouldOpenFetchFilesLogStream, UnitProgressData unitProgressData) {
    TasCommandStepParameters tasCommandStepParameters = (TasCommandStepParameters) stepParameters.getSpec();
    InfrastructureOutcome infrastructureOutcome = cdStepHelper.getInfrastructureOutcome(ambiance);
    List<FileData> fileDataList = prepareFilesForTransfer(executionPassThroughData.getAllFilesFetched());
    TasInfraConfig tasInfraConfig = cdStepHelper.getTasInfraConfig(infrastructureOutcome, ambiance);

    CfCliVersionNG cfCliVersion = executionPassThroughData.getCfCliVersion();

    String accountId = AmbianceUtils.getAccountId(ambiance);
    int timeout = CDStepHelper.getTimeoutInMin(stepParameters);

    CfRunPluginCommandRequestNG cfRunPluginCommandRequestNG =
            CfRunPluginCommandRequestNG.builder()
                    .cfCliVersionNG(cfCliVersion)
                    .cfCommandTypeNG(CfCommandTypeNG.TANZU_COMMAND)
                    .fileDataList(fileDataList)
                    .tasInfraConfig(tasInfraConfig)
                    .accountId(accountId)
                    .timeoutIntervalInMin(timeout)
                    .commandName(TANZU_COMMAND)
                    .commandUnitsProgress(CommandUnitsProgress.builder().build())
                    .filePathsInScript(executionPassThroughData.getPathsFromScript())
                    .repoRoot(executionPassThroughData.getRepoRoot())
                    .renderedScriptString(executionPassThroughData.getRawScript())
                    .build();

    TaskData taskData = TaskData.builder()
            .parameters(new Object[] {cfRunPluginCommandRequestNG})
            .taskType(TaskType.CF_COMMAND_TASK_NG.name())
            .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
            .async(true)
            .build();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
            List.of(CfCommandUnitConstants.Pcfplugin), CF_COMMAND_TASK_NG.getDisplayName(),
            TaskSelectorYaml.toTaskSelector(tasCommandStepParameters.getDelegateSelectors()),
            stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
            .taskRequest(taskRequest)
            .chainEnd(true)
            .passThroughData(executionPassThroughData)
            .build();
  }
}
