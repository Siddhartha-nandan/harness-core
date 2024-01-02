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
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.trafficrouting.TrafficRoutingInfoDTO;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TrafficRoutingResourceCreatorTest extends CategoryTest {
  private static final String STABLE_SERVICE = "stable-service-name";
  public static final String STAGE_SERVICE = "stage-service-name";
  @Mock LogCallback logCallback;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenStable() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("stable", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STABLE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenStage() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("stage", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenCanary() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("canary", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWithoutStableOrStage() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("some-host", null, null)).isEqualTo("some-host");
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenEmptyHost() {
    getTrafficRoutingCreator().updatePlaceHoldersIfExist(null, STABLE_SERVICE, STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderStableWithoutStableService() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("stable", null, null)).isEqualTo("stable");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderStageWithoutStageService() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("stage", null, null)).isEqualTo("stage");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderCanaryWithoutStageService() {
    assertThat(getTrafficRoutingCreator().updatePlaceHoldersIfExist("canary", null, null)).isEqualTo("canary");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceNameWhenNull() {
    assertThat(getTrafficRoutingCreator().getTrafficRoutingResourceName(null, "-some-suffix", "defaultName"))
        .isEqualTo("defaultName");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceName() {
    assertThat(getTrafficRoutingCreator().getTrafficRoutingResourceName("resource-name", "-some-suffix", "defaultName"))
        .isEqualTo("resource-name-some-suffix");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceWithOverLimitResourceName() {
    String overLimitResourceName = RandomStringUtils.randomAlphabetic(260);
    String result =
        getTrafficRoutingCreator().getTrafficRoutingResourceName(overLimitResourceName, "-some-suffix", "defaultName");
    assertThat(result).endsWith("-some-suffix").hasSize(253);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetApiVersion() {
    Set<String> availableApis = Set.of("api1", "api2", "api3", "api4");
    Map<String, String> result = getTrafficRoutingCreator().getApiVersions(availableApis, logCallback);
    assertThat(result).contains(entry("key1", "api3")).contains(entry("key2", "api4"));
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetApiVersionDefault() {
    Set<String> availableApis = Set.of("api99", "api98", "api97");
    Map<String, String> result = getTrafficRoutingCreator().getApiVersions(availableApis, logCallback);
    assertThat(result).contains(entry("key1", "api3")).contains(entry("key2", "api5"));
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingInfo() {
    List<KubernetesResource> k8sResources =
        List.of(KubernetesResource.builder()
                    .resourceId(KubernetesResourceId.builder().name("resourceName").kind("kind").build())
                    .value(Map.of("apiVersion", "api1"))
                    .build(),
            KubernetesResource.builder()
                .resourceId(KubernetesResourceId.builder().name("resourceName2").kind("kind2").build())
                .value(Map.of("apiVersion", "api2"))
                .build(),
            KubernetesResource.builder()
                .resourceId(KubernetesResourceId.builder().name("resourceName3").kind("kind3").build())
                .value(Map.of("apiVersion", "api3"))
                .build());

    Optional<TrafficRoutingInfoDTO> trafficRoutingInfo = getTrafficRoutingCreator().getTrafficRoutingInfo(k8sResources);

    assertThat(trafficRoutingInfo).isPresent();
    assertThat(trafficRoutingInfo.get().getName()).isEqualTo("resourceName");
    assertThat(trafficRoutingInfo.get().getVersion()).isEqualTo("api1");
    assertThat(trafficRoutingInfo.get().getPlural()).isEqualTo("kinds");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingInfoIsEmpty() {
    List<KubernetesResource> k8sResources =
        List.of(KubernetesResource.builder()
                    .resourceId(KubernetesResourceId.builder().name("resourceName1").kind("kind1").build())
                    .value(Map.of("apiVersion", "api1"))
                    .build(),
            KubernetesResource.builder()
                .resourceId(KubernetesResourceId.builder().name("resourceName2").kind("kind2").build())
                .value(Map.of("apiVersion", "api2"))
                .build(),
            KubernetesResource.builder()
                .resourceId(KubernetesResourceId.builder().name("resourceName3").kind("kind3").build())
                .value(Map.of("apiVersion", "api3"))
                .build());

    Optional<TrafficRoutingInfoDTO> trafficRoutingInfo = getTrafficRoutingCreator().getTrafficRoutingInfo(k8sResources);

    assertThat(trafficRoutingInfo).isNotPresent();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testLogDestinationsNormalization() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("svc1", Pair.of(10, 10));
    destinations.put("svc2", Pair.of(10, 20));
    destinations.put("svc3", Pair.of(null, 10));
    destinations.put("svc4", Pair.of(10, null));
    destinations.put("svc5", Pair.of(null, null));

    ArgumentCaptor<String> logCallbackCapture = ArgumentCaptor.forClass(String.class);

    getTrafficRoutingCreator().logDestinationsNormalization(destinations, logCallback);

    verify(logCallback, times(1)).saveExecutionLog(logCallbackCapture.capture());

    logCallbackCapture.getValue().contains("Destination [svc2] weight will be normalized from [10] to [20].");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap_NullDestinations() {
    List<TrafficRoutingDestination> destinations = null;
    Map<String, Pair<Integer, Integer>> outDestinations = getTrafficRoutingCreator().destinationsToMap(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap_EmptyDestinations() {
    List<TrafficRoutingDestination> destinations = new ArrayList<>();
    Map<String, Pair<Integer, Integer>> outDestinations = getTrafficRoutingCreator().destinationsToMap(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testDestinationsToMap() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("svc1").weight(null).build(),
            TrafficRoutingDestination.builder().host("svc2").build(),
            TrafficRoutingDestination.builder().host("svc3").weight(0).build(),
            TrafficRoutingDestination.builder().host("svc4").weight(-10).build(),
            TrafficRoutingDestination.builder().host("svc5").weight(22).build());

    Map<String, Pair<Integer, Integer>> outDestinations = getTrafficRoutingCreator().destinationsToMap(destinations);
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
    List<TrafficRoutingDestination> outDestinations = getTrafficRoutingCreator().mapToDestinations(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isEmpty();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testMapToDestinations_EmptyMap() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    List<TrafficRoutingDestination> outDestinations = getTrafficRoutingCreator().mapToDestinations(destinations);
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

    List<TrafficRoutingDestination> outDestinations = getTrafficRoutingCreator().mapToDestinations(destinations);
    assertThat(outDestinations).isNotNull();
    assertThat(outDestinations).isNotEmpty();
    assertThat(outDestinations).size().isEqualTo(7);

    Map<String, Integer> tmp = new LinkedHashMap<>();
    outDestinations.stream().forEach(trd -> tmp.put(trd.getHost(), trd.getWeight()));
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

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSumDestinationsWeights_NullMap() {
    Map<String, Pair<Integer, Integer>> destinations = null;
    assertThat(getTrafficRoutingCreator().sumDestinationsWeights(destinations)).isEqualTo(0);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSumDestinationsWeights_EmptyMap() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    assertThat(getTrafficRoutingCreator().sumDestinationsWeights(destinations)).isEqualTo(0);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testSumDestinationsWeights() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("svc1", Pair.of(null, null));
    destinations.put("svc2", Pair.of(-10, null));
    destinations.put("svc3", Pair.of(null, -10));
    destinations.put("svc4", Pair.of(-10, -10));
    destinations.put("svc5", Pair.of(20, null));
    destinations.put("svc6", Pair.of(null, 20));
    destinations.put("svc7", Pair.of(30, 40));
    destinations.put("svc8", Pair.of(15, 5));

    assertThat(getTrafficRoutingCreator().sumDestinationsWeights(destinations)).isEqualTo(85);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeWeight() {
    TrafficRoutingResourceCreator trafficRoutingResourceCreator = getTrafficRoutingCreator();
    assertThat(trafficRoutingResourceCreator.normalizeWeight(0, 0, 5, 100)).isEqualTo(20);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(0, 0, 5, 50)).isEqualTo(10);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(0, 0, 4, 10)).isEqualTo(3);

    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 20, 3, 60)).isEqualTo(13);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 30, 3, 60)).isEqualTo(20);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 40, 3, 60)).isEqualTo(27);

    assertThat(trafficRoutingResourceCreator.normalizeWeight(100, null, 5, 100)).isEqualTo(0);

    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 20, 3, 1)).isEqualTo(0);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 30, 3, 1)).isEqualTo(0);
    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 40, 3, 1)).isEqualTo(0);

    assertThat(trafficRoutingResourceCreator.normalizeWeight(90, 45, 3, 1)).isEqualTo(1);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testShouldNormalizeDestinations_NullPair() {
    Pair<Map, Map> filteredDestinations = null;
    assertThat(getTrafficRoutingCreator().shouldNormalizeDestinations(filteredDestinations)).isFalse();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testShouldNormalizeDestinations_EmptyPair() {
    assertThat(getTrafficRoutingCreator().shouldNormalizeDestinations(Pair.of(null, null))).isFalse();
    assertThat(
        getTrafficRoutingCreator().shouldNormalizeDestinations(Pair.of(new LinkedHashMap(), new LinkedHashMap())))
        .isFalse();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testShouldNormalizeDestinations() {
    Map<String, Pair<Integer, Integer>> matchedDestinations = new HashMap<>();
    matchedDestinations.put("svc", Pair.of(10, 20));
    Pair<Map, Map> filteredDestinations = Pair.of(new LinkedHashMap(), matchedDestinations);
    assertThat(getTrafficRoutingCreator().shouldNormalizeDestinations(filteredDestinations)).isTrue();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testFilterDestinations() {
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("svc1", Pair.of(10, 20));
    destinations.put("svc2", Pair.of(10, null));
    destinations.put("svc3", Pair.of(15, 25));
    destinations.put("svc4", Pair.of(null, null));
    destinations.put("svc5", null);

    Pair<Map, Map> filteredDestinations = getTrafficRoutingCreator().filterDestinations(destinations);
    assertThat(filteredDestinations.getLeft().size()).isEqualTo(2);
    assertThat(filteredDestinations.getLeft().keySet()).contains("svc2", "svc4");
    assertThat(filteredDestinations.getLeft().get("svc2")).isNotNull();
    assertThat(filteredDestinations.getLeft().get("svc4")).isNotNull();
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getLeft().get("svc2")).getLeft()).isNotNull();
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getLeft().get("svc2")).getLeft()).isEqualTo(10);
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getLeft().get("svc4")).getLeft()).isNull();
    assertThat(filteredDestinations.getRight().size()).isEqualTo(2);
    assertThat(filteredDestinations.getRight().keySet()).contains("svc1", "svc3");
    assertThat(filteredDestinations.getRight().get("svc1")).isNotNull();
    assertThat(filteredDestinations.getRight().get("svc3")).isNotNull();
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getRight().get("svc1")).getRight()).isNotNull();
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getRight().get("svc1")).getRight()).isEqualTo(20);
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getRight().get("svc3")).getRight()).isNotNull();
    assertThat(((Pair<Integer, Integer>) filteredDestinations.getRight().get("svc3")).getRight()).isEqualTo(25);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeFilteredDestinations() {
    Map<String, Pair<Integer, Integer>> matched = new LinkedHashMap<>();
    matched.put("host1", Pair.of(10, 110));
    Map<String, Pair<Integer, Integer>> nonMatched = new LinkedHashMap<>();
    nonMatched.put("host2", Pair.of(20, null));
    nonMatched.put("host3", Pair.of(30, null));
    nonMatched.put("host4", Pair.of(40, null));

    Map<String, Pair<Integer, Integer>> normalizedFilteredDestinations =
        getTrafficRoutingCreator().normalizeFilteredDestinations(Pair.of(nonMatched, matched));

    assertThat(normalizedFilteredDestinations.size()).isEqualTo(4);
    assertThat(normalizedFilteredDestinations.get("host1").getLeft()).isEqualTo(10);
    assertThat(normalizedFilteredDestinations.get("host1").getRight()).isEqualTo(100);
    assertThat(normalizedFilteredDestinations.get("host2").getLeft()).isEqualTo(20);
    assertThat(normalizedFilteredDestinations.get("host2").getRight()).isEqualTo(0);
    assertThat(normalizedFilteredDestinations.get("host3").getLeft()).isEqualTo(30);
    assertThat(normalizedFilteredDestinations.get("host3").getRight()).isEqualTo(0);
    assertThat(normalizedFilteredDestinations.get("host4").getLeft()).isEqualTo(40);
    assertThat(normalizedFilteredDestinations.get("host4").getRight()).isEqualTo(0);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinations() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(20, null));
    destinations.put("host2", Pair.of(30, null));
    destinations.put("host3", Pair.of(50, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(20);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(20);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(30);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(30);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(50);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsUnder10() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(2, null));
    destinations.put("host2", Pair.of(3, null));
    destinations.put("host3", Pair.of(5, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(2);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(20);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(3);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(30);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(5);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsOver10() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(200, null));
    destinations.put("host2", Pair.of(300, null));
    destinations.put("host3", Pair.of(500, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(200);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(20);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(300);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(30);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(500);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsCustomNumber() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(1, null));
    destinations.put("host2", Pair.of(3, null));
    destinations.put("host3", Pair.of(4, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(1);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(13);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(3);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(38);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(4);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(49);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsWhenWeights0() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(0, null));
    destinations.put("host2", Pair.of(0, null));
    destinations.put("host3", Pair.of(0, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(34);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(33);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(33);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsWhenWeightsNotProvided() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(null, null));
    destinations.put("host2", Pair.of(null, null));
    destinations.put("host3", Pair.of(null, null));
    destinations.put("host4", Pair.of(null, null));
    destinations.put("host5", Pair.of(null, null));
    destinations.put("host6", Pair.of(null, null));
    destinations.put("host7", Pair.of(null, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.get("host1").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(15);
    assertThat(normalizedDestinations.get("host2").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(15);
    assertThat(normalizedDestinations.get("host3").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(14);
    assertThat(normalizedDestinations.get("host4").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host4").getRight()).isEqualTo(14);
    assertThat(normalizedDestinations.get("host5").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host5").getRight()).isEqualTo(14);
    assertThat(normalizedDestinations.get("host6").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host6").getRight()).isEqualTo(14);
    assertThat(normalizedDestinations.get("host7").getLeft()).isNull();
    assertThat(normalizedDestinations.get("host7").getRight()).isEqualTo(14);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsWhenMixed7Weights() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(0, null));
    destinations.put("host2", Pair.of(34, null));
    destinations.put("host3", Pair.of(13, null));
    destinations.put("host4", Pair.of(23, null));
    destinations.put("host5", Pair.of(6, null));
    destinations.put("host6", Pair.of(25, null));
    destinations.put("host7", Pair.of(0, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.size()).isEqualTo(7);
    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(34);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(33);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(13);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(13);
    assertThat(normalizedDestinations.get("host4").getLeft()).isEqualTo(23);
    assertThat(normalizedDestinations.get("host4").getRight()).isEqualTo(23);
    assertThat(normalizedDestinations.get("host5").getLeft()).isEqualTo(6);
    assertThat(normalizedDestinations.get("host5").getRight()).isEqualTo(6);
    assertThat(normalizedDestinations.get("host6").getLeft()).isEqualTo(25);
    assertThat(normalizedDestinations.get("host6").getRight()).isEqualTo(25);
    assertThat(normalizedDestinations.get("host7").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host7").getRight()).isEqualTo(0);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testNormalizeDestinationsWhenMixed8Weights() {
    int cap = 100;
    Map<String, Pair<Integer, Integer>> destinations = new LinkedHashMap<>();
    destinations.put("host1", Pair.of(0, null));
    destinations.put("host2", Pair.of(34, null));
    destinations.put("host3", Pair.of(13, null));
    destinations.put("host4", Pair.of(0, null));
    destinations.put("host5", Pair.of(23, null));
    destinations.put("host6", Pair.of(6, null));
    destinations.put("host7", Pair.of(28, null));
    destinations.put("host8", Pair.of(0, null));

    Map<String, Pair<Integer, Integer>> normalizedDestinations =
        getTrafficRoutingCreator().normalizeDestinations(destinations, cap);

    assertThat(normalizedDestinations.size()).isEqualTo(8);
    assertThat(normalizedDestinations.get("host1").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host1").getRight()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host2").getLeft()).isEqualTo(34);
    assertThat(normalizedDestinations.get("host2").getRight()).isEqualTo(32);
    assertThat(normalizedDestinations.get("host3").getLeft()).isEqualTo(13);
    assertThat(normalizedDestinations.get("host3").getRight()).isEqualTo(13);
    assertThat(normalizedDestinations.get("host4").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host4").getRight()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host5").getLeft()).isEqualTo(23);
    assertThat(normalizedDestinations.get("host5").getRight()).isEqualTo(22);
    assertThat(normalizedDestinations.get("host6").getLeft()).isEqualTo(6);
    assertThat(normalizedDestinations.get("host6").getRight()).isEqualTo(6);
    assertThat(normalizedDestinations.get("host7").getLeft()).isEqualTo(28);
    assertThat(normalizedDestinations.get("host7").getRight()).isEqualTo(27);
    assertThat(normalizedDestinations.get("host8").getLeft()).isEqualTo(0);
    assertThat(normalizedDestinations.get("host8").getRight()).isEqualTo(0);
  }

  public TrafficRoutingResourceCreator getTrafficRoutingCreator() {
    return new TrafficRoutingResourceCreator() {
      @Override
      protected List<String> getManifests(K8sTrafficRoutingConfig k8sTrafficRoutingConfig, String namespace,
          String releaseName, String stableName, String stageName, Map<String, String> apiVersions) {
        return null;
      }

      @Override
      protected Map<String, List<String>> getProviderVersionMap() {
        return Map.of("key1", List.of("api1", "api2", "api3"), "key2", List.of("api4", "api5"));
      }

      @Override
      protected String getMainResourceKind() {
        return "kind";
      }

      @Override
      protected String getMainResourceKindPlural() {
        return "kinds";
      }

      @Override
      public Optional<String> getSwapTrafficRoutingPatch(String stable, String stage) {
        return Optional.of("patch");
      }

      @Override
      public Optional<String> generateTrafficRoutingPatch(K8sTrafficRoutingConfig k8sTrafficRoutingConfig,
          Object trafficRoutingClusterResource, LogCallback logCallback) {
        return Optional.of("trafficroutingpatch");
      }
    };
  }
}
