/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.RouteType;
import io.harness.delegate.task.k8s.trafficrouting.SMIProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.delegate.task.k8s.trafficrouting.util.HarnessTrafficRoutingUtils;
import io.harness.delegate.task.k8s.trafficrouting.util.SMITrafficRoutingUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.smi.Backend;
import io.harness.k8s.model.smi.HttpRouteGroup;
import io.harness.k8s.model.smi.Match;
import io.harness.k8s.model.smi.Metadata;
import io.harness.k8s.model.smi.RouteMatch;
import io.harness.k8s.model.smi.RouteSpec;
import io.harness.k8s.model.smi.SMIRoute;
import io.harness.k8s.model.smi.TrafficSplit;
import io.harness.k8s.model.smi.TrafficSplitSpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

@NoArgsConstructor
@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class SMITrafficRoutingResourceCreator extends TrafficRoutingResourceCreator {
  @Inject SMITrafficRoutingUtils smiTrafficRoutingUtils;
  @Inject HarnessTrafficRoutingUtils harnessTrafficRoutingUtils;
  public static final String PLURAL = "trafficsplits";

  private static final String TRAFFIC_SPLIT_SUFFIX = "-traffic-split";
  private static final String HTTP_ROUTE_GROUP_SUFFIX = "-http-route-group";
  // toDo this needs to be revisited, should not be hardcoded
  private static final String TRAFFIC_SPLIT_DEFAULT_NAME = "harness-traffic-routing-traffic-split";

  static final String SPLIT = "split";
  static final String SPECS = "specs";
  private static final String BACKENDS_PATH = "/spec/backends";
  private static final Map<String, List<String>> SUPPORTED_API_MAP = Map.of(SPLIT,
      List.of("split.smi-spec.io/v1alpha1", "split.smi-spec.io/v1alpha2", "split.smi-spec.io/v1alpha3",
          "split.smi-spec.io/v1alpha4"),
      SPECS,
      List.of("specs.smi-spec.io/v1alpha1", "specs.smi-spec.io/v1alpha2", "specs.smi-spec.io/v1alpha3",
          "specs.smi-spec.io/v1alpha4"));

  @Override
  protected List<String> getManifests(K8sTrafficRoutingConfig k8sTrafficRoutingConfig, String namespace,
      String releaseName, String stableName, String stageName, Map<String, String> apiVersions) {
    TrafficSplit trafficSplit =
        getTrafficSplit(k8sTrafficRoutingConfig, namespace, releaseName, stableName, stageName, apiVersions.get(SPLIT));

    List<SMIRoute> smiRoutes =
        getSMIRoutes(k8sTrafficRoutingConfig.getRoutes(), namespace, releaseName, apiVersions.get(SPECS));

    applyRoutesToTheTrafficSplit(trafficSplit, smiRoutes);
    List<String> trafficRoutingManifests = new ArrayList<>();
    trafficRoutingManifests.add(Yaml.dump(trafficSplit));
    trafficRoutingManifests.addAll(smiRoutes.stream().map(Yaml::dump).collect(Collectors.toList()));

    return trafficRoutingManifests;
  }

  @Override
  protected Map<String, List<String>> getProviderVersionMap() {
    return SUPPORTED_API_MAP;
  }

  @Override
  protected String getMainResourceKind() {
    return "TrafficSplit";
  }

  @Override
  protected String getMainResourceKindPlural() {
    return PLURAL;
  }

  @Override
  public Optional<String> getSwapTrafficRoutingPatch(String stable, String stage) {
    if (isNotEmpty(stable) && isNotEmpty(stage)) {
      List<Backend> backends = List.of(
          Backend.builder().service(stable).weight(100).build(), Backend.builder().service(stage).weight(0).build());

      try {
        return Optional.of(format(format("[%s]", PATCH_REPLACE_JSON_FORMAT), BACKENDS_PATH,
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(backends)));
      } catch (JsonProcessingException e) {
        log.warn("Failed to Deserialize List of Backends", e);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getTrafficRoutingPatch(K8sTrafficRoutingConfig k8sTrafficRoutingConfig,
      Object trafficRoutingClusterResource) throws JsonProcessingException {
    List<String> listOfPatches = new ArrayList<>();
    String trafficRoutingClusterResourceJson = new Gson().toJson(trafficRoutingClusterResource);
    TrafficSplit trafficSplit = new ObjectMapper().readValue(trafficRoutingClusterResourceJson, TrafficSplit.class);
    listOfPatches.addAll(createPatchForTrafficRoutingResourceDestinations(
        k8sTrafficRoutingConfig.getDestinations(), trafficSplit.getSpec().getBackends()));

    if (listOfPatches.size() > 0) {
      return Optional.of(listOfPatches.toString());
    }

    return Optional.empty();
  }

  private Collection<String> createPatchForTrafficRoutingResourceDestinations(
      List<TrafficRoutingDestination> configuredDestinations, List<Backend> backendList) {
    List<String> patches = new ArrayList<>();
    if (backendList != null) {
      List<Backend> allDestinations = new LinkedList<>();
      List<Backend> matchedDestinations = new LinkedList<>();
      List<Backend> nonMatchedDestinations = new LinkedList<>();

      int matchedDestinationsSum = 0;
      // looping through destinations and checking for matching destinations
      for (Backend backend : backendList) {
        for (TrafficRoutingDestination trafficRoutingDestination : configuredDestinations) {
          if (backend.getService().equals(trafficRoutingDestination.getHost())) {
            if (!matchedDestinations.contains(backend)) {
              backend.setWeight(trafficRoutingDestination.getWeight());
              matchedDestinations.add(backend);
              matchedDestinationsSum +=
                  trafficRoutingDestination.getWeight() == null ? 0 : trafficRoutingDestination.getWeight();
            }
            if (nonMatchedDestinations.contains(backend)) {
              nonMatchedDestinations.remove(backend);
            }
            break;
          } else {
            if (!nonMatchedDestinations.contains(backend)) {
              nonMatchedDestinations.add(backend);
            }
          }
        }
      }

      // updating matched destinations weights and normalize remaining ones
      if (matchedDestinations.size() > 0) {
        if (matchedDestinationsSum < 100) {
          allDestinations.addAll(
              smiTrafficRoutingUtils.normalizeDestinations(nonMatchedDestinations, 100 - matchedDestinationsSum));
          allDestinations.addAll(matchedDestinations);
        } else {
          allDestinations.addAll(smiTrafficRoutingUtils.normalizeDestinations(matchedDestinations, 100));
          allDestinations.addAll(smiTrafficRoutingUtils.normalizeDestinations(nonMatchedDestinations, 0));
        }

        // creating a patch for this particular route type and route with updated destinations
        try {
          patches.add(format(PATCH_REPLACE_JSON_FORMAT, BACKENDS_PATH,
              new ObjectMapper()
                  .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                  .writeValueAsString(allDestinations)));
        } catch (JsonProcessingException e) {
          log.warn("Failed to Deserialize List of VirtualServiceDetails", e);
        }
      }
    }
    return patches;
  }

  private TrafficSplit getTrafficSplit(K8sTrafficRoutingConfig k8sTrafficRoutingConfig, String namespace,
      String releaseName, String stableName, String stageName, String apiVersion) {
    String name = getTrafficRoutingResourceName(stableName, TRAFFIC_SPLIT_SUFFIX, TRAFFIC_SPLIT_DEFAULT_NAME);
    Metadata metadata = getMetadata(name, namespace, releaseName);
    String rootService = getRootService((SMIProviderConfig) k8sTrafficRoutingConfig.getProviderConfig(), stableName);
    rootService = updatePlaceHoldersIfExist(rootService, stableName, stageName);

    return TrafficSplit.builder()
        .metadata(metadata)
        .apiVersion(apiVersion)
        .spec(TrafficSplitSpec.builder()
                  .backends(getBackends(
                      harnessTrafficRoutingUtils.normalizeDestinations(k8sTrafficRoutingConfig.getDestinations()),
                      stableName, stageName))
                  .service(rootService)
                  .build())
        .build();
  }

  private List<Backend> getBackends(
      List<TrafficRoutingDestination> normalizedDestinations, String stableName, String stageName) {
    return normalizedDestinations.stream()
        .map(dest
            -> Backend.builder()
                   .service(updatePlaceHoldersIfExist(dest.getHost(), stableName, stageName))
                   .weight(dest.getWeight())
                   .build())
        .collect(Collectors.toList());
  }

  private String getRootService(SMIProviderConfig k8sTrafficRoutingConfig, String stableName) {
    String rootService = k8sTrafficRoutingConfig.getRootService();
    if (isEmpty(rootService)) {
      if (isEmpty(stableName)) {
        throw NestedExceptionUtils.hintWithExplanationException(
            format(KubernetesExceptionHints.TRAFFIC_ROUTING_MISSING_FIELD, "rootService", "SMI"),
            format(KubernetesExceptionExplanation.TRAFFIC_ROUTING_MISSING_FIELD, "rootService"),
            new InvalidArgumentsException(
                "Root service must be provided in the Traffic Routing config for SMI provider"));
      }
      rootService = stableName;
    }
    return rootService;
  }

  private Metadata getMetadata(String name, String namespace, String releaseName) {
    return Metadata.builder()
        .name(name)
        .namespace(namespace)
        .labels(Map.of(HarnessLabels.releaseName, releaseName))
        .build();
  }

  private List<SMIRoute> getSMIRoutes(List<TrafficRoute> routes, String namespace, String releaseName, String api) {
    List<SMIRoute> smiRoutes = new ArrayList<>();
    if (routes != null) {
      smiRoutes.addAll(getHttpRoutes(routes, namespace, releaseName, api));
    }
    return smiRoutes;
  }

  private List<HttpRouteGroup> getHttpRoutes(
      List<TrafficRoute> routes, String namespace, String releaseName, String api) {
    return routes.stream()
        .filter(route -> route.getRouteType() == RouteType.HTTP)
        .flatMap(route
            -> route.getRules() == null
                ? Stream.empty()
                : route.getRules().stream().map(rule -> mapToHttpRouteGroup(rule, namespace, releaseName, api)))
        .collect(Collectors.toList());
  }

  private HttpRouteGroup mapToHttpRouteGroup(
      TrafficRouteRule rule, String namespace, String releaseName, String apiVersion) {
    String defaultName =
        String.format("harness%s-%s", HTTP_ROUTE_GROUP_SUFFIX, RandomStringUtils.randomAlphanumeric(4));

    String resourceName = getTrafficRoutingResourceName(rule.getName(), HTTP_ROUTE_GROUP_SUFFIX, defaultName);
    Map<String, String> headerConfig = rule.getHeaderConfigs() == null
        ? null
        : rule.getHeaderConfigs().stream().collect(Collectors.toMap(HeaderConfig::getKey, HeaderConfig::getValue));

    return HttpRouteGroup.builder()
        .metadata(getMetadata(resourceName, namespace, releaseName))
        .apiVersion(apiVersion)
        .spec(RouteSpec.builder()
                  .matches(List.of(
                      Match.createMatch(rule.getRuleType().name(), rule.getName(), rule.getValue(), headerConfig)))
                  .build())
        .build();
  }

  private void applyRoutesToTheTrafficSplit(TrafficSplit trafficSplit, List<SMIRoute> smiRoutes) {
    if (isNotEmpty(smiRoutes)) {
      trafficSplit.getSpec().setMatches(
          smiRoutes.stream()
              .map(route -> RouteMatch.builder().kind(route.getKind()).name(route.getMetadata().getName()).build())
              .collect(Collectors.toList()));
    }
  }
}
