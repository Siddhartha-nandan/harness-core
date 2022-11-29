/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.FileData;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.artifact.outcome.ArtifactOutcome;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.TanzuApplicationServiceInfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.tasconnector.TasConnectorDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfRunPluginCommandRequest;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.ng.core.BaseNGAccess;
import io.harness.pcf.CfCommandUnitConstants;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.shell.ScriptType;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.TaskType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.harness.beans.FeatureName.LIMIT_PCF_THREADS;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TasCommandStep extends TaskChainExecutableWithRollbackAndRbac implements TasStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.TAS_BG_APP_SETUP.getYamlType())
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

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.TAS_NG)) {
      throw new AccessDeniedException(
          "TAS_NG FF is not enabled for this account. Please contact harness customer care.",
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
                                                        .setUnitName(CfCommandUnitConstants.FetchFiles)
                                                        .setStatus(UnitStatus.SUCCESS)
                                                        .setStartTime(System.currentTimeMillis() - 5)
                                                        .setEndTime(System.currentTimeMillis())
                                                        .build()))
        .build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return tasStepHelper.startChainLink(this, ambiance, stepParameters);
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

    TasBGAppSetupStepParameters tasBGAppSetupStepParameters = (TasBGAppSetupStepParameters) stepParameters.getSpec();

    InfrastructureOutcome infrastructureOutcome = cdStepHelper.getInfrastructureOutcome(ambiance);
    List<FileData> fileDataList = prepareFilesForTransfer(executionPassThroughData.getAllFilesFetched());
    TasInfraConfig tasInfraConfig = cdStepHelper.getTasInfraConfig(infrastructureOutcome, ambiance);

    CfCliVersionNG cfCliVersion = executionPassThroughData.getCfCliVersion();

    CfCommandRequest commandRequest =
            CfRunPluginCommandRequest.builder()
                    .accountId(app.getAccountId())
                    .commandName(PCF_PLUGIN_COMMAND)
                    .pcfConfig(tasInfraConfig)
                    .timeoutIntervalInMin(timeoutIntervalInMin)
                    .renderedScriptString(resolveRenderedScript(rawScriptString, pcfPluginStateExecutionData))
                    .filePathsInScript(resolveFilePathsInScript(pathsFromScript, pcfPluginStateExecutionData))
                    .fileDataList(fileDataList)
                    .repoRoot(executionPassThroughData.getRepoRoot())
                    .cfCliVersion(cfCliVersion)
                    .build();

    TaskParameters taskParameters = ShellScriptTaskParametersNG.builder()
                                        .accountId(AmbianceUtils.getAccountId(ambiance))
                                        .environmentVariables(new HashMap<>())
                                        .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
                                        .script("OUTPUT_PATH_KEY='hello'")
                                        .executeOnDelegate(true)
                                        .scriptType(ScriptType.BASH)
                                        .workingDirectory("tmp")
                                        .outputVars(Collections.singletonList("OUTPUT_PATH_KEY"))
                                        .build();
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {taskParameters})
                            .taskType(TaskType.SHELL_SCRIPT_TASK_NG.name())
                            .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                            .async(true)
                            .build();

    final TaskRequest taskRequest =
        prepareCDTaskRequest(ambiance, taskData, kryoSerializer, List.of(CfCommandUnitConstants.FetchFiles),
            CfCommandUnitConstants.FetchFiles, null, stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(true)
        .passThroughData(executionPassThroughData)
        .build();
  }
}
