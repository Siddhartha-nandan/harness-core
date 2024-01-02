/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.rule.OwnerRule.BUHA;
import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.MatchType;
import io.harness.delegate.task.k8s.trafficrouting.RouteType;
import io.harness.delegate.task.k8s.trafficrouting.RuleType;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.exception.HintException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.istio.Destination;
import io.harness.k8s.model.istio.HttpRouteDestination;
import io.harness.k8s.model.istio.VirtualServiceDetails;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IstioTrafficRoutingResourceCreatorTest extends CategoryTest {
  private static final String namespace = "namespace";
  private static final String releaseName = "release-name";
  private final Set<String> apiVersions = Set.of("networking.istio.io/v1alpha3");
  private final KubernetesResource stableService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stableService").build()).build();
  private final KubernetesResource stageService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stageService").build()).build();

  IstioTrafficRoutingResourceCreator istioTrafficRoutingResourceCreator;
  @Mock Gson gson;
  @Mock LogCallback logCallback;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    istioTrafficRoutingResourceCreator = getIstioTrafficRoutingResourceCreator();
  }

  public IstioTrafficRoutingResourceCreator getIstioTrafficRoutingResourceCreator() {
    return new IstioTrafficRoutingResourceCreator();
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithOnlyDestinations() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(80).build(),
                TrafficRoutingDestination.builder().host("stage").weight(20).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService1.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteUriRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.URI, MatchType.EXACT, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceUriRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteSchemeRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.SCHEME, MatchType.EXACT, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceSchemeRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteMethodRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.METHOD, MatchType.EXACT, "GET");
    String path = "/k8s/trafficrouting/virtualServiceMethodRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteAuthorityRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.AUTHORITY, MatchType.REGEX, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceAuthorityRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteHeaderRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.HEADER, MatchType.REGEX, null);
    String path = "/k8s/trafficrouting/virtualServiceHeaderRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRoutePortRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.PORT, MatchType.EXACT, "8080");
    String path = "/k8s/trafficrouting/virtualServicePortRule.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWhenDestinationWeightIsNotProvided() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(0).build(),
                TrafficRoutingDestination.builder().host("stage").weight(0).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService2.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsNormalizeWeight() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(7).build(),
                TrafficRoutingDestination.builder().host("stage").weight(3).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService3.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithoutStableOrStage() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("firstService").weight(6).build(),
                TrafficRoutingDestination.builder().host("secondService").weight(2).build(),
                TrafficRoutingDestination.builder().host("thirdService").weight(2).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();

    istioTrafficRoutingResourceCreator.createTrafficRoutingResources(
        istioProviderConfig, namespace, releaseName, apiVersions, logCallback);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithoutStableOrStageWithHost() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .providerConfig(IstioProviderConfig.builder().hosts(List.of("root-host")).build())
            .destinations(List.of(TrafficRoutingDestination.builder().host("firstService").weight(6).build(),
                TrafficRoutingDestination.builder().host("secondService").weight(2).build(),
                TrafficRoutingDestination.builder().host("thirdService").weight(2).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .build();

    String path = "/k8s/trafficrouting/virtualService4.yaml";

    List<KubernetesResource> trafficRoutingManifests = istioTrafficRoutingResourceCreator.createTrafficRoutingResources(
        istioProviderConfig, namespace, releaseName, apiVersions, logCallback);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsMultipleRoutes() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .providerConfig(IstioProviderConfig.builder().hosts(List.of("root-host")).build())
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(0).build(),
                TrafficRoutingDestination.builder().host("stage").weight(0).build()))
            .routes(List.of(TrafficRoute.builder()
                                .routeType(RouteType.HTTP)
                                .rules(List.of(TrafficRouteRule.builder()
                                                   .ruleType(RuleType.URI)
                                                   .matchType(MatchType.EXACT)
                                                   .value("value1")
                                                   .name("rule1")
                                                   .build()))
                                .build(),
                TrafficRoute.builder()
                    .routeType(RouteType.HTTP)
                    .rules(List.of(TrafficRouteRule.builder()
                                       .ruleType(RuleType.URI)
                                       .matchType(MatchType.EXACT)
                                       .value("value2")
                                       .name("rule2")
                                       .build()))
                    .build()))
            .build();

    String path = "/k8s/trafficrouting/virtualService5.yaml";

    testK8sResourceCreation(istioProviderConfig, path);
  }

  @Test(expected = HintException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithoutRoutes() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(7).build(),
                TrafficRoutingDestination.builder().host("stage").weight(3).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();

    testK8sResourceCreation(istioProviderConfig, null);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetMainResourceKind() {
    assertThat(new IstioTrafficRoutingResourceCreator().getMainResourceKind()).isEqualTo("VirtualService");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetMainResourceKindPlural() {
    assertThat(new IstioTrafficRoutingResourceCreator().getMainResourceKindPlural()).isEqualTo("virtualservices");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetSwapTrafficRoutingPatch() {
    String expectedPatch =
        "[{ \"op\": \"replace\", \"path\": \"/spec/http\", \"value\": [{\"route\":[{\"destination\":{\"host\":\"service\"},\"weight\":100},{\"destination\":{\"host\":\"service-stage\"},\"weight\":0}]}]}]";

    Optional<String> optionalPatch =
        new IstioTrafficRoutingResourceCreator().getSwapTrafficRoutingPatch("service", "service-stage");
    assertThat(optionalPatch).isPresent();
    assertThat(optionalPatch.get()).contains(expectedPatch);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetSwapTrafficRoutingPatchIsEmpty() {
    assertThat(new IstioTrafficRoutingResourceCreator().getSwapTrafficRoutingPatch(null, "stage")).isNotPresent();
    assertThat(new IstioTrafficRoutingResourceCreator().getSwapTrafficRoutingPatch("stable", null)).isNotPresent();
    assertThat(new IstioTrafficRoutingResourceCreator().getSwapTrafficRoutingPatch(null, null)).isNotPresent();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGenerateTrafficRoutingPatch() {
    Object trafficRoutingClusterResource = new Object();
    on(istioTrafficRoutingResourceCreator).set("gson", gson);
    doThrow(new JsonParseException("json parsing error")).when(gson).toJson(trafficRoutingClusterResource);
    Optional<String> patch = istioTrafficRoutingResourceCreator.generateTrafficRoutingPatch(
        K8sTrafficRoutingConfig.builder().build(), trafficRoutingClusterResource, logCallback);
    assertThat(patch).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testCreatePatchForTrafficRoutingResourceDestinations() {
    List<VirtualServiceDetails> virtualServiceDetailsList =
        List.of(VirtualServiceDetails.builder()
                    .route(List.of(HttpRouteDestination.builder()
                                       .destination(Destination.builder().host("host1").build())
                                       .weight(10)
                                       .build(),
                        HttpRouteDestination.builder()
                            .destination(Destination.builder().host("host2").build())
                            .weight(20)
                            .build(),
                        HttpRouteDestination.builder()
                            .destination(Destination.builder().host("host3").build())
                            .weight(30)
                            .build(),
                        HttpRouteDestination.builder()
                            .destination(Destination.builder().host("host4").build())
                            .weight(40)
                            .build()))
                    .build());

    Map<String, Pair<Integer, Integer>> configuredDestinations = new LinkedHashMap<>();
    configuredDestinations.put("host1", Pair.of(40, null));

    Collection<String> patches = istioTrafficRoutingResourceCreator.createPatchForTrafficRoutingResourceDestinations(
        configuredDestinations, virtualServiceDetailsList, logCallback);

    assertThat(patches).contains(
        "{ \"op\": \"replace\", \"path\": \"/spec/http/0/route\", \"value\": [{\"destination\":{\"host\":\"host2\"},\"weight\":13},{\"destination\":{\"host\":\"host3\"},\"weight\":20},{\"destination\":{\"host\":\"host4\"},\"weight\":27},{\"destination\":{\"host\":\"host1\"},\"weight\":40}]}");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap_NullDestinations() {
    List<HttpRouteDestination> destinations = null;
    Map<String, Pair<Integer, Integer>> outDestinations =
        istioTrafficRoutingResourceCreator.destinationsToMap(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap_EmptyDestinations() {
    List<HttpRouteDestination> destinations = new ArrayList<>();
    Map<String, Pair<Integer, Integer>> outDestinations =
        istioTrafficRoutingResourceCreator.destinationsToMap(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap() {
    List<HttpRouteDestination> destinations = List.of(
        HttpRouteDestination.builder().destination(Destination.builder().host("svc1").build()).weight(null).build(),
        HttpRouteDestination.builder().destination(Destination.builder().host("svc2").build()).build(),
        HttpRouteDestination.builder().destination(Destination.builder().host("svc3").build()).weight(0).build(),
        HttpRouteDestination.builder().destination(Destination.builder().host("svc4").build()).weight(-10).build(),
        HttpRouteDestination.builder().destination(Destination.builder().host("svc5").build()).weight(22).build());

    Map<String, Pair<Integer, Integer>> outDestinations =
        istioTrafficRoutingResourceCreator.destinationsToMap(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isNotEmpty();
    assertThat(outDestinations).size().isEqualTo(5);
    assertThat(outDestinations.get("svc1")).isNotNull();
    assertThat(outDestinations.get("svc1").getLeft()).isNull();
    assertThat(outDestinations.get("svc1").getRight()).isNull();
    assertThat(outDestinations.get("svc2")).isNotNull();
    assertThat(outDestinations.get("svc2").getLeft()).isNull();
    assertThat(outDestinations.get("svc2").getRight()).isNull();
    assertThat(outDestinations.get("svc3")).isNotNull();
    assertThat(outDestinations.get("svc3").getLeft()).isEqualTo(0);
    assertThat(outDestinations.get("svc3").getRight()).isNull();
    assertThat(outDestinations.get("svc4")).isNotNull();
    assertThat(outDestinations.get("svc4").getLeft()).isNull();
    assertThat(outDestinations.get("svc4").getRight()).isNull();
    assertThat(outDestinations.get("svc5")).isNotNull();
    assertThat(outDestinations.get("svc5").getLeft()).isEqualTo(22);
    assertThat(outDestinations.get("svc5").getRight()).isNull();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testMapToDestinations_NullMap() {
    Map<String, Pair<Integer, Integer>> destinations = null;
    List<HttpRouteDestination> outDestinations = istioTrafficRoutingResourceCreator.mapToDestinations(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testMapToDestinations_EmptyMap() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    List<HttpRouteDestination> outDestinations = istioTrafficRoutingResourceCreator.mapToDestinations(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testMapToDestinations() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("svc1", Pair.of(null, null));
    destinations.put("svc2", Pair.of(-10, null));
    destinations.put("svc3", Pair.of(null, -10));
    destinations.put("svc4", Pair.of(-10, -10));
    destinations.put("svc5", Pair.of(20, null));
    destinations.put("svc6", Pair.of(null, 20));
    destinations.put("svc7", Pair.of(30, 40));

    List<HttpRouteDestination> outDestinations = istioTrafficRoutingResourceCreator.mapToDestinations(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isNotEmpty();
    assertThat(outDestinations).size().isEqualTo(7);

    Map<String, Integer> tmp = new LinkedHashMap<>();
    outDestinations.stream().forEach(trd -> tmp.put(trd.getDestination().getHost(), trd.getWeight()));
    assertThat(tmp).isNotEmpty();
    assertThat(tmp).size().isEqualTo(7);
    assertThat(tmp.get("svc1")).isNull();
    assertThat(tmp.get("svc2")).isNull();
    assertThat(tmp.get("svc3")).isNull();
    assertThat(tmp.get("svc4")).isNull();
    assertThat(tmp.get("svc5")).isEqualTo(20);
    assertThat(tmp.get("svc6")).isEqualTo(20);
    assertThat(tmp.get("svc7")).isEqualTo(40);
  }

  private void testK8sResourceCreation(K8sTrafficRoutingConfig istioProviderConfig, String path) throws IOException {
    List<KubernetesResource> trafficRoutingManifests = istioTrafficRoutingResourceCreator.createTrafficRoutingResources(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersions, logCallback);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  private void assertEqualYaml(KubernetesResource k8sResource, String path) throws IOException {
    URL url = this.getClass().getResource(path);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    assertThat(k8sResource.getSpec()).isEqualTo(fileContents);
  }

  private K8sTrafficRoutingConfig getProviderConfig(RuleType ruleType, MatchType matchType, String value) {
    return K8sTrafficRoutingConfig.builder()
        .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(80).build(),
            TrafficRoutingDestination.builder().host("stage").weight(20).build()))
        .providerConfig(IstioProviderConfig.builder().build())
        .routes(List.of(
            TrafficRoute.builder()
                .routeType(RouteType.HTTP)
                .rules(List.of(TrafficRouteRule.builder()
                                   .ruleType(ruleType)
                                   .value(value)
                                   .values(List.of("8080", "8081", "8082"))
                                   .headerConfigs(List.of(
                                       HeaderConfig.builder().matchType(matchType).key("key").value("value").build()))
                                   .matchType(matchType)
                                   .build()))
                .build()))
        .build();
  }
}
