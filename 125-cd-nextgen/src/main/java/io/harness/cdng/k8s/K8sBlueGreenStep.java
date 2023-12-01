/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.delegate.task.k8s.K8sBGDeployRequest.K8sBGDeployRequestBuilder;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.ReleaseMetadataFactory;
import io.harness.cdng.executables.CdTaskChainExecutable;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.helm.ReleaseHelmChartOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.k8s.K8sBlueGreenBaseStepInfo.K8sBlueGreenBaseStepInfoKeys;
import io.harness.cdng.k8s.beans.CustomFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.k8s.trafficrouting.K8sTrafficRoutingHelper;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.instancesync.mapper.K8sPodToServiceInstanceInfoMapper;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.k8s.model.K8sPod;
import io.harness.logging.CommandExecutionStatus;
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
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class K8sBlueGreenStep extends CdTaskChainExecutable implements K8sStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.K8S_BLUE_GREEN.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deploy";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private InstanceInfoService instanceInfoService;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private ReleaseMetadataFactory releaseMetadataFactory;
  @Inject private K8sTrafficRoutingHelper trafficRoutingHelper;

  @Override
  public Class<StepBaseParameters> getStepParametersClass() {
    return StepBaseParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {
    // Noop
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepBaseParameters stepElementParameters, StepInputPackage inputPackage) {
    return k8sStepHelper.startChainLink(this, ambiance, stepElementParameters);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContextAndNodeInfo(Ambiance ambiance,
      StepBaseParameters stepElementParameters, StepInputPackage inputPackage, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    return k8sStepHelper.executeNextLink(this, ambiance, stepElementParameters, passThroughData, responseSupplier);
  }

  @Override
  public TaskChainResponse executeK8sTask(ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepBaseParameters stepElementParameters, List<String> manifestOverrideContents,
      K8sExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream,
      UnitProgressData unitProgressData) {
    InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
    String releaseName = cdStepHelper.getReleaseName(ambiance, infrastructure);
    K8sBlueGreenStepParameters k8sBlueGreenStepParameters =
        (K8sBlueGreenStepParameters) stepElementParameters.getSpec();
    boolean skipDryRun = CDStepHelper.getParameterFieldBooleanValue(
        k8sBlueGreenStepParameters.getSkipDryRun(), K8sBlueGreenBaseStepInfoKeys.skipDryRun, stepElementParameters);
    boolean pruningEnabled = CDStepHelper.getParameterFieldBooleanValue(k8sBlueGreenStepParameters.getPruningEnabled(),
        K8sBlueGreenBaseStepInfoKeys.pruningEnabled, stepElementParameters);
    boolean skipUnchangedManifest =
        CDStepHelper.getParameterFieldBooleanValue(k8sBlueGreenStepParameters.getSkipUnchangedManifest(),
            K8sBlueGreenBaseStepInfoKeys.skipUnchangedManifest, stepElementParameters);
    List<String> manifestFilesContents =
        k8sStepHelper.renderValues(k8sManifestOutcome, ambiance, manifestOverrideContents);
    boolean isOpenshiftTemplate = ManifestType.OpenshiftTemplate.equals(k8sManifestOutcome.getType());

    final String accountId = AmbianceUtils.getAccountId(ambiance);

    K8sBGDeployRequestBuilder bgRequestBuilder =
        K8sBGDeployRequest.builder()
            .skipDryRun(skipDryRun)
            .releaseName(releaseName)
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .taskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(CDStepHelper.getTimeoutInMin(stepElementParameters))
            .valuesYamlList(!isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .openshiftParamList(isOpenshiftTemplate ? manifestFilesContents : Collections.emptyList())
            .kustomizePatchesList(k8sStepHelper.renderPatches(k8sManifestOutcome, ambiance, manifestOverrideContents))
            .k8sInfraDelegateConfig(cdStepHelper.getK8sInfraDelegateConfig(infrastructure, ambiance))
            .manifestDelegateConfig(
                k8sStepHelper.getManifestDelegateConfigWrapper(executionPassThroughData.getZippedManifestId(),
                    k8sManifestOutcome, ambiance, executionPassThroughData.getManifestFiles()))
            .accountId(accountId)
            .skipResourceVersioning(k8sStepHelper.getSkipResourceVersioning(k8sManifestOutcome))
            .shouldOpenFetchFilesLogStream(shouldOpenFetchFilesLogStream)
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .useLatestKustomizeVersion(cdStepHelper.isUseLatestKustomizeVersion(accountId))
            .useNewKubectlVersion(cdStepHelper.isUseNewKubectlVersion(accountId))
            .pruningEnabled(pruningEnabled)
            .useK8sApiForSteadyStateCheck(cdStepHelper.shouldUseK8sApiForSteadyStateCheck(accountId))
            .useDeclarativeRollback(k8sStepHelper.isDeclarativeRollbackEnabled(k8sManifestOutcome))
            .enabledSupportHPAAndPDB(cdStepHelper.isEnabledSupportHPAAndPDB(accountId))
            .skipUnchangedManifest(cdStepHelper.isSkipUnchangedManifest(accountId, skipUnchangedManifest))
            .storeReleaseHash(cdStepHelper.isStoreReleaseHash(accountId));

    if (cdFeatureFlagHelper.isEnabled(accountId, FeatureName.CDS_K8S_SERVICE_HOOKS_NG)) {
      bgRequestBuilder.serviceHooks(k8sStepHelper.getServiceHooks(ambiance));
    }
    if (cdStepHelper.shouldPassReleaseMetadata(accountId)) {
      bgRequestBuilder.releaseMetadata(releaseMetadataFactory.createReleaseMetadata(infrastructure, ambiance));
    }
    if (cdFeatureFlagHelper.isEnabled(accountId, FeatureName.CDS_K8S_TRAFFIC_ROUTING_NG)) {
      bgRequestBuilder.trafficRoutingConfig(
          trafficRoutingHelper.validateAndGetTrafficRoutingConfig(k8sBlueGreenStepParameters.getTrafficRouting())
              .orElse(null));
    }
    Map<String, String> k8sCommandFlag =
        k8sStepHelper.getDelegateK8sCommandFlag(k8sBlueGreenStepParameters.getCommandFlags(), ambiance);
    bgRequestBuilder.k8sCommandFlags(k8sCommandFlag);
    K8sBGDeployRequest k8sBGDeployRequest = bgRequestBuilder.build();
    k8sStepHelper.publishReleaseNameStepDetails(ambiance, releaseName);

    return k8sStepHelper.queueK8sTask(stepElementParameters, k8sBGDeployRequest, ambiance, executionPassThroughData);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContextAndNodeInfo(Ambiance ambiance,
      StepBaseParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof CustomFetchResponsePassThroughData) {
      return k8sStepHelper.handleCustomTaskFailure((CustomFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof GitFetchResponsePassThroughData) {
      return cdStepHelper.handleGitTaskFailure((GitFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof HelmValuesFetchResponsePassThroughData) {
      return k8sStepHelper.handleHelmValuesFetchFailure((HelmValuesFetchResponsePassThroughData) passThroughData);
    }

    if (passThroughData instanceof StepExceptionPassThroughData) {
      return cdStepHelper.handleStepExceptionFailure((StepExceptionPassThroughData) passThroughData);
    }

    K8sExecutionPassThroughData executionPassThroughData = (K8sExecutionPassThroughData) passThroughData;

    K8sDeployResponse k8sTaskExecutionResponse;
    try {
      k8sTaskExecutionResponse = (K8sDeployResponse) responseDataSupplier.get();
    } catch (Exception e) {
      log.error("Error while processing K8s Task response: {}", e.getMessage(), e);
      return k8sStepHelper.handleTaskException(ambiance, executionPassThroughData, e);
    }

    StepResponseBuilder responseBuilder =
        StepResponse.builder().unitProgressList(k8sTaskExecutionResponse.getCommandUnitsProgress().getUnitProgresses());

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return K8sStepHelper.getFailureResponseBuilder(k8sTaskExecutionResponse, responseBuilder).build();
    }

    InfrastructureOutcome infrastructure = executionPassThroughData.getInfrastructure();
    K8sBGDeployResponse k8sBGDeployResponse = (K8sBGDeployResponse) k8sTaskExecutionResponse.getK8sNGTaskResponse();
    K8sBlueGreenStepParameters k8sBlueGreenStepParameters =
        (K8sBlueGreenStepParameters) stepElementParameters.getSpec();
    boolean pruningEnabled = CDStepHelper.getParameterFieldBooleanValue(k8sBlueGreenStepParameters.getPruningEnabled(),
        K8sBlueGreenBaseStepInfoKeys.pruningEnabled, stepElementParameters);
    if (BooleanUtils.isTrue(k8sBGDeployResponse.getStageDeploymentSkipped())) {
      K8sBlueGreenOutcome k8sBlueGreenOutcome = K8sBlueGreenOutcome.builder().stageDeploymentSkipped(true).build();
      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME,
          k8sBlueGreenOutcome, StepOutcomeGroup.STEP.name());
      return responseBuilder.status(Status.SUCCEEDED)
          .stepOutcome(
              StepOutcome.builder().name(OutcomeExpressionConstants.OUTPUT).outcome(k8sBlueGreenOutcome).build())
          .build();
    }
    K8sBlueGreenOutcome k8sBlueGreenOutcome =
        K8sBlueGreenOutcome.builder()
            .releaseName(cdStepHelper.getReleaseName(ambiance, infrastructure))
            .releaseNumber(k8sBGDeployResponse.getReleaseNumber())
            .primaryServiceName(k8sBGDeployResponse.getPrimaryServiceName())
            .stageServiceName(k8sBGDeployResponse.getStageServiceName())
            .stageColor(k8sBGDeployResponse.getStageColor())
            .primaryColor(k8sBGDeployResponse.getPrimaryColor())
            .prunedResourceIds(
                k8sStepHelper.getPrunedResourcesIds(pruningEnabled, k8sBGDeployResponse.getPrunedResourceIds()))
            .manifest(executionPassThroughData.getK8sGitFetchInfo())
            .podIps(k8sBGDeployResponse.getK8sPodList() != null
                    ? k8sBGDeployResponse.getK8sPodList().stream().map(K8sPod::getPodIP).collect(Collectors.toList())
                    : null)
            .build();
    executionSweepingOutputService.consume(
        ambiance, OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME, k8sBlueGreenOutcome, StepOutcomeGroup.STEP.name());
    HelmChartInfo helmChartInfo = k8sBGDeployResponse.getHelmChartInfo();
    ReleaseHelmChartOutcome releaseHelmChartOutcome = k8sStepHelper.getHelmChartOutcome(helmChartInfo);
    StepOutcome stepOutcome = instanceInfoService.saveServerInstancesIntoSweepingOutput(ambiance,
        K8sPodToServiceInstanceInfoMapper.toServerInstanceInfoList(k8sBGDeployResponse.getK8sPodList(), helmChartInfo));

    return responseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder().name(OutcomeExpressionConstants.OUTPUT).outcome(k8sBlueGreenOutcome).build())
        .stepOutcome(stepOutcome)
        .stepOutcome(StepOutcome.builder()
                         .name(OutcomeExpressionConstants.RELEASE_HELM_CHART_OUTCOME)
                         .outcome(releaseHelmChartOutcome)
                         .build())
        .build();
  }
}
