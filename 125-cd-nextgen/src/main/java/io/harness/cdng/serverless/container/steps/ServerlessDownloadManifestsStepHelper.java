/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless.container.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.beans.steps.nodes.GitCloneStepNode;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.cdng.aws.sam.DownloadManifestsCommonHelper;
import io.harness.cdng.containerStepGroup.DownloadAwsS3Step;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStep;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepInfo;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepNode;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.cdng.plugininfoproviders.DownloadAwsS3PluginInfoProvider;
import io.harness.cdng.plugininfoproviders.DownloadHarnessStorePluginInfoProvider;
import io.harness.cdng.plugininfoproviders.GitClonePluginInfoProvider;
import io.harness.cdng.plugininfoproviders.ServerlessV2PluginInfoProviderHelper;
import io.harness.cdng.serverless.beans.ServerlessV2ValuesYamlDataOutcome;
import io.harness.cdng.serverless.beans.ServerlessV2ValuesYamlDataOutcome.ServerlessV2ValuesYamlDataOutcomeBuilder;
import io.harness.cdng.serverless.container.steps.outcome.ServerlessV2DirectoryPathsOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepOutput;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plugin.GitCloneStep;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponseList;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.plugin.ContainerPluginParseException;
import io.harness.pms.sdk.core.plugin.ContainerStepExecutionResponseHelper;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.YamlUtils;
import io.harness.tasks.ResponseData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_SERVERLESS})
@Slf4j
public class ServerlessDownloadManifestsStepHelper {
  @Inject private OutcomeService outcomeService;

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Inject private GitClonePluginInfoProvider gitClonePluginInfoProvider;

  @Inject private EngineExpressionService engineExpressionService;

  @Inject private DownloadAwsS3PluginInfoProvider downloadAwsS3PluginInfoProvider;

  @Inject private DownloadHarnessStorePluginInfoProvider downloadHarnessStorePluginInfoProvider;
  @Inject private DownloadAwsS3Step downloadAwsS3Step;

  @Inject private GitCloneStep gitCloneStep;

  @Inject private DownloadHarnessStoreStep downloadHarnessStoreStep;

  @Inject private ServerlessV2PluginInfoProviderHelper serverlessV2PluginInfoProviderHelper;

  @Inject private ContainerStepExecutionResponseHelper containerStepExecutionResponseHelper;

  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  @Inject DownloadManifestsCommonHelper downloadManifestsCommonHelper;

  public AsyncExecutableResponse executeAsyncAfterRbac(Ambiance ambiance, StepInputPackage inputPackage) {
    ManifestsOutcome manifestsOutcome = serverlessV2PluginInfoProviderHelper.fetchManifestsOutcome(ambiance);

    AsyncExecutableResponse samDirectoryAsyncExecutableResponse =
        getAsyncExecutableResponseForServerlessAwsLambdaManifest(ambiance, inputPackage, manifestsOutcome);

    List<String> callbackIds = new ArrayList<>(samDirectoryAsyncExecutableResponse.getCallbackIdsList());
    List<String> logKeys = new ArrayList<>(samDirectoryAsyncExecutableResponse.getLogKeysList());

    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaValuesManifestOutcome(
            manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      AsyncExecutableResponse valuesAsyncExecutableResponse =
          getAsyncExecutableResponseForValuesManifest(ambiance, inputPackage, valuesManifestOutcome);
      callbackIds.addAll(valuesAsyncExecutableResponse.getCallbackIdsList());
      logKeys.addAll(valuesAsyncExecutableResponse.getLogKeysList());
    }

    return AsyncExecutableResponse.newBuilder()
        .addAllCallbackIds(callbackIds)
        .setStatus(samDirectoryAsyncExecutableResponse.getStatus())
        .addAllLogKeys(logKeys)
        .build();
  }

