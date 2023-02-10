/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.lambda;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.aws.lambda.AwsLambdaCommandUnitConstants;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.aws.lambda.beans.AwsLambdaStepOutcome;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.mappers.ManifestOutcomeValidator;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.aws.lambda.AwsLambda;
import io.harness.delegate.task.aws.lambda.AwsLambdaCommandTypeNG;
import io.harness.delegate.task.aws.lambda.AwsLambdaFunctionsInfraConfig;
import io.harness.delegate.task.aws.lambda.request.AwsLambdaCommandRequest;
import io.harness.delegate.task.aws.lambda.request.AwsLambdaPrepareRollbackRequest;
import io.harness.delegate.task.aws.lambda.response.AwsLambdaDeployResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.gitcommon.GitRequestFileConfig;
import io.harness.delegate.task.gitcommon.GitTaskNGRequest;
import io.harness.delegate.task.gitcommon.GitTaskNGResponse;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.googlefunctions.command.GoogleFunctionsCommandUnitConstants;
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
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.TaskRequestsUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
public class AwsLambdaHelper extends CDStepHelper {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private AwsLambdaEntityHelper awsLambdaEntityHelper;

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;

  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;

  @Inject private PipelineRbacHelper pipelineRbacHelper;

  private final String AWS_LAMBDA_PREPARE_ROLLBACK_COMMAND_NAME = "PrepareRollbackAwsLambda";

