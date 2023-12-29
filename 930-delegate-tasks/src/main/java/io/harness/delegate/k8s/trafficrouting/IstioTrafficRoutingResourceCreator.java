/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.k8s.trafficrouting.RouteType.HTTP;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.delegate.task.k8s.trafficrouting.util.HarnessTrafficRoutingUtils;
import io.harness.delegate.task.k8s.trafficrouting.util.IstioTrafficRoutingUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.istio.Destination;
import io.harness.k8s.model.istio.HttpRouteDestination;
import io.harness.k8s.model.istio.Match;
import io.harness.k8s.model.istio.Metadata;
import io.harness.k8s.model.istio.VirtualService;
import io.harness.k8s.model.istio.VirtualServiceDetails;
import io.harness.k8s.model.istio.VirtualServiceSpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@NoArgsConstructor
@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class IstioTrafficRoutingResourceCreator extends TrafficRoutingResourceCreator {
  @Inject IstioTrafficRoutingUtils istioTrafficRoutingUtils;
  @Inject HarnessTrafficRoutingUtils harnessTrafficRoutingUtils;
  public static final String PLURAL = "virtualservices";

  private static final String VS_SUFFIX = "-virtual-service";
  // toDo this needs to be revisited, should not be hardcoded
  private static final String TRAFFIC_ROUTING_STEP_VIRTUAL_SERVICE = "harness-traffic-routing-virtual-service";
  private static final String NETWORKING = "networking";
  private static final String HTTP_ROUTE_TYPE_PATH = "/spec/http";
  private static final String HTTP_ROUTE_TYPE_ROUTE_DESTINATION_PATH =
      format("%s%s", HTTP_ROUTE_TYPE_PATH, "/%d/route");
  private static final Map<String, List<String>> SUPPORTED_API_MAP =
      Map.of(NETWORKING, List.of("networking.istio.io/v1alpha3", "networking.istio.io/v1beta1"));

  @Override
  protected List<String> getManifests(K8sTrafficRoutingConfig k8sTrafficRoutingConfig, String namespace,
      String releaseName, String stableName, String stageName, Map<String, String> apiVersions) {
    String virtualServiceName =
        getTrafficRoutingResourceName(stableName, VS_SUFFIX, TRAFFIC_ROUTING_STEP_VIRTUAL_SERVICE);
    VirtualService vs = VirtualService.builder()
                            .metadata(Metadata.builder()
                                          .name(virtualServiceName)
                                          .namespace(namespace)
                                          .labels(Map.of(HarnessLabels.releaseName, releaseName))
                                          .build())
                            .apiVersion(apiVersions.get(NETWORKING))
                            .spec(getVirtualServiceSpec(k8sTrafficRoutingConfig, stableName, stageName))
                            .build();
    return List.of(Yaml.dump(vs));
  }

  @Override
  protected Map<String, List<String>> getProviderVersionMap() {
    return SUPPORTED_API_MAP;
  }

  @Override
  protected String getMainResourceKind() {
    return "VirtualService";
  }

  @Override
  protected String getMainResourceKindPlural() {
    return PLURAL;
  }

  @Override
  public Optional<String> getSwapTrafficRoutingPatch(String stable, String stage) {
    if (isNotEmpty(stable) && isNotEmpty(stage)) {
      List<VirtualServiceDetails> virtualServiceDetails =
          List.of(VirtualServiceDetails.builder()
                      .route(List.of(HttpRouteDestination.builder()
                                         .destination(Destination.builder().host(stable).build())
                                         .weight(100)
                                         .build(),
                          HttpRouteDestination.builder()
                              .destination(Destination.builder().host(stage).build())
                              .weight(0)
                              .build()))
                      .build());

      try {
        return Optional.of(format(format("[%s]", PATCH_REPLACE_JSON_FORMAT), HTTP_ROUTE_TYPE_PATH,
            new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(virtualServiceDetails)));
      } catch (JsonProcessingException e) {
        log.warn("Failed to Deserialize List of VirtualServiceDetails", e);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getTrafficRoutingPatch(K8sTrafficRoutingConfig k8sTrafficRoutingConfig,
      Object trafficRoutingClusterResource) throws JsonProcessingException {
    List<String> listOfPatches = new ArrayList<>();
    String trafficRoutingClusterResourceJson = new Gson().toJson(trafficRoutingClusterResource);
    VirtualService virtualService =
        new ObjectMapper().readValue(trafficRoutingClusterResourceJson, VirtualService.class);

    listOfPatches.addAll(createPatchForTrafficRoutingResourceDestinations(
        k8sTrafficRoutingConfig.getDestinations(), virtualService.getSpec().getHttp()));
    listOfPatches.addAll(createPatchForTrafficRoutingResourceDestinations(
        k8sTrafficRoutingConfig.getDestinations(), virtualService.getSpec().getTcp()));
    listOfPatches.addAll(createPatchForTrafficRoutingResourceDestinations(
        k8sTrafficRoutingConfig.getDestinations(), virtualService.getSpec().getTls()));

    if (listOfPatches.size() > 0) {
      return Optional.of(listOfPatches.toString());
    }

    return Optional.empty();
  }

  private Collection<String> createPatchForTrafficRoutingResourceDestinations(
      List<TrafficRoutingDestination> configuredDestinations, List<VirtualServiceDetails> virtualServiceDetailsList) {
    List<String> patches = new ArrayList<>();
    if (virtualServiceDetailsList != null) {
      for (int i = 0; i < virtualServiceDetailsList.size(); i++) {
        List<HttpRouteDestination> allDestinations = new LinkedList<>();
        List<HttpRouteDestination> matchedDestinations = new LinkedList<>();
        List<HttpRouteDestination> nonMatchedDestinations = new LinkedList<>();

        int matchedDestinationsSum = 0;
        // looping through destinations and checking for matching destinations
        for (HttpRouteDestination httpRouteDestination : virtualServiceDetailsList.get(i).getRoute()) {
          for (TrafficRoutingDestination trafficRoutingDestination : configuredDestinations) {
            if (httpRouteDestination.getDestination().getHost().equals(trafficRoutingDestination.getHost())) {
              if (!matchedDestinations.contains(httpRouteDestination)) {
                httpRouteDestination.setWeight(trafficRoutingDestination.getWeight());
                matchedDestinations.add(httpRouteDestination);
                matchedDestinationsSum +=
                    trafficRoutingDestination.getWeight() == null ? 0 : trafficRoutingDestination.getWeight();
              }
              if (nonMatchedDestinations.contains(httpRouteDestination)) {
                nonMatchedDestinations.remove(httpRouteDestination);
              }
              break;
            } else {
              if (!nonMatchedDestinations.contains(httpRouteDestination)) {
                nonMatchedDestinations.add(httpRouteDestination);
              }
            }
          }
        }

        // updating matched destinations weights and normalize remaining ones
        if (matchedDestinations.size() > 0) {
          if (matchedDestinationsSum < 100) {
            allDestinations.addAll(
                istioTrafficRoutingUtils.normalizeDestinations(nonMatchedDestinations, 100 - matchedDestinationsSum));
            allDestinations.addAll(matchedDestinations);
          } else {
            allDestinations.addAll(istioTrafficRoutingUtils.normalizeDestinations(matchedDestinations, 100));
            allDestinations.addAll(istioTrafficRoutingUtils.normalizeDestinations(nonMatchedDestinations, 0));
          }

          // creating a patch for this particular route type and route with updated destinations
          try {
            patches.add(format(PATCH_REPLACE_JSON_FORMAT, format(HTTP_ROUTE_TYPE_ROUTE_DESTINATION_PATH, i),
                new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writeValueAsString(allDestinations)));
          } catch (JsonProcessingException e) {
            log.warn("Failed to Deserialize List of VirtualServiceDetails", e);
          }
        }
      }
    }
    return patches;
  }

  private VirtualServiceSpec getVirtualServiceSpec(
      K8sTrafficRoutingConfig k8sTrafficRoutingConfig, String stableName, String stageName) {
    IstioProviderConfig providerConfig = (IstioProviderConfig) k8sTrafficRoutingConfig.getProviderConfig();
    List<String> hosts = providerConfig.getHosts();
    if (isEmpty(hosts)) {
      if (isEmpty(stableName)) {
        throw new InvalidArgumentsException("Hosts should be specified in the Istio Traffic Routing Config");
      }
      hosts = List.of(stableName);
    }

    return VirtualServiceSpec.builder()
        .gateways(providerConfig.getGateways())
        .hosts(hosts)
        .http(getHttpRouteSpec(k8sTrafficRoutingConfig.getRoutes(),
            harnessTrafficRoutingUtils.normalizeDestinations(k8sTrafficRoutingConfig.getDestinations()), stableName,
            stageName))
        .build();
  }

  private List<VirtualServiceDetails> getHttpRouteSpec(
      List<TrafficRoute> routes, List<TrafficRoutingDestination> destinations, String stableName, String stageName) {
    if (routes == null) {
      throw NestedExceptionUtils.hintWithExplanationException(
          format(KubernetesExceptionHints.TRAFFIC_ROUTING_MISSING_FIELD, "routes", "ISTIO"),
          format(KubernetesExceptionExplanation.TRAFFIC_ROUTING_MISSING_FIELD, "routes"),
          new InvalidArgumentsException("Traffic Routes are missing in the Traffic Routing configuration"));
    }
    return routes.stream()
        .filter(route -> route.getRouteType() == HTTP)
        .map(route
            -> VirtualServiceDetails.builder()
                   .match(getIstioMatch(route.getRules()))
                   .route(getRouteDestinations(destinations, stableName, stageName))
                   .build())
        .collect(Collectors.toList());
  }

  private List<Match> getIstioMatch(List<TrafficRouteRule> rules) {
    if (rules == null) {
      return null;
    }
    return rules.stream()
        .map(rule
            -> Match.createMatch(rule.getRuleType().name(), rule.getName(), rule.getValue(), rule.getMatchType().name(),
                mapHeaderConfigs(rule.getHeaderConfigs())))
        .collect(Collectors.toList());
  }

  private Map<String, Pair<String, String>> mapHeaderConfigs(List<HeaderConfig> headerConfigs) {
    return headerConfigs != null ? headerConfigs.stream().collect(Collectors.toMap(HeaderConfig::getKey,
               headerConfig -> Pair.of(headerConfig.getValue(), headerConfig.getMatchType().name())))
                                 : Collections.emptyMap();
  }

  private List<HttpRouteDestination> getRouteDestinations(
      List<TrafficRoutingDestination> destinations, String stableName, String stageName) {
    return destinations.stream()
        .map(destination
            -> HttpRouteDestination.builder()
                   .weight(destination.getWeight())
                   .destination(Destination.builder()
                                    .host(updatePlaceHoldersIfExist(destination.getHost(), stableName, stageName))
                                    .build())
                   .build())
        .collect(Collectors.toList());
  }
}
