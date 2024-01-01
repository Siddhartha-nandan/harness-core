/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.K8sCommandUnitConstants.Apply;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.TrafficRouting;
import static io.harness.k8s.exception.KubernetesExceptionMessages.TRAFFIC_ROUTING_FAILED;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.trafficrouting.TrafficRoutingResourceCreator;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.K8sTrafficRoutingRequest;
import io.harness.delegate.task.k8s.K8sTrafficRoutingResponse;
import io.harness.delegate.task.k8s.K8sTrafficRoutingResponse.K8sTrafficRoutingResponseBuilder;
import io.harness.delegate.task.k8s.client.K8sApiClient;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfigType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.helpers.k8s.releasehistory.K8sReleaseHandler;
import io.harness.k8s.K8sApiVersion;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.KubectlFactory;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.releasehistory.IK8sRelease;
import io.harness.k8s.releasehistory.IK8sReleaseHistory;
import io.harness.k8s.trafficrouting.TrafficRoutingInfoDTO;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(CDP)
@NoArgsConstructor
@Slf4j
public class K8sTrafficRoutingRequestHandler extends K8sRequestHandler {
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private K8sApiClient kubernetesApiClient;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private Map<String, TrafficRoutingResourceCreator> k8sTrafficRoutingCreators;
  private KubernetesConfig kubernetesConfig;
  private String releaseName;
  private Kubectl client;
  private IK8sReleaseHistory releaseHistory;
  private IK8sRelease latestRelease;
  private K8sReleaseHandler releaseHandler;
  private boolean useDeclarativeRollback;
  private List<KubernetesResource> resources;
  private K8sTrafficRoutingConfigType k8sTrafficRoutingConfigType;
  private TrafficRoutingInfoDTO trafficRoutingInfo;

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sTrafficRoutingRequest)) {
      throw new InvalidArgumentsException(Pair.of("k8sDeployRequest", "Must be instance of K8sTrafficRoutingRequest"));
    }
    K8sTrafficRoutingRequest k8sTrafficRoutingRequest = (K8sTrafficRoutingRequest) k8sDeployRequest;
    kubernetesConfig = k8sDelegateTaskParams.getKubernetesConfig();
    releaseName = k8sTrafficRoutingRequest.getReleaseName();
    useDeclarativeRollback = k8sTrafficRoutingRequest.isUseDeclarativeRollback();
    releaseHandler = k8sTaskHelperBase.getReleaseHandler(useDeclarativeRollback);
    resources = new ArrayList<>();

    if (k8sTrafficRoutingRequest.getTrafficRoutingConfig() != null) {
      k8sTrafficRoutingConfigType = k8sTrafficRoutingRequest.getTrafficRoutingConfig().getType();
      trafficRoutingInfo = k8sTrafficRoutingRequest.getTrafficRoutingInfo();
    }

    init(k8sTrafficRoutingRequest, k8sDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);

    K8sTrafficRoutingResponseBuilder k8sTrafficRoutingResponseBuilder = K8sTrafficRoutingResponse.builder();
    if (K8sTrafficRoutingConfigType.CONFIG.equals(k8sTrafficRoutingConfigType)) {
      Optional<TrafficRoutingInfoDTO> optionalTrafficRoutingInfoDTO = prepareForTrafficRouting(
          k8sTrafficRoutingRequest, k8sDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);

      if (optionalTrafficRoutingInfoDTO.isPresent()) {
        k8sTrafficRoutingResponseBuilder.info(optionalTrafficRoutingInfoDTO.get());
      }

      applyTrafficRouting(k8sDelegateTaskParams, logStreamingTaskClient, commandUnitsProgress);
    } else if (K8sTrafficRoutingConfigType.INHERIT.equals(k8sTrafficRoutingConfigType)) {
      if (trafficRoutingInfo == null) {
        if (latestRelease == null || latestRelease.getTrafficRoutingInfo() == null) {
          throw new InvalidArgumentsException("There is not a valid info on which to do traffic routing");
        }
        trafficRoutingInfo = latestRelease.getTrafficRoutingInfo();
      }

      Optional<String> patch = prepareTrafficRoutingPatch(k8sTrafficRoutingRequest.getK8sInfraDelegateConfig(),
          k8sTrafficRoutingRequest.getTrafficRoutingConfig(), trafficRoutingInfo, logStreamingTaskClient,
          commandUnitsProgress);

      patchTrafficRoutingResource(patch, trafficRoutingInfo, logStreamingTaskClient, commandUnitsProgress);
    } else {
      throw new UnsupportedOperationException(
          format("Traffic Routing task option [%s] is not yet supported", k8sTrafficRoutingConfigType));
    }

    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(k8sTrafficRoutingResponseBuilder.build())
        .build();
  }

  @Override
  public boolean isErrorFrameworkSupported() {
    return true;
  }

  @Override
  protected void handleTaskFailure(K8sDeployRequest request, Exception exception) throws Exception {}

  void init(K8sTrafficRoutingRequest k8sTrafficRoutingRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    LogCallback logCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, commandUnitsProgress);

    logCallback.saveExecutionLog("Initializing..\n");
    logCallback.saveExecutionLog(color(format("Release Name: [%s]", releaseName), Yellow, Bold));

    if (kubernetesConfig == null) {
      log.warn("Kubernetes config passed to task is NULL. Creating it again...");
      kubernetesConfig = containerDeploymentDelegateBaseHelper.createKubernetesConfig(
          k8sTrafficRoutingRequest.getK8sInfraDelegateConfig(), k8sDelegateTaskParams.getWorkingDirectory(),
          logCallback);
    }
    client = KubectlFactory.getKubectlClient(k8sDelegateTaskParams.getKubectlPath(),
        k8sDelegateTaskParams.getKubeconfigPath(), k8sDelegateTaskParams.getWorkingDirectory());
    releaseHistory = releaseHandler.getReleaseHistory(kubernetesConfig, releaseName);
    if (!releaseHistory.isEmpty()) {
      latestRelease = releaseHistory.getLatestRelease();
    }

    logCallback.saveExecutionLog("Done.", INFO, SUCCESS);
  }

  Optional<TrafficRoutingInfoDTO> prepareForTrafficRouting(K8sTrafficRoutingRequest k8sTrafficRoutingRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) {
    K8sTrafficRoutingConfig trafficRoutingConfig = k8sTrafficRoutingRequest.getTrafficRoutingConfig();
    LogCallback logCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, TrafficRouting, true, commandUnitsProgress);

    logCallback.saveExecutionLog(format("%s%n", "Preparing Traffic Routing resources based on configuration."));

    if (trafficRoutingConfig != null) {
      Set<String> availableApiVersions =
          kubernetesApiClient.getApiVersions(k8sTrafficRoutingRequest.getK8sInfraDelegateConfig(),
              k8sDelegateTaskParams.getWorkingDirectory(), kubernetesConfig, logCallback);

      TrafficRoutingResourceCreator trafficRoutingResourceCreator =
          k8sTrafficRoutingCreators.get(trafficRoutingConfig.getProviderConfig().getProviderType().name());

      Map<String, Pair<Integer, Integer>> dests =
          trafficRoutingResourceCreator.destinationsToMap(trafficRoutingConfig.getDestinations());
      if (trafficRoutingResourceCreator.sumDestinationsWeights(dests) != 100) {
        trafficRoutingResourceCreator.normalizeDestinations(dests, 100);
        trafficRoutingResourceCreator.logDestinationsNormalization(dests, logCallback);
      }

      List<KubernetesResource> trafficRoutingResources = trafficRoutingResourceCreator.createTrafficRoutingResources(
          trafficRoutingConfig, kubernetesConfig.getNamespace(), releaseName, availableApiVersions, logCallback);

      resources.addAll(trafficRoutingResources);

      logCallback.saveExecutionLog(format("%n%s", "Done."), INFO, SUCCESS);

      return trafficRoutingResourceCreator.getTrafficRoutingInfo(trafficRoutingResources);
    }

    logCallback.saveExecutionLog("Traffic Routing configuration was not provided.", ERROR);
    throw NestedExceptionUtils.hintWithExplanationException(
        KubernetesExceptionHints.GENERATING_TRAFFIC_ROUTING_RESOURCE_FAILED,
        KubernetesExceptionExplanation.GENERATING_TRAFFIC_ROUTING_RESOURCE_FAILED,
        new KubernetesTaskException(TRAFFIC_ROUTING_FAILED));
  }

  void applyTrafficRouting(K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    LogCallback logCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Apply, true, commandUnitsProgress);

    k8sTaskHelperBase.applyManifests(client, resources, k8sDelegateTaskParams, logCallback, true, true, "");

    logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  Optional<String> prepareTrafficRoutingPatch(K8sInfraDelegateConfig k8sInfraDelegateConfig,
      K8sTrafficRoutingConfig trafficRoutingConfig, TrafficRoutingInfoDTO trafficRoutingInfo,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    LogCallback logCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, TrafficRouting, true, commandUnitsProgress);

    logCallback.saveExecutionLog(
        format("Preparing to update Traffic Routing resource %s based on inherited configuration.%n",
            trafficRoutingInfo.getName()));

    Object trafficRoutingClusterResource = kubernetesContainerService.getCustomObject(kubernetesConfig,
        trafficRoutingInfo.getName(), k8sInfraDelegateConfig.getNamespace(), trafficRoutingInfo.getPlural(),
        K8sApiVersion.fromApiVersion(trafficRoutingInfo.getVersion()));

    TrafficRoutingResourceCreator trafficRoutingResourceCreator =
        k8sTrafficRoutingCreators.get(trafficRoutingInfo.getPlural());

    Optional<String> patch = trafficRoutingResourceCreator.generateTrafficRoutingPatch(
        trafficRoutingConfig, trafficRoutingClusterResource, logCallback);

    if (patch.isEmpty()) {
      logCallback.saveExecutionLog(format("Failed to create patch object, therefore resource %s will not be updated",
                                       trafficRoutingInfo.getName()),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw NestedExceptionUtils.hintWithExplanationException(
          format(KubernetesExceptionHints.FAILED_TO_CREATE_PATCH, trafficRoutingInfo.getVersion(),
              trafficRoutingInfo.getPlural(), trafficRoutingInfo.getName()),
          format(KubernetesExceptionExplanation.UPDATING_TRAFFIC_ROUTING_RESOURCE_FAILED, trafficRoutingInfo.getName()),
          new KubernetesTaskException(TRAFFIC_ROUTING_FAILED));
    }

    logCallback.saveExecutionLog("Done.", INFO, SUCCESS);

    return patch;
  }

  void patchTrafficRoutingResource(Optional<String> patch, TrafficRoutingInfoDTO trafficRoutingInfo,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    LogCallback logCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Apply, true, commandUnitsProgress);

    logCallback.saveExecutionLog(format("Patching %s resource with:%n%s%n", trafficRoutingInfo.getName(), patch.get()));

    Object patchedObject;
    try {
      patchedObject = kubernetesContainerService.patchCustomObject(kubernetesConfig, trafficRoutingInfo.getName(),
          K8sApiVersion.fromApiVersion(trafficRoutingInfo.getVersion()), trafficRoutingInfo.getPlural(), patch.get());
    } catch (Exception e) {
      logCallback.saveExecutionLog(
          format("Patching failed. Resource: %s is not updated.", trafficRoutingInfo.getName()), LogLevel.ERROR,
          CommandExecutionStatus.FAILURE);
      throw NestedExceptionUtils.hintWithExplanationException(
          format(KubernetesExceptionHints.PATCHING_TRAFFIC_ROUTING_RESOURCE_FAILED, trafficRoutingInfo.getVersion(),
              trafficRoutingInfo.getPlural(), trafficRoutingInfo.getName()),
          format(KubernetesExceptionExplanation.UPDATING_TRAFFIC_ROUTING_RESOURCE_FAILED, trafficRoutingInfo.getName()),
          e);
    }

    String patchedObjectYaml = removeMetadataAndConvertToYaml(patchedObject);
    logCallback.saveExecutionLog(
        format("Patch applied successfully.%nThe resource is updated:%n%s%n", patchedObjectYaml));

    logCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  private String removeMetadataAndConvertToYaml(Object patchedObject) {
    Map<String, Object> map = new ObjectMapper().convertValue(patchedObject, Map.class);
    map.remove("metadata");
    return Yaml.dump(map);
  }
}