  public AwsLambdaFunctionsInfraConfig getInfraConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    return awsLambdaEntityHelper.getInfraConfig(infrastructure, AmbianceUtils.getNgAccess(ambiance));
  }

  public TaskChainResponse queueTask(StepElementParameters stepElementParameters,
      AwsLambdaCommandRequest awsLambdaCommandRequest, Ambiance ambiance, PassThroughData passThroughData,
      boolean isChainEnd) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {awsLambdaCommandRequest})
                            .taskType(TaskType.AWS_LAMBDA_DEPLOY_COMMAND_TASK_NG.name())
                            .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                            .async(true)
                            .build();
    String taskName =
        TaskType.AWS_LAMBDA_DEPLOY_COMMAND_TASK_NG.getDisplayName() + " : " + awsLambdaCommandRequest.getCommandName();
    AwsLambdaSpecParameters awsLambdaSpecParameters = (AwsLambdaSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = TaskRequestsUtils.prepareCDTaskRequest(ambiance, taskData,
        referenceFalseKryoSerializer, awsLambdaSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(
            emptyIfNull(getParameterFieldValue(awsLambdaSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(isChainEnd)
        .passThroughData(passThroughData)
        .build();
  }

  public ManifestOutcome getAwsLambdaManifestOutcome(@NotEmpty Collection<ManifestOutcome> manifestOutcomes) {
    // Filter only Aws Lambda supported manifest types
    List<ManifestOutcome> awsLambdaManifests =
        manifestOutcomes.stream()
            .filter(
                manifestOutcome -> ManifestType.AWS_LAMBDA_SUPPORTED_MANIFEST_TYPES.contains(manifestOutcome.getType()))
            .collect(Collectors.toList());

    // Check if Aws Lambda Manifests are empty
    if (isEmpty(awsLambdaManifests)) {
      throw new InvalidRequestException("Aws Lambda Manifest is mandatory.", USER);
    }
    return awsLambdaManifests.get(0);
  }

  public TaskChainResponse executeNextLink(Ambiance ambiance, StepElementParameters stepElementParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    ResponseData responseData = responseDataSupplier.get();

    AwsLambdaStepPassThroughData awsLambdaStepPassThroughData = (AwsLambdaStepPassThroughData) passThroughData;

    UnitProgressData unitProgressData = null;
    TaskChainResponse taskChainResponse = null;

    try {
      if (responseData instanceof GitTaskNGResponse) {
        GitTaskNGResponse gitTaskResponse = (GitTaskNGResponse) responseData;
        taskChainResponse =
            handleGitFetchFilesResponse(ambiance, stepElementParameters, gitTaskResponse, awsLambdaStepPassThroughData);
      }
    } catch (Exception e) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(AwsLambdaStepExceptionPassThroughData.builder()
                               .errorMsg(ExceptionUtils.getMessage(e))
                               .unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e.getMessage()))
                               .build())
          .build();
    }

    return taskChainResponse;
  }

  private TaskChainResponse handleGitFetchFilesResponse(Ambiance ambiance, StepElementParameters stepParameters,
      GitTaskNGResponse gitTaskResponse, AwsLambdaStepPassThroughData awsLambdaStepPassThroughData) {
    if (gitTaskResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      AwsLambdaStepExceptionPassThroughData awsLambdaStepExceptionPassThroughData =
          AwsLambdaStepExceptionPassThroughData.builder()
              .errorMsg(gitTaskResponse.getErrorMessage())
              .unitProgressData(gitTaskResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().passThroughData(awsLambdaStepExceptionPassThroughData).chainEnd(true).build();
    }
    String manifestContent = getManifestContentFromGitResponse(gitTaskResponse, ambiance);

    AwsLambdaStepPassThroughData awsLambdaPrepareRollbackStepPassThroughData =
        AwsLambdaStepPassThroughData.builder()
            .manifestsOutcome(awsLambdaStepPassThroughData.getManifestsOutcome())
            .manifestContent(manifestContent)
            .infrastructureOutcome(awsLambdaStepPassThroughData.getInfrastructureOutcome())
            .build();
    return executePrepareRollbackTask(
        ambiance, stepParameters, awsLambdaPrepareRollbackStepPassThroughData, gitTaskResponse.getUnitProgressData());
  }

  public TaskChainResponse executePrepareRollbackTask(Ambiance ambiance, StepElementParameters stepParameters,
      AwsLambdaStepPassThroughData awsLambdaStepPassThroughData, UnitProgressData unitProgressData) {
    InfrastructureOutcome infrastructureOutcome = awsLambdaStepPassThroughData.getInfrastructureOutcome();
    AwsLambdaPrepareRollbackRequest.AwsLambdaPrepareRollbackRequestBuilder awsLambdaPrepareRollbackRequestBuilder =
        AwsLambdaPrepareRollbackRequest.builder()
            .awsLambdaCommandTypeNG(AwsLambdaCommandTypeNG.AWS_LAMBDA_PREPARE_ROLLBACK)
            .commandName(AWS_LAMBDA_PREPARE_ROLLBACK_COMMAND_NAME)
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .awsLambdaDeployManifestContent(awsLambdaStepPassThroughData.getManifestContent())
            .awsLambdaFunctionsInfraConfig(getInfraConfig(infrastructureOutcome, ambiance));
    return queueTask(
        stepParameters, awsLambdaPrepareRollbackRequestBuilder.build(), ambiance, awsLambdaStepPassThroughData, false);
  }

  private String getManifestContentFromGitResponse(GitTaskNGResponse gitTaskResponse, Ambiance ambiance) {
    String manifestContent = gitTaskResponse.getGitFetchFilesResults().get(0).getFiles().get(0).getFileContent();
    return engineExpressionService.renderExpression(ambiance, manifestContent);
  }

  public StepResponse handleStepFailureException(
      Ambiance ambiance, AwsLambdaStepPassThroughData stepPassThroughData, Exception e) throws Exception {
    if (ExceptionUtils.cause(TaskNGDataException.class, e) != null) {
      throw e;
    }

    UnitProgressData unitProgressData =
        completeUnitProgressData(stepPassThroughData.getUnitProgressData(), ambiance, e.getMessage());
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

  public static StepResponse.StepResponseBuilder getFailureResponseBuilder(
      AwsLambdaDeployResponse awsLambdaDeployResponse, StepResponse.StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(
            FailureInfo.newBuilder()
                .setErrorMessage(
                    awsLambdaDeployResponse.getErrorMessage() == null ? "" : awsLambdaDeployResponse.getErrorMessage())
                .build());
    return stepResponseBuilder;
  }

  public AwsLambdaStepOutcome getAwsLambdaStepOutcome(AwsLambda awsLambda) {
    return AwsLambdaStepOutcome.builder()
        .functionName(awsLambda.getFunctionName())
        .runtime(awsLambda.getRuntime())
        .build();
  }

  public TaskChainResponse startChainLink(Ambiance ambiance, StepElementParameters stepElementParameters) {
    // Get ManifestsOutcome
    ManifestsOutcome manifestsOutcome = resolveAwsLambdaManifestsOutcome(ambiance);

    // Get InfrastructureOutcome
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    // Update expressions in ManifestsOutcome
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));

    // Validate ManifestsOutcome
    validateManifestsOutcome(ambiance, manifestsOutcome);

    ManifestOutcome awsLambdaManifestOutcome = getAwsLambdaManifestOutcome(manifestsOutcome.values());

    return prepareManifestGitFetchTask(
        infrastructureOutcome, ambiance, stepElementParameters, awsLambdaManifestOutcome);
  }

  private ManifestsOutcome resolveAwsLambdaManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName =
          AmbianceUtils.getStageLevelFromAmbiance(ambiance).map(Level::getIdentifier).orElse("Deployment stage");
      String stepType = Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance))
                            .map(StepType::getType)
                            .orElse("Google Function");
      throw new GeneralException(
          format("No manifests found in stage %s. %s step requires a manifest defined in stage service definition",
              stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  public void validateManifestsOutcome(Ambiance ambiance, ManifestsOutcome manifestsOutcome) {
    Set<EntityDetailProtoDTO> entityDetails = new HashSet<>();
    manifestsOutcome.values().forEach(value -> {
      entityDetails.addAll(entityReferenceExtractorUtils.extractReferredEntities(ambiance, value.getStore()));
      ManifestOutcomeValidator.validate(value, false);
    });

    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
  }

  private TaskChainResponse prepareManifestGitFetchTask(InfrastructureOutcome infrastructureOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, ManifestOutcome manifestOutcome) {
    GitRequestFileConfig gitRequestFileConfig = null;

    if (ManifestStoreType.isInGitSubset(manifestOutcome.getStore().getKind())) {
      gitRequestFileConfig = getGitFetchFilesConfigFromManifestOutcome(manifestOutcome, ambiance);
    }

    AwsLambdaStepPassThroughData awsLambdaStepPassThroughData = AwsLambdaStepPassThroughData.builder()
                                                                    .manifestsOutcome(manifestOutcome)
                                                                    .infrastructureOutcome(infrastructureOutcome)
                                                                    .build();

    return getGitFetchFileTaskResponse(
        ambiance, false, stepElementParameters, awsLambdaStepPassThroughData, gitRequestFileConfig);
  }

  private GitRequestFileConfig getGitFetchFilesConfigFromManifestOutcome(
      ManifestOutcome manifestOutcome, Ambiance ambiance) {
    StoreConfig storeConfig = manifestOutcome.getStore();
    if (!ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      throw new InvalidRequestException("Invalid kind of storeConfig for Ecs step", USER);
    }
    GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
    return getGitFetchFilesConfig(ambiance, gitStoreConfig, manifestOutcome);
  }

  private GitRequestFileConfig getGitFetchFilesConfig(
      Ambiance ambiance, GitStoreConfig gitStoreConfig, ManifestOutcome manifestOutcome) {
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    String validationMessage = format("Aws Lambda manifest with Id [%s]", manifestOutcome.getIdentifier());
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    ConnectorInfoDTO connectorDTO = awsLambdaEntityHelper.getConnectorInfoDTO(connectorId, ngAccess);
    validateManifest(gitStoreConfig.getKind(), connectorDTO, validationMessage);
    return GitRequestFileConfig.builder()
        .gitStoreDelegateConfig(getGitStoreDelegateConfig(
            gitStoreConfig, connectorDTO, manifestOutcome, gitStoreConfig.getPaths().getValue(), ambiance))
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(false)
        .build();
  }

  private TaskChainResponse getGitFetchFileTaskResponse(Ambiance ambiance, boolean shouldOpenLogStream,
      StepElementParameters stepElementParameters, AwsLambdaStepPassThroughData awsLambdaStepPassThroughData,
      GitRequestFileConfig gitRequestFileConfig) {
    String accountId = AmbianceUtils.getAccountId(ambiance);

    GitTaskNGRequest gitTaskNGRequest = GitTaskNGRequest.builder()
                                            .accountId(accountId)
                                            .gitRequestFileConfigs(Collections.singletonList(gitRequestFileConfig))
                                            .shouldOpenLogStream(shouldOpenLogStream)
                                            .commandUnitName(AwsLambdaCommandUnitConstants.fetchManifests.toString())
                                            .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.GIT_TASK_NG.name())
                                  .parameters(new Object[] {gitTaskNGRequest})
                                  .build();

    String taskName = TaskType.GIT_TASK_NG.getDisplayName();

    AwsLambdaSpecParameters awsLambdaSpecParameters = (AwsLambdaSpecParameters) stepElementParameters.getSpec();

    final TaskRequest taskRequest = TaskRequestsUtils.prepareCDTaskRequest(ambiance, taskData,
        referenceFalseKryoSerializer, awsLambdaSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(
            emptyIfNull(getParameterFieldValue(awsLambdaSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(awsLambdaStepPassThroughData)
        .build();
  }
}
