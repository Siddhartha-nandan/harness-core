/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.azure.webapp;

import static io.harness.azure.model.AzureConstants.DEPLOY_TO_SLOT;
import static io.harness.azure.model.AzureConstants.SAVE_EXISTING_CONFIGURATIONS;
import static io.harness.azure.model.AzureConstants.UPDATE_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.cdng.manifest.yaml.harness.HarnessStoreConstants.HARNESS_STORE_TYPE;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.APPLICATION_SETTINGS;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.CONNECTION_STRINGS;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.STARTUP_SCRIPT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.K8sCommandUnitConstants.FetchFiles;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.azure.webapp.beans.AzureSlotDeploymentDataOutput;
import io.harness.cdng.azure.webapp.beans.AzureSlotDeploymentPassThroughData;
import io.harness.cdng.azure.webapp.beans.AzureSlotDeploymentPassThroughData.AzureSlotDeploymentPassThroughDataBuilder;
import io.harness.cdng.azure.webapp.beans.AzureWebAppPreDeploymentDataOutput;
import io.harness.cdng.infra.beans.AzureWebAppInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AzureWebAppFetchPreDeploymentDataRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AzureWebAppSlotDeploymentRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppFetchPreDeploymentDataResponse;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppSlotDeploymentResponse;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppTaskResponse;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class AzureWebAppSlotDeploymentStep extends TaskChainExecutableWithRollbackAndRbac {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.AZURE_SLOT_DEPLOYMENT.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject private AzureWebAppStepHelper azureWebAppStepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {}

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    Map<String, StoreConfig> webAppConfig = azureWebAppStepHelper.fetchWebAppConfig(ambiance);
    InfrastructureOutcome infrastructure = cdStepHelper.getInfrastructureOutcome(ambiance);
    if (!(infrastructure instanceof AzureWebAppInfrastructureOutcome)) {
      throw new InvalidArgumentsException(Pair.of(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME,
          format("Invalid infrastructure outcome found. Expected: %s, found: %s", InfrastructureKind.AZURE_WEB_APP,
              infrastructure.getKind())));
    }

    AzureSlotDeploymentPassThroughDataBuilder passThroughDataBuilder =
        AzureSlotDeploymentPassThroughData.builder()
            .infrastructure((AzureWebAppInfrastructureOutcome) infrastructure)
            .configs(emptyMap())
            .commandUnitsProgress(CommandUnitsProgress.builder().build())
            .unprocessedConfigs(emptyMap());

    if (isNotEmpty(webAppConfig)) {
      return processAndFetchWebAppConfigs(
          stepParameters, ambiance, passThroughDataBuilder.unprocessedConfigs(webAppConfig).build());
    }

    return queueFetchPreDeploymentData(stepParameters, ambiance, passThroughDataBuilder.configs(emptyMap()).build());
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    ResponseData responseData = responseSupplier.get();
    AzureSlotDeploymentPassThroughData azureSlotDeploymentPassThroughData =
        (AzureSlotDeploymentPassThroughData) passThroughData;
    AzureSlotDeploymentPassThroughDataBuilder newPassThroughDataBuilder =
        azureSlotDeploymentPassThroughData.toBuilder();

    if (responseData instanceof GitFetchResponse) {
      GitFetchResponse gitFetchResponse = (GitFetchResponse) responseData;
      Map<String, String> resultConfigs =
          azureWebAppStepHelper.getConfigValuesFromGitFetchResponse(ambiance, gitFetchResponse);
      newPassThroughDataBuilder.configs(ImmutableMap.<String, String>builder()
                                            .putAll(azureSlotDeploymentPassThroughData.getConfigs())
                                            .putAll(resultConfigs)
                                            .build());
      newPassThroughDataBuilder.commandUnitsProgress(
          UnitProgressDataMapper.toCommandUnitsProgress(gitFetchResponse.getUnitProgressData()));
    } else if (responseData instanceof AzureWebAppTaskResponse) {
      AzureWebAppTaskResponse azureWebAppTaskResponse = (AzureWebAppTaskResponse) responseData;
      newPassThroughDataBuilder.commandUnitsProgress(
          UnitProgressDataMapper.toCommandUnitsProgress(azureWebAppTaskResponse.getCommandUnitsProgress()));
      if (azureWebAppTaskResponse.getRequestResponse() instanceof AzureWebAppFetchPreDeploymentDataResponse) {
        AzureWebAppFetchPreDeploymentDataResponse fetchPreDeploymentDataResponse =
            (AzureWebAppFetchPreDeploymentDataResponse) azureWebAppTaskResponse.getRequestResponse();
        newPassThroughDataBuilder.preDeploymentData(fetchPreDeploymentDataResponse.getPreDeploymentData());
        executionSweepingOutputService.consume(ambiance, AzureWebAppPreDeploymentDataOutput.OUTPUT_NAME,
            AzureWebAppPreDeploymentDataOutput.builder()
                .preDeploymentData(fetchPreDeploymentDataResponse.getPreDeploymentData())
                .build(),
            StepCategory.STEP.name());
      }
    }

    AzureSlotDeploymentPassThroughData newPassThroughData = newPassThroughDataBuilder.build();
    if (isNotEmpty(newPassThroughData.getUnprocessedConfigs())) {
      return processAndFetchWebAppConfigs(stepParameters, ambiance, newPassThroughDataBuilder.build());
    }

    if (newPassThroughData.getPreDeploymentData() == null) {
      return queueFetchPreDeploymentData(stepParameters, ambiance, newPassThroughDataBuilder.build());
    }

    return queueSlotDeploymentTask(stepParameters, ambiance, newPassThroughDataBuilder.build());
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    AzureWebAppTaskResponse webAppTaskResponse = (AzureWebAppTaskResponse) responseDataSupplier.get();
    AzureWebAppSlotDeploymentResponse slotDeploymentResponse =
        (AzureWebAppSlotDeploymentResponse) webAppTaskResponse.getRequestResponse();
    stepResponseBuilder.status(Status.SUCCEEDED);
    stepResponseBuilder.unitProgressList(webAppTaskResponse.getCommandUnitsProgress().getUnitProgresses());

    executionSweepingOutputService.consume(ambiance, AzureSlotDeploymentDataOutput.OUTPUT_NAME,
        AzureSlotDeploymentDataOutput.builder()
            .deploymentProgressMarker(slotDeploymentResponse.getDeploymentProgressMarker())
            .build(),
        StepCategory.STEP.name());

    return stepResponseBuilder.build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  private TaskChainResponse processAndFetchWebAppConfigs(StepElementParameters stepElementParameters, Ambiance ambiance,
      AzureSlotDeploymentPassThroughData passThroughData) {
    AzureSlotDeploymentPassThroughDataBuilder newPassThroughDataBuilder = passThroughData.toBuilder();
    Map<String, StoreConfig> unprocessedConfigs = passThroughData.getUnprocessedConfigs();
    Map<String, GitStoreConfig> gitStoreConfigs =
        AzureWebAppStepHelper.filterAndMapConfigs(unprocessedConfigs, ManifestStoreType::isInGitSubset);

    if (isNotEmpty(gitStoreConfigs)) {
      newPassThroughDataBuilder.unprocessedConfigs(
          AzureWebAppStepHelper.getConfigDifference(unprocessedConfigs, gitStoreConfigs));
      return TaskChainResponse.builder()
          .chainEnd(false)
          .passThroughData(newPassThroughDataBuilder.build())
          .taskRequest(
              azureWebAppStepHelper.prepareGitFetchTaskRequest(stepElementParameters, ambiance, gitStoreConfigs,
                  asList(FetchFiles, SAVE_EXISTING_CONFIGURATIONS, UPDATE_SLOT_CONFIGURATION_SETTINGS, DEPLOY_TO_SLOT)))
          .build();
    }

    Map<String, HarnessStore> harnessStoreConfigs =
        AzureWebAppStepHelper.filterAndMapConfigs(unprocessedConfigs, HARNESS_STORE_TYPE::equals);
    if (isNotEmpty(harnessStoreConfigs)) {
      Map<String, String> configs =
          azureWebAppStepHelper.fetchWebAppConfigsFromHarnessStore(ambiance, harnessStoreConfigs);
      newPassThroughDataBuilder.unprocessedConfigs(
          AzureWebAppStepHelper.getConfigDifference(unprocessedConfigs, harnessStoreConfigs));
      newPassThroughDataBuilder.configs(
          ImmutableMap.<String, String>builder().putAll(passThroughData.getConfigs()).putAll(configs).build());
    }

    if (passThroughData.getPreDeploymentData() == null) {
      return queueFetchPreDeploymentData(stepElementParameters, ambiance, newPassThroughDataBuilder.build());
    } else {
      return queueSlotDeploymentTask(stepElementParameters, ambiance, newPassThroughDataBuilder.build());
    }
  }

  private TaskChainResponse queueFetchPreDeploymentData(StepElementParameters stepElementParameters, Ambiance ambiance,
      AzureSlotDeploymentPassThroughData passThroughData) {
    if (isNotEmpty(passThroughData.getUnprocessedConfigs())) {
      String unprocessedConfigsRepr = passThroughData.getUnprocessedConfigs()
                                          .entrySet()
                                          .stream()
                                          .map(entry -> format("{%s: %s}", entry.getKey(), entry.getValue().getKind()))
                                          .collect(Collectors.joining(", "));
      log.warn("Unexpected unprocessed configuration: [{}]", unprocessedConfigsRepr);
    }

    AzureAppServiceConfiguration appServiceConfiguration =
        AzureAppServiceConfiguration.builder()
            .appSettingsJSON(passThroughData.getConfigs().get(APPLICATION_SETTINGS))
            .connStringsJSON(passThroughData.getConfigs().get(CONNECTION_STRINGS))
            .build();

    AzureWebAppFetchPreDeploymentDataRequest fetchPreDeploymentDataRequest =
        AzureWebAppFetchPreDeploymentDataRequest.builder()
            .infraDelegateConfig(
                azureWebAppStepHelper.getInfraDelegateConfig(ambiance, passThroughData.getInfrastructure()))
            .applicationSettings(appServiceConfiguration.getAppSettings())
            .connectionStrings(appServiceConfiguration.getConnStrings())
            .startupCommand(passThroughData.getConfigs().getOrDefault(STARTUP_SCRIPT, ""))
            .artifact(azureWebAppStepHelper.getPrimaryArtifactConfig(ambiance))
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepElementParameters))
            .commandUnitsProgress(passThroughData.getCommandUnitsProgress())
            .build();

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(azureWebAppStepHelper.prepareTaskRequest(stepElementParameters, ambiance,
            fetchPreDeploymentDataRequest, TaskType.AZURE_WEB_APP_TASK_NG,
            asList(SAVE_EXISTING_CONFIGURATIONS, UPDATE_SLOT_CONFIGURATION_SETTINGS, DEPLOY_TO_SLOT)))
        .passThroughData(passThroughData)
        .build();
  }

  private TaskChainResponse queueSlotDeploymentTask(StepElementParameters stepElementParameters, Ambiance ambiance,
      AzureSlotDeploymentPassThroughData passThroughData) {
    AzureAppServiceConfiguration appServiceConfiguration =
        AzureAppServiceConfiguration.builder()
            .appSettingsJSON(passThroughData.getConfigs().get(APPLICATION_SETTINGS))
            .connStringsJSON(passThroughData.getConfigs().get(CONNECTION_STRINGS))
            .build();

    AzureWebAppSlotDeploymentRequest slotDeploymentRequest =
        AzureWebAppSlotDeploymentRequest.builder()
            .preDeploymentData(passThroughData.getPreDeploymentData())
            .applicationSettings(appServiceConfiguration.getAppSettings())
            .connectionStrings(appServiceConfiguration.getConnStrings())
            .startupCommand(passThroughData.getConfigs().get(STARTUP_SCRIPT))
            .infrastructure(azureWebAppStepHelper.getInfraDelegateConfig(ambiance, passThroughData.getInfrastructure()))
            .artifact(azureWebAppStepHelper.getPrimaryArtifactConfig(ambiance))
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepElementParameters))
            .commandUnitsProgress(passThroughData.getCommandUnitsProgress())
            .build();

    return TaskChainResponse.builder()
        .chainEnd(true)
        .taskRequest(azureWebAppStepHelper.prepareTaskRequest(stepElementParameters, ambiance, slotDeploymentRequest,
            TaskType.AZURE_WEB_APP_TASK_NG, asList(UPDATE_SLOT_CONFIGURATION_SETTINGS, DEPLOY_TO_SLOT)))
        .passThroughData(passThroughData)
        .build();
  }
}
