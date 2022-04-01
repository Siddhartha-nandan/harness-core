/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.nexus.NexusHelper.isRequestSuccessful;
import static io.harness.nexus.NexusHelper.isSuccessful;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifact.ArtifactUtilities;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.artifact.ArtifactFileMetadataInternal;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.NexusRegistryException;
import io.harness.exception.WingsException;
import io.harness.nexus.model.Asset;
import io.harness.nexus.model.DockerImageResponse;
import io.harness.nexus.model.Nexus3ComponentResponse;
import io.harness.nexus.model.Nexus3Repository;

import software.wings.utils.RepositoryFormat;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class NexusThreeClientImpl {
  private static final List<String> IGNORE_EXTENSIONS = Lists.newArrayList("pom", "sha1", "sha256", "sha512", "md5");
  private static final String REPO_PORT_REGEX = "^[\\d]+$";

  public Map<String, String> getRepositories(NexusRequest nexusConfig, String repositoryFormat) throws IOException {
    log.info("Retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.isHasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }

    if (isSuccessful(response)) {
      if (isNotEmpty(response.body())) {
        log.info("Retrieving {} repositories success", repositoryFormat);
        final Map<String, String> repositories;
        if (repositoryFormat == null) {
          repositories =
              response.body().stream().collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        } else {
          final String filterBy = repositoryFormat.equals(RepositoryFormat.maven.name()) ? "maven2" : repositoryFormat;
          repositories = response.body()
                             .stream()
                             .filter(o -> o.getFormat().equals(filterBy))
                             .collect(Collectors.toMap(Nexus3Repository::getName, Nexus3Repository::getName));
        }
        log.info("Retrieved repositories are {}", repositories.values());
        return repositories;
      } else {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the connector details - URL & credentials are correct",
            "No repositories were found for the connector",
            new InvalidArtifactServerException("Failed to fetch the repositories", WingsException.USER));
      }
    }
    log.info("No repositories found returning empty map");
    return emptyMap();
  }

  public List<String> getDockerImages(NexusRequest nexusConfig, String repository) throws IOException {
    String repositoryKey = ArtifactUtilities.trimSlashforwardChars(repository);
    log.info("Retrieving docker images for repository {} from url {}", repositoryKey, nexusConfig.getNexusUrl());
    List<String> images = new ArrayList<>();
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    Response<DockerImageResponse> response;
    if (nexusConfig.isHasCredentials()) {
      response =
          nexusThreeRestClient
              .getDockerImages(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repositoryKey)
              .execute();
    } else {
      response = nexusThreeRestClient.getDockerImages(repositoryKey).execute();
    }
    if (isSuccessful(response)) {
      if (response.body() != null && response.body().getRepositories() != null) {
        images.addAll(response.body().getRepositories().stream().collect(toList()));
        log.info("Retrieving docker images for repository {} from url {} success. Images are {}", repositoryKey,
            nexusConfig.getNexusUrl(), images);
      } else {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the connector details - URL & credentials are correct", "No images were found for the connector",
            new InvalidArtifactServerException("Failed to fetch the images", WingsException.USER));
      }
    } else {
      log.warn("Failed to fetch the docker images as request is not success");
      throw new InvalidArtifactServerException("Failed to fetch the docker images", WingsException.USER);
    }
    log.info("No images found for repository {}", repositoryKey);
    return images;
  }

  public NexusThreeRestClient getNexusThreeClient(NexusRequest nexusConfig) {
    return NexusHelper.getRetrofit(nexusConfig, JacksonConverterFactory.create()).create(NexusThreeRestClient.class);
  }

  public NexusThreeRestClient getNexusThreeClient(NexusRequest nexusConfig, String artifactoDownloadUrl) {
    return NexusHelper
        .getRetrofit(artifactoDownloadUrl, nexusConfig.isCertValidationRequired(), JacksonConverterFactory.create())
        .create(NexusThreeRestClient.class);
  }

  public boolean isServerValid(NexusRequest nexusConfig) throws IOException {
    log.info("Validate if nexus is running by retrieving repositories");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    Response<List<Nexus3Repository>> response;
    if (nexusConfig.isHasCredentials()) {
      response =
          nexusThreeRestClient
              .listRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())))
              .execute();
    } else {
      response = nexusThreeRestClient.listRepositories().execute();
    }
    if (response == null) {
      return false;
    }

    if (response.code() == 404) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the Nexus URL & Nexus version are correct. Nexus URLs are different for different Nexus versions",
          "The Nexus URL or the version for the connector is incorrect",
          new InvalidArtifactServerException("Invalid Nexus connector details"));
    }
    return NexusHelper.isSuccessful(response);
  }

  public List<BuildDetailsInternal> getArtifactsVersions(
      NexusRequest nexusConfig, String repository, String port, String artifactName, String repositoryFormat) {
    if (isNotEmpty(port) && !port.matches(REPO_PORT_REGEX)) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check repository port field for Nexus artifact configuration.",
          String.format("Repository port [%s] field must only contain numeric characters.", port),
          new NexusRegistryException("Invalid Nexus artifact configuration details"));
    }
    String repositoryKey = ArtifactUtilities.trimSlashforwardChars(repository);
    String artifactPath = ArtifactUtilities.trimSlashforwardChars(artifactName);

    try {
      Map<String, String> repos = getRepositories(nexusConfig, repositoryFormat);
      if (EmptyPredicate.isEmpty(repos.get(repositoryKey))) {
        throw new NexusRegistryException("Repository was not found");
      }
    } catch (IOException | NexusRegistryException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Nexus artifact configuration and verify that repository is valid.",
          String.format("Failed to retrieve repository '%s'", repositoryKey),
          new NexusRegistryException(e.getMessage()));
    }

    try {
      List<String> images = getDockerImages(nexusConfig, repositoryKey);
      if (images.stream().noneMatch(img -> img.equalsIgnoreCase(artifactPath))) {
        throw new NexusRegistryException("Artifact was not found");
      }
    } catch (IOException | NexusRegistryException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Nexus artifact configuration and verify that artifact path is valid.",
          String.format("Failed to retrieve image artifact '%s'", artifactPath),
          new NexusRegistryException(e.getMessage()));
    }

    log.info("Retrieving artifact versions(tags)");
    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    final Call<Nexus3ComponentResponse> request;
    if (nexusConfig.isHasCredentials()) {
      request = nexusThreeRestClient.search(
          Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repositoryKey,
          artifactPath, repositoryFormat, null);
    } else {
      request = nexusThreeRestClient.search(repositoryKey, artifactPath, repositoryFormat, null);
    }

    Response<Nexus3ComponentResponse> response = executeRequest(request);
    List<BuildDetailsInternal> result = processComponentResponse(
        request, response, nexusConfig, repositoryKey, port, artifactPath, repositoryFormat, null);

    if (isEmpty(result)) {
      throw NestedExceptionUtils.hintWithExplanationException("Please check your artifact configuration.",
          String.format("Failed to retrieve artifact tags by API call '%s %s' and got response code '%s'",
              request.request().method(), request.request().url(), response.code()),
          new NexusRegistryException(
              String.format("No tags found for artifact [repositoryFormat=%s, repository=%s, artifact=%s].",
                  repositoryFormat, repositoryKey, artifactPath)));
    }

    return result;
  }

  private List<ArtifactFileMetadataInternal> getArtifactMetadata(List<Asset> assets, String repoId) {
    List<ArtifactFileMetadataInternal> artifactFileMetadataInternals = new ArrayList<>();
    if (isEmpty(assets)) {
      return artifactFileMetadataInternals;
    }
    for (Asset item : assets) {
      String url = item.getDownloadUrl();
      String artifactFileName = url.substring(url.lastIndexOf('/') + 1);
      String artifactPath = item.getPath();
      if (IGNORE_EXTENSIONS.stream().anyMatch(artifactFileName::endsWith)) {
        continue;
      }
      if (!item.getRepository().equals(repoId)) {
        url = url.replace(item.getRepository(), repoId);
      }
      artifactFileMetadataInternals.add(
          ArtifactFileMetadataInternal.builder().fileName(artifactFileName).imagePath(artifactPath).url(url).build());
    }
    return artifactFileMetadataInternals;
  }

  private String getArtifactDownloadUrl(
      List<ArtifactFileMetadataInternal> artifactFileMetadataInternals, String extension, String classifier) {
    String defaultUrl = artifactFileMetadataInternals.get(0).getUrl();
    String url = null;
    if (StringUtils.isNoneBlank(extension, classifier)) {
      url = artifactFileMetadataInternals.stream()
                .filter(meta -> meta.getFileName().endsWith(extension) && meta.getFileName().contains(classifier))
                .map(ArtifactFileMetadataInternal::getUrl)
                .findFirst()
                .orElse(null);
    }
    return StringUtils.isNotBlank(url) ? url : defaultUrl;
  }

  private String getArtifactImagePath(
      List<ArtifactFileMetadataInternal> artifactFileMetadataInternals, String extension, String classifier) {
    String defaultArtifactPath = artifactFileMetadataInternals.get(0).getImagePath();
    String artifactPath = null;
    if (StringUtils.isNoneBlank(extension, classifier)) {
      artifactPath =
          artifactFileMetadataInternals.stream()
              .filter(meta -> meta.getFileName().endsWith(extension) && meta.getFileName().contains(classifier))
              .map(ArtifactFileMetadataInternal::getImagePath)
              .findFirst()
              .orElse(null);
    }
    return StringUtils.isNotBlank(artifactPath) ? artifactPath : defaultArtifactPath;
  }

  public List<BuildDetailsInternal> getBuildDetails(NexusRequest nexusConfig, String repository, String port,
      String artifactName, String repositoryFormat, String tag) {
    String repositoryKey = ArtifactUtilities.trimSlashforwardChars(repository);
    String artifactPath = ArtifactUtilities.trimSlashforwardChars(artifactName);

    if (isNotEmpty(port) && !port.matches(REPO_PORT_REGEX)) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check repository port field in your Nexus artifact configuration.",
          String.format("Repository port [%s] field must only contain numeric characters.", port),
          new NexusRegistryException(String.format(
              "Repository port has an invalid value.", repositoryFormat, repositoryKey, artifactPath, tag)));
    }
    log.info("Retrieving artifact details");

    try {
      Map<String, String> repos = getRepositories(nexusConfig, repositoryFormat);
      if (EmptyPredicate.isEmpty(repos.get(repositoryKey))) {
        throw new NexusRegistryException("Repository was not found");
      }
    } catch (IOException | NexusRegistryException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Nexus artifact configuration and verify that repository is valid.",
          String.format("Failed to retrieve repository '%s'", repositoryKey),
          new NexusRegistryException(e.getMessage()));
    }

    try {
      List<String> images = getDockerImages(nexusConfig, repositoryKey);
      if (images.stream().noneMatch(img -> img.equalsIgnoreCase(artifactPath))) {
        throw new NexusRegistryException("Artifact was not found");
      }
    } catch (IOException | NexusRegistryException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Nexus artifact configuration and verify that artifact path is valid.",
          String.format("Failed to retrieve image artifact '%s'", artifactPath),
          new NexusRegistryException(e.getMessage()));
    }

    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig);
    final Call<Nexus3ComponentResponse> request;
    if (nexusConfig.isHasCredentials()) {
      request = nexusThreeRestClient.getArtifact(
          Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repositoryKey,
          artifactPath, repositoryFormat, tag, null);
    } else {
      request = nexusThreeRestClient.getArtifact(repositoryKey, artifactPath, repositoryFormat, tag, null);
    }

    Response<Nexus3ComponentResponse> response = executeRequest(request);
    List<BuildDetailsInternal> result = processComponentResponse(
        request, response, nexusConfig, repositoryKey, port, artifactPath, repositoryFormat, tag);

    if (isEmpty(result)) {
      throw NestedExceptionUtils.hintWithExplanationException("Please check your artifact configuration.",
          String.format("Failed to retrieve artifact metadata with API call '%s %s' and got response code '%s'",
              request.request().method(), request.request().url(), response.code()),
          new NexusRegistryException(
              String.format("Artifact [repositoryFormat=%s, repository=%s, artifact=%s, tag=%s] was not found.",
                  repositoryFormat, repositoryKey, artifactPath, tag)));
    }

    return result;
  }

  private Response executeRequest(Call request) {
    try {
      return request.execute();
    } catch (IOException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Nexus registry might not be online. Please check Nexus connector configuration and verify that URL is valid.",
          String.format("Failed to execute API call '%s %s'", request.request().method(), request.request().url()),
          new NexusRegistryException(e.getMessage()));
    }
  }

  private List<BuildDetailsInternal> processComponentResponse(Call request, Response<Nexus3ComponentResponse> response,
      NexusRequest nexusConfig, String repository, String port, String artifactName, String repositoryFormat,
      String tag) {
    List<BuildDetailsInternal> components = new ArrayList<>();
    if (isRequestSuccessful(request, response)) {
      if (isNotEmpty(response.body().getItems())) {
        for (Nexus3ComponentResponse.Component component : response.body().getItems()) {
          List<ArtifactFileMetadataInternal> artifactFileMetadataInternals =
              getArtifactMetadata(component.getAssets(), repository);
          String versionDownloadUrl = null;
          String artifactPath = null;
          String actualTag = isEmpty(tag) ? component.getVersion() : tag;
          if (isNotEmpty(artifactFileMetadataInternals)) {
            versionDownloadUrl = getArtifactDownloadUrl(artifactFileMetadataInternals, null, null);
            artifactPath = getArtifactImagePath(artifactFileMetadataInternals, null, null);
          }

          String repoName = ArtifactUtilities.getNexusRepositoryNameNG(
              nexusConfig.getNexusUrl(), port, nexusConfig.getArtifactRepositoryUrl(), artifactName);
          log.info("Retrieving docker tags for repository {} imageName {} ", repository, artifactName);
          Map<String, String> metadata = new HashMap<>();
          metadata.put(ArtifactMetadataKeys.IMAGE, repoName + ":" + actualTag);
          metadata.put(ArtifactMetadataKeys.TAG, actualTag);

          BuildDetailsInternal buildDetailsInternal = BuildDetailsInternal.builder()
                                                          .number(component.getVersion())
                                                          .metadata(metadata)
                                                          .buildUrl(versionDownloadUrl)
                                                          .artifactPath(artifactPath)
                                                          .build();

          components.add(buildDetailsInternal);
        }
      }
    }
    return components;
  }
}
