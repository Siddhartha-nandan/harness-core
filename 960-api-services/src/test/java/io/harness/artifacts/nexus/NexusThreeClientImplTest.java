/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.nexus;

import static io.harness.rule.OwnerRule.ABHISHEK;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.SHIVAM;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.beans.DockerImageManifestResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NexusRegistryException;
import io.harness.nexus.NexusRequest;
import io.harness.nexus.NexusThreeClientImpl;
import io.harness.nexus.NexusThreeRestClient;
import io.harness.rule.Owner;

import software.wings.utils.RepositoryFormat;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Response;

@OwnedBy(HarnessTeam.CDP)
public class NexusThreeClientImplTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(WireMockConfiguration.options().wireMockConfig().port(Options.DYNAMIC_PORT), false);
  @InjectMocks NexusThreeClientImpl nexusThreeService;
  @Mock NexusThreeRestClient nexusThreeRestClient;

  private static String url;
  private static String artifactRepoUrl;
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String VERSION_3 = "3.x";
  private static final String REPO_KEY = "TestRepoKey1";
  private static final String ARTIFACT = "test/artifact";
  private static final String LATEST = "latest";
  private static final String DOCKER_CONTENT_DIGEST = "Docker-Content-Digest";
  private static final String SHA = "sha256:12345";

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    url = "http://localhost:" + wireMockRule.port();
    artifactRepoUrl = "http://localhost:999";
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetRepositories() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version(VERSION_3)
                                   .build();

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("["
                                 + "{\n"
                                 + "        \"name\": \"docker-test1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },"
                                 + "{\n"
                                 + "        \"name\": \"docker-test2\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test2\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }"
                                 + "]")));
    try {
      Map<String, String> response = nexusThreeService.getRepositories(nexusConfig, RepositoryFormat.docker.name());

      assertThat(response).isNotNull();
      assertThat(response).size().isEqualTo(2);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testIsServerValid() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version(VERSION_3)
                                   .build();

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("["
                                 + "{\n"
                                 + "        \"name\": \"docker-test1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },"
                                 + "{\n"
                                 + "        \"name\": \"docker-test2\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test2\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }"
                                 + "]")));
    try {
      boolean response = nexusThreeService.isServerValid(nexusConfig);

      assertThat(response).isNotNull();
      assertThat(response).isEqualTo(true);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories")).willReturn(aResponse().withStatus(404)));
    try {
      assertThatThrownBy(() -> nexusThreeService.isServerValid(nexusConfig))
          .isInstanceOf(HintException.class)
          .getCause()
          .isInstanceOf(ExplanationException.class)
          .getCause()
          .isInstanceOf(InvalidArtifactServerException.class);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetArtifactsVersionsInvalidPort() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version(VERSION_3)
                                   .build();

    assertThatThrownBy(()
                           -> nexusThreeService.getArtifactsVersions(
                               nexusConfig, "todolist", null, "todolist", RepositoryFormat.docker.name()))
        .isInstanceOf(HintException.class)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .getCause()
        .isInstanceOf(NexusRegistryException.class);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetArtifactsVersionsSuccess() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey
                + "&name=" + URLEncoder.encode(artifactPath, "UTF-8") + "&format=docker"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getArtifactsVersions(
        nexusConfig, repoKey, null, artifactPath, RepositoryFormat.docker.name());

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    List<String> dockerPullCommands =
        response.stream().map(bdi -> bdi.getMetadata().get(ArtifactMetadataKeys.IMAGE)).collect(Collectors.toList());

    String fullRepoPath = "localhost:999/" + artifactPath;
    assertThat(dockerPullCommands).contains(fullRepoPath + ":latest2", fullRepoPath + ":a1new");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetBuildDetails() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey2";
    String artifactPath = "test/artifact";
    String tag = "latest2";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey
                + "&name=" + URLEncoder.encode(artifactPath, "UTF-8") + "&format=docker&version=" + tag))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "        ],\n"
                + "\"continuationToken\": null"
                + "}")));

    List<BuildDetailsInternal> response = nexusThreeService.getBuildDetails(
        nexusConfig, repoKey, null, artifactPath, RepositoryFormat.docker.name(), tag);

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(1);
    assertThat(response.get(0).getMetadata().get(ArtifactMetadataKeys.IMAGE))
        .isEqualTo("localhost:999/" + artifactPath + ":" + tag);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetBuildDetailException() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey2";
    String artifactPath = "test/artifact";
    String tag = "latest2";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey
                + "&name=" + URLEncoder.encode(artifactPath, "UTF-8") + "&format=docker&version=" + tag))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "        ],\n"
                + "\"continuationToken\": null"
                + "}")));

    try {
      List<BuildDetailsInternal> response = nexusThreeService.getBuildDetails(
          nexusConfig, repoKey, "8080:", artifactPath, RepositoryFormat.docker.name(), tag);
    } catch (HintException ex) {
      assertThat(ex.getMessage()).isEqualTo("Please check repository port field in your Nexus artifact configuration.");
    }
  }
  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetVersion() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey2";
    String artifactPath = "test/artifact";
    String tag = "latest2";

    wireMockRule.stubFor(get(urlEqualTo("service/rest/v1/search?sort=version&direction=desc"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    try {
      List<BuildDetailsInternal> response =
          nexusThreeService.getVersions(nexusConfig, repoKey, null, artifactPath, null, null, Integer.MAX_VALUE);
    } catch (InvalidArtifactServerException exception) {
      assertThat(exception.getMessage()).isEqualTo("INVALID_ARTIFACT_SERVER");
    }
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetVersionsSuccess() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey
                + "&maven.groupId=groupId&maven.artifactId=" + URLEncoder.encode(artifactPath, "UTF-8")
                + "&maven.extension=war&maven.classifier=ex"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response =
        nexusThreeService.getVersions(nexusConfig, repoKey, "groupId", artifactPath, "war", "ex", Integer.MAX_VALUE);

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("a1new");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("Version# a1new");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetVersionsForClassifierNotFound() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey
                                 + "&maven.groupId=groupId&maven.artifactId=" + URLEncoder.encode(artifactPath, "UTF-8")
                                 + "&maven.extension=war&maven.classifier=ex"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"items\": [],\n"
                                 + "\"continuationToken\": null\n"
                                 + "}")));
    assertThatThrownBy(()
                           -> nexusThreeService.getVersions(
                               nexusConfig, repoKey, "groupId", artifactPath, "war", "ex", Integer.MAX_VALUE))
        .isInstanceOf(HintException.class);
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetVersionsNoCredentials() throws UnsupportedEncodingException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(false)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey
                + "&maven.groupId=groupId&maven.artifactId=" + URLEncoder.encode(artifactPath, "UTF-8")
                + "&maven.extension=war&maven.classifier=ex"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response =
        nexusThreeService.getVersions(nexusConfig, repoKey, "groupId", artifactPath, "war", "ex", Integer.MAX_VALUE);

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("a1new");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("Version# a1new");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }
  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageVersionsSuccess() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey + "&name=package"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getPackageVersions(nexusConfig, repoKey, "package");

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("a1new");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("Version# a1new");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageVersionsNoCredentials() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(false)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey + "&name=package"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getPackageVersions(nexusConfig, repoKey, "package");

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("a1new");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("Version# a1new");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageVersionsNotFound() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey + "&name=package1234"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    try {
      nexusThreeService.getPackageVersions(nexusConfig, repoKey, "package");
    } catch (InvalidArtifactServerException exception) {
      assertThat(exception.getMessage()).isEqualTo("INVALID_ARTIFACT_SERVER");
    }
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageNameBuildDetails() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey + "&group=group"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getPackageNamesBuildDetails(nexusConfig, repoKey, "group");

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("test/artifact");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("test/artifact");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageNameBuildDetailsNoCredentials() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(false)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?sort=version&direction=desc&repository=" + repoKey + "&group=group"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getPackageNamesBuildDetails(nexusConfig, repoKey, "group");

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    assertThat(response.get(0).getNumber()).isEqualTo("test/artifact");
    assertThat(response.get(0).getUiDisplayName()).isEqualTo("test/artifact");
    assertThat(response.get(0).getBuildUrl())
        .isEqualTo("https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testGetPackageNameBuildDetailsExceptions() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    String repoKey = "TestRepoKey1";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey + "&group=group"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    try {
      nexusThreeService.getPackageNamesBuildDetails(nexusConfig, repoKey, "group");
    } catch (InvalidArtifactServerException exception) {
      assertThat(exception.getMessage()).isEqualTo("INVALID_ARTIFACT_SERVER");
    }
  }

  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void testFetchImageManifest() throws IOException {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username(USERNAME)
                                   .password(PASSWORD.toCharArray())
                                   .hasCredentials(false)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version(VERSION_3)
                                   .build();

    wireMockRule.stubFor(
        get(urlEqualTo("/repository/TestRepoKey1/v2/test/artifact/manifests/latest"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(DOCKER_CONTENT_DIGEST, SHA)
                    .withBody("{\n"
                        + "    \"name\": \"projects/cd-play/locations/us-south1/repositories/vivek-repo/packages/mongo/tags/latest10\",\n"
                        + "    \"version\": \"projects/cd-play/locations/us-south1/repositories/vivek-repo/packages/mongo/versions/sha256:38cd16441be083f00bf2c3e0e307292531b6d98eb77c09271cf43f2b58ce9f9e\"\n"
                        + "}")));

    Response<DockerImageManifestResponse> response =
        nexusThreeService.fetchImageManifest(nexusConfig, REPO_KEY, ARTIFACT, true, LATEST);

    assertThat(response).isNotNull();
    assertThat(response.code()).isEqualTo(200);
    assertThat(response.headers().get(DOCKER_CONTENT_DIGEST)).isEqualTo(SHA);
  }
}