  public AsyncExecutableResponse getAsyncExecutableResponseForServerlessAwsLambdaManifest(
      Ambiance ambiance, StepInputPackage inputPackage, ManifestsOutcome manifestsOutcome) {
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaDirectoryManifestOutcome =
        (ServerlessAwsLambdaManifestOutcome) serverlessV2PluginInfoProviderHelper
            .getServerlessAwsLambdaDirectoryManifestOutcome(manifestsOutcome.values());

    if (serverlessAwsLambdaDirectoryManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) serverlessAwsLambdaDirectoryManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfo(serverlessAwsLambdaDirectoryManifestOutcome, s3StoreConfig);

      StepElementParameters stepElementParameters = downloadManifestsCommonHelper.getDownloadS3StepElementParameters(
          serverlessAwsLambdaDirectoryManifestOutcome, downloadAwsS3StepInfo);
      Ambiance ambianceForServerlessAwsLambdaManifest = downloadManifestsCommonHelper.buildAmbiance(ambiance,
          downloadManifestsCommonHelper.getDownloadS3StepIdentifier(serverlessAwsLambdaDirectoryManifestOutcome));
      return downloadAwsS3Step.executeAsyncAfterRbac(
          ambianceForServerlessAwsLambdaManifest, stepElementParameters, inputPackage);
    } else if (serverlessAwsLambdaDirectoryManifestOutcome.getStore() instanceof HarnessStore) {
      checkForS3DownloadFeatureFlag(ambiance);

      HarnessStore harnessStore = (HarnessStore) serverlessAwsLambdaDirectoryManifestOutcome.getStore();
      DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfo(
              serverlessAwsLambdaDirectoryManifestOutcome, harnessStore);

      StepElementParameters stepElementParameters =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepElementParameters(
              serverlessAwsLambdaDirectoryManifestOutcome, downloadHarnessStoreStepInfo);
      Ambiance ambianceForServerlessAwsLambdaManifest = downloadManifestsCommonHelper.buildAmbiance(ambiance,
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepIdentifier(
              serverlessAwsLambdaDirectoryManifestOutcome));
      return downloadHarnessStoreStep.executeAsyncAfterRbac(
          ambianceForServerlessAwsLambdaManifest, stepElementParameters, inputPackage);
    } else if (serverlessAwsLambdaDirectoryManifestOutcome.getStore() instanceof GitStoreConfig) {
      GitCloneStepInfo gitCloneStepInfo = downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(
          serverlessAwsLambdaDirectoryManifestOutcome);

      StepElementParameters stepElementParameters = downloadManifestsCommonHelper.getGitStepElementParameters(
          serverlessAwsLambdaDirectoryManifestOutcome, gitCloneStepInfo);

      Ambiance ambianceForServerlessAwsLambdaManifest = downloadManifestsCommonHelper.buildAmbiance(ambiance,
          downloadManifestsCommonHelper.getGitCloneStepIdentifier(serverlessAwsLambdaDirectoryManifestOutcome));
      return gitCloneStep.executeAsyncAfterRbac(
          ambianceForServerlessAwsLambdaManifest, stepElementParameters, inputPackage);
    } else {
      throw new InvalidRequestException(
          format("%s store type not supported for Serverless Aws Lambda Directory Manifest",
              serverlessAwsLambdaDirectoryManifestOutcome.getStore().getKind()),
          USER);
    }
  }

  private void checkForS3DownloadFeatureFlag(Ambiance ambiance) {
    if (!cdFeatureFlagHelper.isEnabled(
            AmbianceUtils.getAccountId(ambiance), FeatureName.CDS_CONTAINER_STEP_GROUP_AWS_S3_DOWNLOAD)) {
      throw new AccessDeniedException(
          "CDS_CONTAINER_STEP_GROUP_AWS_S3_DOWNLOAD FF is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  public AsyncExecutableResponse getAsyncExecutableResponseForValuesManifest(
      Ambiance ambiance, StepInputPackage inputPackage, ValuesManifestOutcome valuesManifestOutcome) {
    if (valuesManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) valuesManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfoWithOutputFilePathContents(valuesManifestOutcome, s3StoreConfig,
              serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      StepElementParameters stepElementParameters = downloadManifestsCommonHelper.getDownloadS3StepElementParameters(
          valuesManifestOutcome, downloadAwsS3StepInfo);
      Ambiance ambianceForValuesManifest = downloadManifestsCommonHelper.buildAmbiance(
          ambiance, downloadManifestsCommonHelper.getDownloadS3StepIdentifier(valuesManifestOutcome));
      return downloadAwsS3Step.executeAsyncAfterRbac(ambianceForValuesManifest, stepElementParameters, inputPackage);
    } else if (valuesManifestOutcome.getStore() instanceof HarnessStore) {
      checkForS3DownloadFeatureFlag(ambiance);

      HarnessStore harnessStore = (HarnessStore) valuesManifestOutcome.getStore();
      DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfoWithOutputFilePathContents(valuesManifestOutcome,
              harnessStore,
              serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      StepElementParameters stepElementParameters =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepElementParameters(
              valuesManifestOutcome, downloadHarnessStoreStepInfo);
      Ambiance ambianceForValuesManifest = downloadManifestsCommonHelper.buildAmbiance(
          ambiance, downloadManifestsCommonHelper.getDownloadHarnessStoreStepIdentifier(valuesManifestOutcome));
      return downloadHarnessStoreStep.executeAsyncAfterRbac(
          ambianceForValuesManifest, stepElementParameters, inputPackage);
    } else if (valuesManifestOutcome.getStore() instanceof GitStoreConfig) {
      GitCloneStepInfo valuesGitCloneStepInfo =
          downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcomeWithOutputFilePathContents(
              valuesManifestOutcome,
              Collections.singletonList(
                  serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome)));

      StepElementParameters valuesStepElementParameters =
          downloadManifestsCommonHelper.getGitStepElementParameters(valuesManifestOutcome, valuesGitCloneStepInfo);

      Ambiance ambianceForValuesManifest = downloadManifestsCommonHelper.buildAmbiance(
          ambiance, downloadManifestsCommonHelper.getGitCloneStepIdentifier(valuesManifestOutcome));

      return gitCloneStep.executeAsyncAfterRbac(ambianceForValuesManifest, valuesStepElementParameters, inputPackage);
    } else {
      throw new InvalidRequestException(
          format("%s store type not supported for Values Manifest", valuesManifestOutcome.getStore().getKind()), USER);
    }
  }

  public StepResponse handleAsyncResponse(Ambiance ambiance, Map<String, ResponseData> responseDataMap) {
    ManifestsOutcome manifestsOutcome = serverlessV2PluginInfoProviderHelper.fetchManifestsOutcome(ambiance);

    handleResponseForValuesManifest(ambiance, responseDataMap, manifestsOutcome);

    StepResponse.StepOutcome stepOutcome = handleResponseForServerlessAwsLambdaManifest(manifestsOutcome);

    return StepResponse.builder().stepOutcome(stepOutcome).status(Status.SUCCEEDED).build();
  }

  public StepOutcome handleResponseForServerlessAwsLambdaManifest(ManifestsOutcome manifestsOutcome) {
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaDirectoryManifestOutcome =
        (ServerlessAwsLambdaManifestOutcome) serverlessV2PluginInfoProviderHelper
            .getServerlessAwsLambdaDirectoryManifestOutcome(manifestsOutcome.values());
    String serverlessDirectoryPath =
        serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaDirectoryPathFromManifestOutcome(
            serverlessAwsLambdaDirectoryManifestOutcome);
    ServerlessV2DirectoryPathsOutcome serverlessV2DirectoryPathsOutcome =
        ServerlessV2DirectoryPathsOutcome.builder().serverlessDirectoryPath(serverlessDirectoryPath).build();

    return StepOutcome.builder()
        .outcome(serverlessV2DirectoryPathsOutcome)
        .name(OutcomeExpressionConstants.SERVERLESS_V2_DIRECTORY_PATH_OUTCOME)
        .group(StepCategory.STEP_GROUP.name())
        .build();
  }

  public void handleResponseForValuesManifest(
      Ambiance ambiance, Map<String, ResponseData> responseDataMap, ManifestsOutcome manifestsOutcome) {
    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaValuesManifestOutcome(
            manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      ServerlessV2ValuesYamlDataOutcomeBuilder serverlessValuesYamlDataOutcomeBuilder =
          ServerlessV2ValuesYamlDataOutcome.builder();

      containerStepExecutionResponseHelper.deserializeResponse(responseDataMap);

      for (Map.Entry<String, ResponseData> entry : responseDataMap.entrySet()) {
        ResponseData responseData = entry.getValue();
        if (responseData instanceof StepStatusTaskResponseData) {
          StepStatusTaskResponseData stepStatusTaskResponseData = (StepStatusTaskResponseData) responseData;
          if (StepExecutionStatus.SUCCESS == stepStatusTaskResponseData.getStepStatus().getStepExecutionStatus()) {
            StepOutput stepOutput = stepStatusTaskResponseData.getStepStatus().getOutput();

            renderValuesManifestAndSaveToSweepingOutput(
                ambiance, valuesManifestOutcome, serverlessValuesYamlDataOutcomeBuilder, stepOutput);
          }
        }
      }
    }
  }

  public void renderValuesManifestAndSaveToSweepingOutput(Ambiance ambiance,
      ValuesManifestOutcome valuesManifestOutcome,
      ServerlessV2ValuesYamlDataOutcomeBuilder serverlessValuesYamlDataOutcomeBuilder, StepOutput stepOutput) {
    if (stepOutput instanceof StepMapOutput) {
      StepMapOutput stepMapOutput = (StepMapOutput) stepOutput;
      if (EmptyPredicate.isNotEmpty(stepMapOutput.getMap())) {
        String valuesYamlPath =
            serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome);
        String valuesYamlContent = getValuesManifestContent(stepMapOutput, valuesYamlPath);

        if (!isEmpty(valuesYamlPath) && !isEmpty(valuesYamlContent)) {
          valuesYamlContent = engineExpressionService.renderExpression(ambiance, valuesYamlContent);
          serverlessValuesYamlDataOutcomeBuilder.valuesYamlPath(valuesYamlPath);
          serverlessValuesYamlDataOutcomeBuilder.valuesYamlContent(valuesYamlContent);
          executionSweepingOutputService.consume(ambiance,
              OutcomeExpressionConstants.SERVERLESS_VALUES_YAML_DATA_OUTCOME,
              serverlessValuesYamlDataOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
        }
      }
    }
  }

  @VisibleForTesting
  String getValuesManifestContent(StepMapOutput stepMapOutput, String valuesYamlPath) {
    String valuesYamlContentBase64 = stepMapOutput.getMap().get(valuesYamlPath);
    if (isEmpty(valuesYamlContentBase64)) {
      return EMPTY;
    }

    // fixing yaml base64 content because github.com/joho/godotenv.Read() can't parse == while fetching env variables
    String fixedValuesYamlContentBase64 = valuesYamlContentBase64.replace("-", "=").replace(SPACE, EMPTY);
    try {
      return new String(Base64.getDecoder().decode(fixedValuesYamlContentBase64));
    } catch (Exception ex) {
      throw new InvalidRequestException(format("Unable to fetch values YAML, valuesYamlPath: %s", valuesYamlPath), ex);
    }
  }

  public PluginCreationResponseList getPluginInfoList(
      PluginCreationRequest request, Set<Integer> usedPorts, Ambiance ambiance) {
    String stepJsonNode = request.getStepJsonNode();
    CdAbstractStepNode cdAbstractStepNode;
    try {
      cdAbstractStepNode = getCdAbstractStepNode(stepJsonNode);
    } catch (IOException e) {
      throw new ContainerPluginParseException(
          format("Error in parsing CD step for step type [%s]", request.getType()), e);
    }

    List<PluginCreationResponseWrapper> pluginCreationResponseWrapperList = new ArrayList<>();

    ManifestsOutcome manifestsOutcome = serverlessV2PluginInfoProviderHelper.fetchManifestsOutcome(ambiance);

    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        getPluginCreationResponseWrapperForServerlessAwsLambdaManifest(
            request, usedPorts, ambiance, cdAbstractStepNode, manifestsOutcome);

    usedPorts.addAll(pluginCreationResponseWrapper.getResponse().getPluginDetails().getPortUsedList());

    pluginCreationResponseWrapperList.add(pluginCreationResponseWrapper);

    // Values Yaml

    ValuesManifestOutcome valuesManifestOutcome =
        (ValuesManifestOutcome) serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaValuesManifestOutcome(
            manifestsOutcome.values());

    if (valuesManifestOutcome != null) {
      PluginCreationResponseWrapper valuesPluginCreationResponseWrapper =
          getPluginCreationResponseWrapperForValuesManifest(
              request, usedPorts, ambiance, cdAbstractStepNode, valuesManifestOutcome);
      pluginCreationResponseWrapperList.add(valuesPluginCreationResponseWrapper);
    }

    return PluginCreationResponseList.newBuilder().addAllResponse(pluginCreationResponseWrapperList).build();
  }

  public PluginCreationResponseWrapper getPluginCreationResponseWrapperForValuesManifest(PluginCreationRequest request,
      Set<Integer> usedPorts, Ambiance ambiance, CdAbstractStepNode cdAbstractStepNode,
      ValuesManifestOutcome valuesManifestOutcome) {
    if (valuesManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) valuesManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfoWithOutputFilePathContents(valuesManifestOutcome, s3StoreConfig,
              serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      DownloadAwsS3StepNode downloadAwsS3StepNode = downloadManifestsCommonHelper.getAwsS3StepNode(
          cdAbstractStepNode, valuesManifestOutcome, downloadAwsS3StepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(getStepJsonNode(downloadAwsS3StepNode)).build();
      return downloadAwsS3PluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    } else if (valuesManifestOutcome.getStore() instanceof HarnessStore) {
      checkForS3DownloadFeatureFlag(ambiance);

      HarnessStore harnessStore = (HarnessStore) valuesManifestOutcome.getStore();
      DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfoWithOutputFilePathContents(valuesManifestOutcome,
              harnessStore,
              serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome));

      DownloadHarnessStoreStepNode downloadHarnessStoreStepNode =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepNode(
              cdAbstractStepNode, valuesManifestOutcome, downloadHarnessStoreStepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(getStepJsonNode(downloadHarnessStoreStepNode)).build();
      return downloadHarnessStorePluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    } else if (valuesManifestOutcome.getStore() instanceof GitStoreConfig) {
      GitCloneStepInfo valuesGitCloneStepInfo =
          downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcomeWithOutputFilePathContents(
              valuesManifestOutcome,
              Collections.singletonList(
                  serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome)));

      GitCloneStepNode valuesGitCloneStepNode = downloadManifestsCommonHelper.getGitCloneStepNode(
          valuesManifestOutcome, valuesGitCloneStepInfo, cdAbstractStepNode);

      PluginCreationRequest valuesPluginCreationRequest =
          request.toBuilder().setStepJsonNode(YamlUtils.writeYamlString(valuesGitCloneStepNode)).build();

      return gitClonePluginInfoProvider.getPluginInfo(valuesPluginCreationRequest, usedPorts, ambiance);
    } else {
      throw new InvalidRequestException(
          format("%s store type not supported for Values Manifest", valuesManifestOutcome.getStore().getKind()), USER);
    }
  }

  public PluginCreationResponseWrapper getPluginCreationResponseWrapperForServerlessAwsLambdaManifest(
      PluginCreationRequest request, Set<Integer> usedPorts, Ambiance ambiance, CdAbstractStepNode cdAbstractStepNode,
      ManifestsOutcome manifestsOutcome) {
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
        (ServerlessAwsLambdaManifestOutcome) serverlessV2PluginInfoProviderHelper
            .getServerlessAwsLambdaDirectoryManifestOutcome(manifestsOutcome.values());

    if (serverlessAwsLambdaManifestOutcome.getStore() instanceof S3StoreConfig) {
      checkForS3DownloadFeatureFlag(ambiance);

      S3StoreConfig s3StoreConfig = (S3StoreConfig) serverlessAwsLambdaManifestOutcome.getStore();
      DownloadAwsS3StepInfo downloadAwsS3StepInfo =
          downloadManifestsCommonHelper.getAwsS3StepInfo(serverlessAwsLambdaManifestOutcome, s3StoreConfig);

      DownloadAwsS3StepNode downloadAwsS3StepNode = downloadManifestsCommonHelper.getAwsS3StepNode(
          cdAbstractStepNode, serverlessAwsLambdaManifestOutcome, downloadAwsS3StepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(getStepJsonNode(downloadAwsS3StepNode)).build();
      return downloadAwsS3PluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    } else if (serverlessAwsLambdaManifestOutcome.getStore() instanceof HarnessStore) {
      checkForS3DownloadFeatureFlag(ambiance);

      HarnessStore harnessStore = (HarnessStore) serverlessAwsLambdaManifestOutcome.getStore();
      DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfo(
              serverlessAwsLambdaManifestOutcome, harnessStore);

      DownloadHarnessStoreStepNode downloadHarnessStoreStepNode =
          downloadManifestsCommonHelper.getDownloadHarnessStoreStepNode(
              cdAbstractStepNode, serverlessAwsLambdaManifestOutcome, downloadHarnessStoreStepInfo);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(getStepJsonNode(downloadHarnessStoreStepNode)).build();
      return downloadHarnessStorePluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    } else if (serverlessAwsLambdaManifestOutcome.getStore() instanceof GitStoreConfig) {
      GitCloneStepInfo gitCloneStepInfo =
          downloadManifestsCommonHelper.getGitCloneStepInfoFromManifestOutcome(serverlessAwsLambdaManifestOutcome);

      GitCloneStepNode gitCloneStepNode = downloadManifestsCommonHelper.getGitCloneStepNode(
          serverlessAwsLambdaManifestOutcome, gitCloneStepInfo, cdAbstractStepNode);

      PluginCreationRequest pluginCreationRequest =
          request.toBuilder().setStepJsonNode(getStepJsonNode(gitCloneStepNode)).build();

      return gitClonePluginInfoProvider.getPluginInfo(pluginCreationRequest, usedPorts, ambiance);
    } else {
      throw new InvalidRequestException(
          format("%s store type not supported for Serverless Aws Lambda Directory Manifest",
              serverlessAwsLambdaManifestOutcome.getStore().getKind()),
          USER);
    }
  }

  public RefObject getOutcomeRefObject() {
    return RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS);
  }

  public CdAbstractStepNode getCdAbstractStepNode(String stepJsonNode) throws IOException {
    return YamlUtils.read(stepJsonNode, CdAbstractStepNode.class);
  }

  public String getStepJsonNode(AbstractStepNode abstractStepNode) {
    return YamlUtils.writeYamlString(abstractStepNode);
  }
}
