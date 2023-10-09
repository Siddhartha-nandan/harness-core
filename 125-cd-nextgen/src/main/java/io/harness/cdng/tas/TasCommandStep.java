/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.beans.FileData;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.k8s.beans.CustomFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.tas.outcome.TanzuCommandOutcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.pcf.CfCommandTypeNG;
import io.harness.delegate.task.pcf.request.CfRunPluginCommandRequestNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.delegate.task.pcf.response.TasRunPluginResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.plancreator.steps.TaskSelectorYaml;
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
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.StepHelper;
import io.harness.steps.TaskRequestsUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PCF})
@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class TasCommandStep extends TaskChainExecutableWithRollbackAndRbac implements TasStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.TANZU_COMMAND.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private TasStepHelper tasStepHelper;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private InstanceInfoService instanceInfoService;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private StepHelper stepHelper;
  @Inject private OutcomeService outcomeService;
  @Inject private TasEntityHelper tasEntityHelper;
  @Inject private io.harness.cdng.expressions.CDExpressionResolver cdExpressionResolver;
  public static final String TANZU_COMMAND = "TanzuCommand";

  @Override
  public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.NG_SVC_ENV_REDESIGN)) {
      throw new AccessDeniedException(
          "NG_SVC_ENV_REDESIGN FF is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepBaseParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    return tasStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepBaseParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    try {
      if (passThroughData instanceof GitFetchResponsePassThroughData) {
        GitFetchResponsePassThroughData stepExceptionPassThroughData =
            (GitFetchResponsePassThroughData) passThroughData;
        return StepResponse.builder()
            .status(Status.FAILED)
            .unitProgressList(stepExceptionPassThroughData.getUnitProgressData().getUnitProgresses())
            .failureInfo(FailureInfo.newBuilder().setErrorMessage(stepExceptionPassThroughData.getErrorMsg()).build())
            .build();
      }
      if (passThroughData instanceof CustomFetchResponsePassThroughData) {
        CustomFetchResponsePassThroughData stepExceptionPassThroughData =
            (CustomFetchResponsePassThroughData) passThroughData;
        return StepResponse.builder()
            .status(Status.FAILED)
            .unitProgressList(stepExceptionPassThroughData.getUnitProgressData().getUnitProgresses())
            .failureInfo(FailureInfo.newBuilder().setErrorMessage(stepExceptionPassThroughData.getErrorMsg()).build())
            .build();
      }
      if (passThroughData instanceof StepExceptionPassThroughData) {
        StepExceptionPassThroughData stepExceptionPassThroughData = (StepExceptionPassThroughData) passThroughData;
        return StepResponse.builder()
            .status(Status.FAILED)
            .unitProgressList(stepExceptionPassThroughData.getUnitProgressData().getUnitProgresses())
            .failureInfo(
                FailureInfo.newBuilder().setErrorMessage(stepExceptionPassThroughData.getErrorMessage()).build())
            .build();
      }
      TasRunPluginResponse response;
      try {
        response = (TasRunPluginResponse) responseDataSupplier.get();
      } catch (Exception ex) {
        log.error("Error while processing Tas response: {}", ExceptionUtils.getMessage(ex), ex);
        throw ex;
      }
      if (!response.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
        return StepResponse.builder()
            .status(Status.FAILED)
            .failureInfo(FailureInfo.newBuilder().setErrorMessage(response.getErrorMessage()).build())
            .unitProgressList(
                tasStepHelper
                    .completeUnitProgressData(response.getUnitProgressData(), ambiance, response.getErrorMessage())
                    .getUnitProgresses())
            .build();
      }
      TasCommandStepParameters tasCommandStepParameters = (TasCommandStepParameters) stepParameters.getSpec();
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(OutputExpressionConstants.OUTPUT)
                           .outcome(prepareTanzuCommandOutcome(
                               response.getOutputVariables(), tasCommandStepParameters.getOutputVariables()))
                           .build())
          .unitProgressList(response.getUnitProgressData().getUnitProgresses())
          .build();
    } finally {
      tasStepHelper.closeLogStream(ambiance);
    }
  }

  private TanzuCommandOutcome prepareTanzuCommandOutcome(
      Map<String, String> response, Map<String, Object> outputVariables) {
    if (outputVariables == null || response == null) {
      return null;
    }
    Map<String, String> resolvedOutputVariables = new HashMap<>();
    outputVariables.keySet().forEach(name -> {
      Object value = ((ParameterField<?>) outputVariables.get(name)).getValue();
      resolvedOutputVariables.put(name, response.get(value));
    });
    return TanzuCommandOutcome.builder().outputVariables(resolvedOutputVariables).build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepBaseParameters stepParameters, StepInputPackage inputPackage) {
    return tasStepHelper.startChainLinkForCommandStep(this, ambiance, stepParameters);
  }

  @Override
  public Class<StepBaseParameters> getStepParametersClass() {
    return StepBaseParameters.class;
  }

  private List<FileData> prepareFilesForTransfer(Map<String, String> allFiles) {
    return allFiles.entrySet()
        .stream()
        .map(entry -> FileData.builder().filePath(entry.getKey()).fileContent(entry.getValue()).build())
        .collect(Collectors.toList());
  }

  @Override
  public TaskChainResponse executeTasTask(ManifestOutcome tasManifestOutcome, Ambiance ambiance,
      StepBaseParameters stepParameters, TasExecutionPassThroughData executionPassThroughData,
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
            .cfCliVersion(tasStepHelper.cfCliVersionNGMapper(cfCliVersion))
            .cfCommandTypeNG(CfCommandTypeNG.TANZU_COMMAND)
            .fileDataList(fileDataList)
            .tasInfraConfig(tasInfraConfig)
            .accountId(accountId)
            .timeoutIntervalInMin(timeout)
            .commandName(CfCommandTypeNG.TANZU_COMMAND.name())
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .filePathsInScript(executionPassThroughData.getPathsFromScript())
            .repoRoot(executionPassThroughData.getRepoRoot())
            .renderedScriptString(executionPassThroughData.getRawScript())
            .useCfCLI(true)
            .inputVariables(getInputVariables(tasCommandStepParameters.getInputVariables(), ambiance))
            .outputVariables(getOutputVars(tasCommandStepParameters.getOutputVariables()))
            .build();

    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {cfRunPluginCommandRequestNG})
                            .taskType(TaskType.TANZU_COMMAND.name())
                            .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                            .async(true)
                            .build();

    final TaskRequest taskRequest =
        TaskRequestsUtils.prepareCDTaskRequest(ambiance, taskData, referenceFalseKryoSerializer,
            executionPassThroughData.getCommandUnits(), TaskType.TANZU_COMMAND.getDisplayName(),
            TaskSelectorYaml.toTaskSelector(tasCommandStepParameters.getDelegateSelectors()),
            stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(true)
        .passThroughData(executionPassThroughData)
        .build();
  }

  public Map<String, String> getInputVariables(Map<String, Object> inputVariables, Ambiance ambiance) {
    Map<String, String> res = new LinkedHashMap<>();
    if (inputVariables == null) {
      return res;
    }

    inputVariables.forEach((key, value) -> {
      if (value instanceof ParameterField) {
        ParameterField<?> parameterFieldValue = (ParameterField<?>) value;
        if (parameterFieldValue.fetchFinalValue() == null) {
          throw new InvalidRequestException(String.format("Env. variable [%s] value found to be null", key));
        }
        res.put(key, parameterFieldValue.fetchFinalValue().toString());
      } else if (value instanceof String) {
        res.put(key, (String) value);
      } else {
        log.error(String.format(
            "Value other than String or ParameterField found for env. variable [%s]. value: [%s]", key, value));
      }
    });

    cdExpressionResolver.updateExpressions(ambiance, res);

    // check for unresolved harness expressions
    StringBuilder unresolvedInputVariables = new StringBuilder();
    res.forEach((key, value) -> {
      if (EngineExpressionEvaluator.hasExpressions(value)) {
        unresolvedInputVariables.append(key).append(", ");
      }
    });

    // Remove the trailing comma and whitespace, if any
    if (unresolvedInputVariables.length() > 0) {
      unresolvedInputVariables.setLength(unresolvedInputVariables.length() - 2);
      throw new InvalidRequestException(
          String.format("Env. variables: [%s] found to be unresolved", unresolvedInputVariables));
    }

    return res;
  }

  public List<String> getOutputVars(Map<String, Object> outputVariables) {
    if (EmptyPredicate.isEmpty(outputVariables)) {
      return emptyList();
    }
    List<String> outputVars = new ArrayList<>();
    outputVariables.forEach((key, val) -> {
      if (val instanceof ParameterField) {
        ParameterField<?> parameterFieldValue = (ParameterField<?>) val;
        if (parameterFieldValue.getValue() == null) {
          throw new InvalidRequestException(String.format("Output variable [%s] value found to be empty", key));
        }
        outputVars.add(((ParameterField<?>) val).getValue().toString());
      } else if (val instanceof String) {
        outputVars.add((String) val);
      } else {
        log.error(String.format(
            "Value other than String or ParameterField found for output variable [%s]. value: [%s]", key, val));
      }
    });
    return outputVars;
  }
}
