package io.harness.gitops.models;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Scanner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ClusterModelTest {
  private final ObjectMapper mapper = new ObjectMapper();
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testModel() throws JsonProcessingException {
    InputStream is = getClass().getClassLoader().getResourceAsStream("cluster/cluster.json");
    String jsonResponse = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    Cluster cluster = mapper.readValue(jsonResponse, Cluster.class);

    assertThat(cluster).isNotNull();
    assertThat(cluster.getIdentifier()).isEqualTo("clusterid");
    assertThat(cluster.name()).isEqualTo("mycluster");
  }
}