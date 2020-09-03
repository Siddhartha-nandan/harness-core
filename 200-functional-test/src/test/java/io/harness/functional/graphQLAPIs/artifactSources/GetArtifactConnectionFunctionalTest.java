package io.harness.functional.graphQLAPIs.artifactSources;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.GraphQLRestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetArtifactConnectionFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private FeatureFlagService featureFlagService;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  private Service service;
  @Before
  public void setUp() {
    owners = ownerManager.create();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, service.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, service.getAccountId());
    }
  }

  @Test
  @Owner(developers = MILAN, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldGetArtifactConnectionWithArtifactSourceFilter() {
    List<Artifact> artifacts = getArtifacts(service, service.getAppId());

    String query = getGraphQLQueryToFetchArtifactConnection(service.getArtifactStreamIds().get(0));
    Map<Object, Object> response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, service.getAccountId(), query);

    assertThat(response).isNotEmpty();

    assertThat(response.get("artifacts")).isNotNull();
    Map<String, Object> artifactsData = (Map<String, Object>) response.get("artifacts");
    assertThat(artifactsData.get("nodes")).isNotNull();
    List<Map<String, Object>> nodesDataList = (List<Map<String, Object>>) artifactsData.get("nodes");
    assertThat(nodesDataList).isNotNull();

    List<String> originalArtifactsIds = artifacts.stream().map(Artifact::getUuid).collect(Collectors.toList());
    List<String> graphqlArtifactIds =
        nodesDataList.stream().map(map -> (String) map.get("id")).collect(Collectors.toList());
    assertThat(originalArtifactsIds).containsAll(graphqlArtifactIds);
  }

  private String getGraphQLQueryToFetchArtifactConnection(String artifactStreamId) {
    return $GQL(/*
query{
  artifacts(filters:[
    {
      artifactSource: {
        operator:EQUALS,
        values: [
          "%s"
        ]
      }
    }
  ],limit: 10, offset: 0) {
    nodes {
      id,
      artifactSource {
        name
      }
    }
  }
}*/ artifactStreamId);
  }

  private List<Artifact> getArtifacts(Service service, String appId) {
    return ArtifactRestUtils.waitAndFetchArtifactListByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0));
  }
}
