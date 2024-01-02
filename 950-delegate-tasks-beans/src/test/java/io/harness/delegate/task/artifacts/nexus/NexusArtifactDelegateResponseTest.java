/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.nexus;

import static io.harness.rule.OwnerRule.RAKSHIT_AGARWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class NexusArtifactDelegateResponseTest {
  public final String TAG = "tag";
  public final String ARTIFACT_PATH = "artifactPath";
  public final String REPOSITORY_NAME = "repositoryName";

  @InjectMocks NexusArtifactDelegateResponse nexusArtifactDelegateResponse;
  @Before
  public void setup() throws Exception {}

  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestDescribe_Docker() {
    ArtifactBuildDetailsNG buildDetailsNG = ArtifactBuildDetailsNG.builder().build();
    nexusArtifactDelegateResponse = new NexusArtifactDelegateResponse(buildDetailsNG,
        ArtifactSourceType.NEXUS3_REGISTRY, REPOSITORY_NAME, ARTIFACT_PATH, "docker", TAG, new HashMap<>());
    String resp = nexusArtifactDelegateResponse.describe();
    String[] values = resp.split("\n");
    assertThat(resp).isNotNull();
    assertThat(values[0]).contains("type:");
    assertThat(values[1]).contains("repository:");
    assertThat(values[2]).contains("imagePath:");
    assertThat(values[3]).contains("tag:");
    assertThat(values[4]).contains("repository type:");
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestDescribe_Maven() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", "value1");
    metadata.put("key2", "value2");

    ArtifactBuildDetailsNG buildDetailsNG = ArtifactBuildDetailsNG.builder().metadata(metadata).build();

    nexusArtifactDelegateResponse = new NexusArtifactDelegateResponse(buildDetailsNG,
        ArtifactSourceType.NEXUS3_REGISTRY, REPOSITORY_NAME, ARTIFACT_PATH, "maven", TAG, new HashMap<>());
    String resp = nexusArtifactDelegateResponse.describe();
    String[] values = resp.split("\n");
    assertThat(resp).isNotNull();
    assertThat(values[0]).contains("type:");
    assertThat(values[1]).contains("repository:");
    assertThat(values[2]).contains("GroupId:");
    assertThat(values[3]).contains("ArtifactPath:");
    assertThat(values[4]).contains("tag:");
    assertThat(values[5]).contains("repository type:");
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestDescribe_Npm() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", "value1");
    metadata.put("key2", "value2");

    ArtifactBuildDetailsNG buildDetailsNG = ArtifactBuildDetailsNG.builder().metadata(metadata).build();
    nexusArtifactDelegateResponse = new NexusArtifactDelegateResponse(buildDetailsNG,
        ArtifactSourceType.NEXUS3_REGISTRY, REPOSITORY_NAME, ARTIFACT_PATH, "npm", TAG, new HashMap<>());
    String resp = nexusArtifactDelegateResponse.describe();
    String[] values = resp.split("\n");
    assertThat(resp).isNotNull();
    assertThat(values[0]).contains("type:");
    assertThat(values[1]).contains("repository:");
    assertThat(values[2]).contains("packageName:");
    assertThat(values[3]).contains("tag:");
    assertThat(values[4]).contains("repository type:");
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestDescribe_Raw() {
    ArtifactBuildDetailsNG buildDetailsNG = ArtifactBuildDetailsNG.builder().build();
    nexusArtifactDelegateResponse = new NexusArtifactDelegateResponse(buildDetailsNG,
        ArtifactSourceType.NEXUS3_REGISTRY, REPOSITORY_NAME, ARTIFACT_PATH, "raw", TAG, new HashMap<>());
    String resp = nexusArtifactDelegateResponse.describe();
    String[] values = resp.split("\n");
    assertThat(resp).isNotNull();
    assertThat(values[0]).contains("type:");
    assertThat(values[1]).contains("repository:");
    assertThat(values[2]).contains("tag:");
    assertThat(values[3]).contains("repository type:");
  }
}
