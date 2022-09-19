/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.ecs;

import static io.harness.cdng.manifest.yaml.harness.HarnessStoreConstants.HARNESS_STORE_TYPE;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static java.lang.String.format;

import io.harness.cdng.CDStepHelper;
import io.harness.cdng.ecs.beans.EcsExecutionPassThroughData;
import io.harness.cdng.ecs.beans.EcsFetchFileFailurePassThroughData;
import io.harness.cdng.ecs.beans.EcsFetchFilePassThroughData;
import io.harness.cdng.ecs.beans.EcsPrepareRollbackDataPassThroughData;
import io.harness.cdng.ecs.beans.EcsRollingRollbackDataOutcome;
import io.harness.cdng.ecs.beans.EcsRollingRollbackDataOutcome.EcsRollingRollbackDataOutcomeBuilder;
import io.harness.cdng.ecs.beans.EcsStepExceptionPassThroughData;
import io.harness.cdng.ecs.beans.EcsStepExecutorParams;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ecs.EcsCanaryDeployResult;
import io.harness.delegate.beans.ecs.EcsPrepareRollbackDataResult;
import io.harness.delegate.beans.ecs.EcsRollingDeployResult;
import io.harness.delegate.beans.ecs.EcsRollingRollbackResult;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.mapper.EcsTaskToServerInstanceInfoMapper;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.ecs.EcsFetchFileConfig;
import io.harness.delegate.task.ecs.EcsGitFetchFileConfig;
import io.harness.delegate.task.ecs.EcsInfraConfig;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.request.EcsFetchFileRequest;
import io.harness.delegate.task.ecs.request.EcsGitFetchRequest;
import io.harness.delegate.task.ecs.response.EcsCanaryDeployResponse;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.delegate.task.ecs.response.EcsFetchFileResponse;
import io.harness.delegate.task.ecs.response.EcsGitFetchResponse;
import io.harness.delegate.task.ecs.response.EcsPrepareRollbackDataResponse;
import io.harness.delegate.task.ecs.response.EcsRollingDeployResponse;
import io.harness.delegate.task.ecs.response.EcsRollingRollbackResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.git.model.FetchFilesResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;

public class EcsStepCommonHelper extends EcsStepUtils {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private EcsEntityHelper ecsEntityHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  public TaskChainResponse startChainLink(
      Ambiance ambiance, StepElementParameters stepElementParameters, EcsStepHelper ecsStepHelper) {
    // Get ManifestsOutcome
    ManifestsOutcome manifestsOutcome = resolveEcsManifestsOutcome(ambiance);

    // Get InfrastructureOutcome
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    // Update expressions in ManifestsOutcome
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));

    // Validate ManifestsOutcome
    validateManifestsOutcome(ambiance, manifestsOutcome);

    List<ManifestOutcome> ecsManifestOutcome = getEcsManifestOutcome(manifestsOutcome.values(), ecsStepHelper);

    return prepareEcsManifestFetchTask(
        ambiance, stepElementParameters, infrastructureOutcome, ecsManifestOutcome, ecsStepHelper);
  }

  public List<ManifestOutcome> getEcsManifestOutcome(
      @NotEmpty Collection<ManifestOutcome> manifestOutcomes, EcsStepHelper ecsStepHelper) {
    return ecsStepHelper.getEcsManifestOutcome(manifestOutcomes);
  }

  public ManifestsOutcome resolveEcsManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName =
          AmbianceUtils.getStageLevelFromAmbiance(ambiance).map(Level::getIdentifier).orElse("Deployment stage");
      String stepType =
          Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance)).map(StepType::getType).orElse("Ecs");
      throw new GeneralException(
          format("No manifests found in stage %s. %s step requires a manifest defined in stage service definition",
              stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  private TaskChainResponse prepareEcsManifestFetchTask(Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructureOutcome, List<ManifestOutcome> ecsManifestOutcomes,
      EcsStepHelper ecsStepHelper) {
    // Get EcsFetchFileConfig for task definition
    ManifestOutcome ecsTaskDefinitionManifestOutcome =
        ecsStepHelper.getEcsTaskDefinitionManifestOutcome(ecsManifestOutcomes);

    EcsFetchFileConfig ecsTaskDefinitionFetchFileConfig =
        getEcsFetchFilesConfigFromManifestOutcome(ecsTaskDefinitionManifestOutcome, ambiance, ecsStepHelper);

    // Get EcsFetchFileConfig for service definition
    ManifestOutcome ecsServiceDefinitionManifestOutcome =
        ecsStepHelper.getEcsServiceDefinitionManifestOutcome(ecsManifestOutcomes);

    EcsFetchFileConfig ecsServiceDefinitionFetchFileConfig =
        getEcsFetchFilesConfigFromManifestOutcome(ecsServiceDefinitionManifestOutcome, ambiance, ecsStepHelper);

    // Get EcsFetchFileConfig list for scalable targets if present
    List<EcsFetchFileConfig> ecsScalableTargetFetchFileConfigs = null;

    List<ManifestOutcome> ecsScalableTargetManifestOutcomes =
        ecsStepHelper.getManifestOutcomesByType(ecsManifestOutcomes, ManifestType.EcsScalableTargetDefinition);

    if (CollectionUtils.isNotEmpty(ecsScalableTargetManifestOutcomes)) {
      ecsScalableTargetFetchFileConfigs =
          ecsScalableTargetManifestOutcomes.stream()
              .map(manifestOutcome
                  -> getEcsFetchFilesConfigFromManifestOutcome(manifestOutcome, ambiance, ecsStepHelper))
              .collect(Collectors.toList());
    }

    // Get EcsFetchFileConfig list for scaling policies if present
    List<EcsFetchFileConfig> ecsScalingPolicyFetchFileConfigs = null;

    List<ManifestOutcome> ecsScalingPolicyManifestOutcomes =
        ecsStepHelper.getManifestOutcomesByType(ecsManifestOutcomes, ManifestType.EcsScalingPolicyDefinition);

    if (CollectionUtils.isNotEmpty(ecsScalingPolicyManifestOutcomes)) {
      ecsScalingPolicyFetchFileConfigs =
          ecsScalingPolicyManifestOutcomes.stream()
              .map(manifestOutcome
                  -> getEcsFetchFilesConfigFromManifestOutcome(manifestOutcome, ambiance, ecsStepHelper))
              .collect(Collectors.toList());
    }

    EcsFetchFilePassThroughData ecsFetchFilePassThroughData =
        EcsFetchFilePassThroughData.builder().infrastructureOutcome(infrastructureOutcome).build();

    return getFetchFileTaskResponse(ambiance, true, stepElementParameters, ecsFetchFilePassThroughData,
        ecsTaskDefinitionFetchFileConfig, ecsServiceDefinitionFetchFileConfig, ecsScalableTargetFetchFileConfigs,
        ecsScalingPolicyFetchFileConfigs);
  }

  private EcsFetchFileConfig getEcsFetchFilesConfigFromManifestOutcome(
      ManifestOutcome manifestOutcome, Ambiance ambiance, EcsStepHelper ecsStepHelper) {
    StoreConfig storeConfig = manifestOutcome.getStore();
    if (!ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      throw new InvalidRequestException("Invalid kind of storeConfig for Ecs step", USER);
    }
    return getEcsFetchFilesConfig(ambiance, storeConfig, manifestOutcome, ecsStepHelper);
  }

  private EcsFetchFileConfig getEcsFetchFilesConfig(
      Ambiance ambiance, StoreConfig storeConfig, ManifestOutcome manifestOutcome, EcsStepHelper ecsStepHelper) {
    return EcsFetchFileConfig.builder()
        .storeDelegateConfig(getGitStoreDelegateConfig(ambiance, (GitStoreConfig) storeConfig, manifestOutcome))
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(false)
        .build();
  }

  private TaskChainResponse getFetchFileTaskResponse(Ambiance ambiance, boolean shouldOpenLogStream,
      StepElementParameters stepElementParameters, EcsFetchFilePassThroughData ecsFetchFilePassThroughData,
      EcsFetchFileConfig ecsTaskDefinitionFetchFileConfig, EcsFetchFileConfig ecsServiceDefinitionFetchFileConfig,
      List<EcsFetchFileConfig> ecsScalableTargetFetchFileConfigs,
      List<EcsFetchFileConfig> ecsScalingPolicyFetchFileConfigs) {
    String accountId = AmbianceUtils.getAccountId(ambiance);

    EcsFetchFileRequest ecsFetchFileRequest =
        EcsFetchFileRequest.builder()
            .accountId(accountId)
            .ecsTaskDefinitionFetchFileConfig(ecsTaskDefinitionFetchFileConfig)
            .ecsServiceDefinitionFetchFileConfig(ecsServiceDefinitionFetchFileConfig)
            .ecsScalableTargetFetchFileConfigs(ecsScalableTargetFetchFileConfigs)
            .ecsScalingPolicyFetchFileConfigs(ecsScalingPolicyFetchFileConfigs)
            .shouldOpenLogStream(shouldOpenLogStream)
            .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.ECS_GIT_FETCH_TASK_NG.name())
                                  .parameters(new Object[] {ecsFetchFileRequest})
                                  .build();

    String taskName = TaskType.ECS_GIT_FETCH_TASK_NG.getDisplayName();

    EcsSpecParameters ecsSpecParameters = (EcsSpecParameters) stepElementParameters.getSpec();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        ecsSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(ecsSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(ecsFetchFilePassThroughData)
        .build();
  }

  public TaskChainResponse executeNextLinkRolling(EcsStepExecutor ecsStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier, EcsStepHelper ecsStepHelper) throws Exception {
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;
    TaskChainResponse taskChainResponse = null;
    try {
      if (responseData instanceof EcsFetchFileResponse) { // if EcsFetchFileResponse is received

        EcsFetchFileResponse ecsFetchFileResponse = (EcsFetchFileResponse) responseData;
        EcsFetchFilePassThroughData ecsFetchFilePassThroughData = (EcsFetchFilePassThroughData) passThroughData;

        taskChainResponse = handleEcsFetchFilesResponseRolling(ecsFetchFileResponse, ecsStepExecutor, ambiance,
            stepElementParameters, ecsFetchFilePassThroughData, ecsStepHelper);

      } else if (responseData
          instanceof EcsPrepareRollbackDataResponse) { // if EcsPrepareRollbackDataResponse is received

        EcsPrepareRollbackDataResponse ecsPrepareRollbackDataResponse = (EcsPrepareRollbackDataResponse) responseData;
        EcsPrepareRollbackDataPassThroughData ecsStepPassThroughData =
            (EcsPrepareRollbackDataPassThroughData) passThroughData;

        taskChainResponse = handleEcsPrepareRollbackDataResponseRolling(
            ecsPrepareRollbackDataResponse, ecsStepExecutor, ambiance, stepElementParameters, ecsStepPassThroughData);
      }
    } catch (Exception e) {
      taskChainResponse =
          TaskChainResponse.builder()
              .chainEnd(true)
              .passThroughData(
                  EcsStepExceptionPassThroughData.builder()
                      .errorMessage(ExceptionUtils.getMessage(e))
                      .unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e.getMessage()))
                      .build())
              .build();
    }

    return taskChainResponse;
  }

  public TaskChainResponse executeNextLinkCanary(EcsStepExecutor ecsStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier, EcsStepHelper ecsStepHelper) throws Exception {
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;
    TaskChainResponse taskChainResponse = null;
    try {
      if (responseData instanceof EcsFetchFileResponse) { // if EcsFetchFileResponse is received

        EcsFetchFileResponse ecsFetchFileResponse = (EcsFetchFileResponse) responseData;
        EcsFetchFilePassThroughData ecsFetchFilePassThroughData = (EcsFetchFilePassThroughData) passThroughData;

        taskChainResponse = handleEcsFetchFilesResponseCanary(ecsFetchFileResponse, ecsStepExecutor, ambiance,
            stepElementParameters, ecsFetchFilePassThroughData, ecsStepHelper);
      }
    } catch (Exception e) {
      taskChainResponse =
          TaskChainResponse.builder()
              .chainEnd(true)
              .passThroughData(
                  EcsStepExceptionPassThroughData.builder()
                      .errorMessage(ExceptionUtils.getMessage(e))
                      .unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e.getMessage()))
                      .build())
              .build();
    }

    return taskChainResponse;
  }

  public EcsInfraConfig getEcsInfraConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return ecsEntityHelper.getEcsInfraConfig(infrastructure, ngAccess);
  }

  private TaskChainResponse handleEcsFetchFilesResponseRolling(EcsFetchFileResponse ecsFetchFileResponse,
      EcsStepExecutor ecsStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters,
      EcsFetchFilePassThroughData ecsFetchFilePassThroughData, EcsStepHelper ecsStepHelper) {
    if (ecsFetchFileResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      EcsFetchFileFailurePassThroughData ecsFetchFileFailurePassThroughData =
          EcsFetchFileFailurePassThroughData.builder()
              .errorMsg(ecsFetchFileResponse.getErrorMessage())
              .unitProgressData(ecsFetchFileResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().passThroughData(ecsFetchFileFailurePassThroughData).chainEnd(true).build();
    }

    // Get ecsTaskDefinitionFileContent from ecsFetchFileResponse
    FetchFilesResult ecsTaskDefinitionFetchFileResult = ecsFetchFileResponse.getEcsTaskDefinitionFetchFilesResult();
    String ecsTaskDefinitionFileContent = ecsTaskDefinitionFetchFileResult.getFiles().get(0).getFileContent();
    ecsTaskDefinitionFileContent = engineExpressionService.renderExpression(ambiance, ecsTaskDefinitionFileContent);

    // Get ecsServiceDefinitionFetchFileResult from ecsFetchFileResponse
    FetchFilesResult ecsServiceDefinitionFetchFileResult =
        ecsFetchFileResponse.getEcsServiceDefinitionFetchFilesResult();
    String ecsServiceDefinitionFileContent = ecsServiceDefinitionFetchFileResult.getFiles().get(0).getFileContent();
    ecsServiceDefinitionFileContent =
        engineExpressionService.renderExpression(ambiance, ecsServiceDefinitionFileContent);

    // Get ecsScalableTargetManifestContentList from ecsFetchFileResponse if present
    List<String> ecsScalableTargetManifestContentList = null;
    List<FetchFilesResult> ecsScalableTargetFetchFilesResults =
        ecsFetchFileResponse.getEcsScalableTargetFetchFilesResults();

    if (CollectionUtils.isNotEmpty(ecsScalableTargetFetchFilesResults)) {
      ecsScalableTargetManifestContentList =
          ecsScalableTargetFetchFilesResults.stream()
              .map(ecsScalableTargetFetchFilesResult
                  -> ecsScalableTargetFetchFilesResult.getFiles().get(0).getFileContent())
              .collect(Collectors.toList());

      ecsScalableTargetManifestContentList =
          ecsScalableTargetManifestContentList.stream()
              .map(ecsScalableTargetManifestContent
                  -> engineExpressionService.renderExpression(ambiance, ecsScalableTargetManifestContent))
              .collect(Collectors.toList());
    }

    // Get ecsScalingPolicyManifestContentList from ecsFetchFileResponse if present
    List<String> ecsScalingPolicyManifestContentList = null;
    List<FetchFilesResult> ecsScalingPolicyFetchFilesResults =
        ecsFetchFileResponse.getEcsScalingPolicyFetchFilesResults();

    if (CollectionUtils.isNotEmpty(ecsScalingPolicyFetchFilesResults)) {
      ecsScalingPolicyManifestContentList =
          ecsScalingPolicyFetchFilesResults.stream()
              .map(ecsScalingPolicyFetchFilesResult
                  -> ecsScalingPolicyFetchFilesResult.getFiles().get(0).getFileContent())
              .collect(Collectors.toList());

      ecsScalingPolicyManifestContentList =
          ecsScalingPolicyManifestContentList.stream()
              .map(ecsScalingPolicyManifestContent
                  -> engineExpressionService.renderExpression(ambiance, ecsScalingPolicyManifestContent))
              .collect(Collectors.toList());
    }

    EcsPrepareRollbackDataPassThroughData ecsPrepareRollbackDataPassThroughData =
        EcsPrepareRollbackDataPassThroughData.builder()
            .infrastructureOutcome(ecsFetchFilePassThroughData.getInfrastructureOutcome())
            .ecsTaskDefinitionManifestContent(ecsTaskDefinitionFileContent)
            .ecsServiceDefinitionManifestContent(ecsServiceDefinitionFileContent)
            .ecsScalableTargetManifestContentList(ecsScalableTargetManifestContentList)
            .ecsScalingPolicyManifestContentList(ecsScalingPolicyManifestContentList)
            .build();

    return ecsStepExecutor.executeEcsPrepareRollbackTask(ambiance, stepElementParameters,
        ecsPrepareRollbackDataPassThroughData, ecsFetchFileResponse.getUnitProgressData());
  }

  private TaskChainResponse handleEcsFetchFilesResponseCanary(EcsFetchFileResponse ecsFetchFileResponse,
      EcsStepExecutor ecsStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters,
      EcsFetchFilePassThroughData ecsFetchFilePassThroughData, EcsStepHelper ecsStepHelper) {
    if (ecsFetchFileResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      EcsFetchFileFailurePassThroughData ecsFetchFileFailurePassThroughData =
          EcsFetchFileFailurePassThroughData.builder()
              .errorMsg(ecsFetchFileResponse.getErrorMessage())
              .unitProgressData(ecsFetchFileResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().passThroughData(ecsFetchFileFailurePassThroughData).chainEnd(true).build();
    }

    // Get ecsTaskDefinitionFileContent from ecsFetchFileResponse
    FetchFilesResult ecsTaskDefinitionFetchFileResult = ecsFetchFileResponse.getEcsTaskDefinitionFetchFilesResult();
    String ecsTaskDefinitionFileContent = ecsTaskDefinitionFetchFileResult.getFiles().get(0).getFileContent();
    ecsTaskDefinitionFileContent = engineExpressionService.renderExpression(ambiance, ecsTaskDefinitionFileContent);

    // Get ecsServiceDefinitionFetchFileResult from ecsFetchFileResponse
    FetchFilesResult ecsServiceDefinitionFetchFileResult =
        ecsFetchFileResponse.getEcsServiceDefinitionFetchFilesResult();
    String ecsServiceDefinitionFileContent = ecsServiceDefinitionFetchFileResult.getFiles().get(0).getFileContent();
    ecsServiceDefinitionFileContent =
        engineExpressionService.renderExpression(ambiance, ecsServiceDefinitionFileContent);

    // Get ecsScalableTargetManifestContentList from ecsFetchFileResponse if present
    List<String> ecsScalableTargetManifestContentList = null;
    List<FetchFilesResult> ecsScalableTargetFetchFilesResults =
        ecsFetchFileResponse.getEcsScalableTargetFetchFilesResults();

    if (CollectionUtils.isNotEmpty(ecsScalableTargetFetchFilesResults)) {
      ecsScalableTargetManifestContentList =
          ecsScalableTargetFetchFilesResults.stream()
              .map(ecsScalableTargetFetchFilesResult
                  -> ecsScalableTargetFetchFilesResult.getFiles().get(0).getFileContent())
              .collect(Collectors.toList());

      ecsScalableTargetManifestContentList =
          ecsScalableTargetManifestContentList.stream()
              .map(ecsScalableTargetManifestContent
                  -> engineExpressionService.renderExpression(ambiance, ecsScalableTargetManifestContent))
              .collect(Collectors.toList());
    }

    // Get ecsScalingPolicyManifestContentList from ecsFetchFileResponse if present
    List<String> ecsScalingPolicyManifestContentList = null;
    List<FetchFilesResult> ecsScalingPolicyFetchFilesResults =
        ecsFetchFileResponse.getEcsScalingPolicyFetchFilesResults();

    if (CollectionUtils.isNotEmpty(ecsScalingPolicyFetchFilesResults)) {
      ecsScalingPolicyManifestContentList =
          ecsScalingPolicyFetchFilesResults.stream()
              .map(ecsScalingPolicyFetchFilesResult
                  -> ecsScalingPolicyFetchFilesResult.getFiles().get(0).getFileContent())
              .collect(Collectors.toList());

      ecsScalingPolicyManifestContentList =
          ecsScalingPolicyManifestContentList.stream()
              .map(ecsScalingPolicyManifestContent
                  -> engineExpressionService.renderExpression(ambiance, ecsScalingPolicyManifestContent))
              .collect(Collectors.toList());
    }

    EcsExecutionPassThroughData ecsExecutionPassThroughData =
        EcsExecutionPassThroughData.builder()
            .infrastructure(ecsFetchFilePassThroughData.getInfrastructureOutcome())
            .lastActiveUnitProgressData(ecsFetchFileResponse.getUnitProgressData())
            .build();

    EcsStepExecutorParams ecsStepExecutorParams =
        EcsStepExecutorParams.builder()
            .shouldOpenFetchFilesLogStream(false)
            .ecsTaskDefinitionManifestContent(ecsTaskDefinitionFileContent)
            .ecsServiceDefinitionManifestContent(ecsServiceDefinitionFileContent)
            .ecsScalableTargetManifestContentList(ecsScalableTargetManifestContentList)
            .ecsScalingPolicyManifestContentList(ecsScalingPolicyManifestContentList)
            .build();

    return ecsStepExecutor.executeEcsTask(ambiance, stepElementParameters, ecsExecutionPassThroughData,
        ecsFetchFileResponse.getUnitProgressData(), ecsStepExecutorParams);
  }

  private TaskChainResponse handleEcsPrepareRollbackDataResponseRolling(
      EcsPrepareRollbackDataResponse ecsPrepareRollbackDataResponse, EcsStepExecutor ecsStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, EcsPrepareRollbackDataPassThroughData ecsStepPassThroughData) {
    if (ecsPrepareRollbackDataResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      EcsStepExceptionPassThroughData ecsStepExceptionPassThroughData =
          EcsStepExceptionPassThroughData.builder()
              .errorMessage(ecsPrepareRollbackDataResponse.getErrorMessage())
              .unitProgressData(ecsPrepareRollbackDataResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().passThroughData(ecsStepExceptionPassThroughData).chainEnd(true).build();
    }

    if (ecsStepExecutor instanceof EcsRollingDeployStep) {
      EcsPrepareRollbackDataResult ecsPrepareRollbackDataResult =
          ecsPrepareRollbackDataResponse.getEcsPrepareRollbackDataResult();

      EcsRollingRollbackDataOutcomeBuilder ecsRollbackDataOutcomeBuilder = EcsRollingRollbackDataOutcome.builder();

      ecsRollbackDataOutcomeBuilder.serviceName(ecsPrepareRollbackDataResult.getServiceName());
      ecsRollbackDataOutcomeBuilder.createServiceRequestBuilderString(
          ecsPrepareRollbackDataResult.getCreateServiceRequestBuilderString());
      ecsRollbackDataOutcomeBuilder.isFirstDeployment(ecsPrepareRollbackDataResult.isFirstDeployment());
      ecsRollbackDataOutcomeBuilder.registerScalableTargetRequestBuilderStrings(
          ecsPrepareRollbackDataResult.getRegisterScalableTargetRequestBuilderStrings());
      ecsRollbackDataOutcomeBuilder.registerScalingPolicyRequestBuilderStrings(
          ecsPrepareRollbackDataResult.getRegisterScalingPolicyRequestBuilderStrings());

      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.ECS_ROLLING_ROLLBACK_OUTCOME,
          ecsRollbackDataOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
    }

    EcsExecutionPassThroughData ecsExecutionPassThroughData =
        EcsExecutionPassThroughData.builder()
            .infrastructure(ecsStepPassThroughData.getInfrastructureOutcome())
            .lastActiveUnitProgressData(ecsPrepareRollbackDataResponse.getUnitProgressData())
            .build();

    String ecsTaskDefinitionFileContent = ecsStepPassThroughData.getEcsTaskDefinitionManifestContent();

    String ecsServiceDefinitionFileContent = ecsStepPassThroughData.getEcsServiceDefinitionManifestContent();

    List<String> ecsScalableTargetManifestContentList =
        ecsStepPassThroughData.getEcsScalableTargetManifestContentList();

    List<String> ecsScalingPolicyManifestContentList = ecsStepPassThroughData.getEcsScalingPolicyManifestContentList();

    EcsStepExecutorParams ecsStepExecutorParams =
        EcsStepExecutorParams.builder()
            .shouldOpenFetchFilesLogStream(false)
            .ecsTaskDefinitionManifestContent(ecsTaskDefinitionFileContent)
            .ecsServiceDefinitionManifestContent(ecsServiceDefinitionFileContent)
            .ecsScalableTargetManifestContentList(ecsScalableTargetManifestContentList)
            .ecsScalingPolicyManifestContentList(ecsScalingPolicyManifestContentList)
            .build();

    return ecsStepExecutor.executeEcsTask(ambiance, stepElementParameters, ecsExecutionPassThroughData,
        ecsPrepareRollbackDataResponse.getUnitProgressData(), ecsStepExecutorParams);
  }

  public TaskChainResponse queueEcsTask(StepElementParameters stepElementParameters,
      EcsCommandRequest ecsCommandRequest, Ambiance ambiance, PassThroughData passThroughData, boolean isChainEnd) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {ecsCommandRequest})
                            .taskType(TaskType.ECS_COMMAND_TASK_NG.name())
                            .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                            .async(true)
                            .build();

    String taskName = TaskType.ECS_COMMAND_TASK_NG.getDisplayName() + " : " + ecsCommandRequest.getCommandName();

    EcsSpecParameters ecsSpecParameters = (EcsSpecParameters) stepElementParameters.getSpec();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        ecsSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(ecsSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(isChainEnd)
        .passThroughData(passThroughData)
        .build();
  }

  public StepResponse handleFetchFileTaskFailure(
      EcsFetchFileFailurePassThroughData ecsFetchFileFailurePassThroughData) {
    UnitProgressData unitProgressData = ecsFetchFileFailurePassThroughData.getUnitProgressData();
    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(ecsFetchFileFailurePassThroughData.getErrorMsg()).build())
        .build();
  }

  public StepResponse handleStepExceptionFailure(EcsStepExceptionPassThroughData stepException) {
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(io.harness.eraro.Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(HarnessStringUtils.emptyIfNull(stepException.getErrorMessage()))
                                  .build();
    return StepResponse.builder()
        .unitProgressList(stepException.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public StepResponse handleTaskException(
      Ambiance ambiance, EcsExecutionPassThroughData executionPassThroughData, Exception e) throws Exception {
    if (ExceptionUtils.cause(TaskNGDataException.class, e) != null) {
      throw e;
    }

    UnitProgressData unitProgressData =
        completeUnitProgressData(executionPassThroughData.getLastActiveUnitProgressData(), ambiance, e.getMessage());
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(io.harness.eraro.Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(HarnessStringUtils.emptyIfNull(ExceptionUtils.getMessage(e)))
                                  .build();

    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public static StepResponseBuilder getFailureResponseBuilder(
      EcsCommandResponse ecsCommandResponse, StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(
            FailureInfo.newBuilder().setErrorMessage(EcsStepCommonHelper.getErrorMessage(ecsCommandResponse)).build());
    return stepResponseBuilder;
  }

  public static String getErrorMessage(EcsCommandResponse ecsCommandResponse) {
    return ecsCommandResponse.getErrorMessage() == null ? "" : ecsCommandResponse.getErrorMessage();
  }

  public List<ServerInstanceInfo> getServerInstanceInfos(
      EcsCommandResponse ecsCommandResponse, String infrastructureKey) {
    if (ecsCommandResponse instanceof EcsRollingDeployResponse) {
      EcsRollingDeployResult ecsRollingDeployResult =
          ((EcsRollingDeployResponse) ecsCommandResponse).getEcsRollingDeployResult();
      return EcsTaskToServerInstanceInfoMapper.toServerInstanceInfoList(
          ecsRollingDeployResult.getEcsTasks(), infrastructureKey, ecsRollingDeployResult.getRegion());
    } else if (ecsCommandResponse instanceof EcsRollingRollbackResponse) {
      EcsRollingRollbackResult ecsRollingRollbackResult =
          ((EcsRollingRollbackResponse) ecsCommandResponse).getEcsRollingRollbackResult();
      return EcsTaskToServerInstanceInfoMapper.toServerInstanceInfoList(
          ecsRollingRollbackResult.getEcsTasks(), infrastructureKey, ecsRollingRollbackResult.getRegion());
    } else if (ecsCommandResponse instanceof EcsCanaryDeployResponse) {
      EcsCanaryDeployResult ecsCanaryDeployResult =
          ((EcsCanaryDeployResponse) ecsCommandResponse).getEcsCanaryDeployResult();
      return EcsTaskToServerInstanceInfoMapper.toServerInstanceInfoList(
          ecsCanaryDeployResult.getEcsTasks(), infrastructureKey, ecsCanaryDeployResult.getRegion());
    }
    throw new GeneralException("Invalid ecs command response instance");
  }
}
