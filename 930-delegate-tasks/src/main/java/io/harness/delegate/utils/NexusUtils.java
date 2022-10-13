/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactUtilities;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.task.ssh.artifact.NexusArtifactDelegateConfig;
import io.harness.delegate.task.ssh.artifact.SshWinRmArtifactDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.nexus.NexusRequest;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

@UtilityClass
@Slf4j
@OwnedBy(CDP)
public class NexusUtils {
  private static final String MAVEN_REPOSITORY_FORMAT = "maven";

  public static NexusArtifactDelegateConfig getNexusArtifactDelegateConfig(
      SshWinRmArtifactDelegateConfig artifactDelegateConfig) {
    if (!(artifactDelegateConfig instanceof NexusArtifactDelegateConfig)) {
      log.error(
          "Wrong artifact delegate config submitted. Expecting nexus delegate config, artifactDelegateConfigClass: {}",
          artifactDelegateConfig.getClass());
      throw new InvalidRequestException("Invalid artifact delegate config submitted, expected Nexus config");
    }

    return (NexusArtifactDelegateConfig) artifactDelegateConfig;
  }

  public static String getBasicAuthHeader(NexusRequest nexusRequest) {
    return ArtifactUtilities.getBasicAuthHeader(
        nexusRequest.isHasCredentials(), nexusRequest.getUsername(), nexusRequest.getPassword());
  }

  public static String getNexusArtifactFileName(
      NexusVersion nexusVersion, final String repositoryFormat, final String artifactUrl) {
    if (NexusVersion.NEXUS2 == nexusVersion && MAVEN_REPOSITORY_FORMAT.equals(repositoryFormat)) {
      return buildNexusArtifactFileNameFromMavenArtifactUrl(artifactUrl);
    }
    // NEXUS3, NEXUS2 nuget, NEXUS2 npm
    return ArtifactUtilities.getArtifactName(artifactUrl);
  }

  public static String buildNexusArtifactFileNameFromMavenArtifactUrl(final String mvnArtifactUrl) {
    HttpUrl httpUrl = HttpUrl.parse(mvnArtifactUrl);
    if (httpUrl == null) {
      throw new InvalidRequestException(format("Unable to parse Nexus2 maven artifact url, %s", mvnArtifactUrl));
    }

    String artifactId = httpUrl.queryParameter("a");
    if (isEmpty(artifactId)) {
      throw new InvalidRequestException(
          format("Unable to found artifact Id from Nexus artifact path, artifactUrl: %s", mvnArtifactUrl));
    }

    StringBuilder artifactName = new StringBuilder(artifactId);
    String version = httpUrl.queryParameter("v");
    if (isNotEmpty(version)) {
      artifactName.append('-').append(version);
    }
    String classifier = httpUrl.queryParameter("c");
    if (isNotEmpty(classifier)) {
      artifactName.append('-').append(classifier);
    }
    String extension = httpUrl.queryParameter("e");
    return artifactName.append('.').append(extension).toString();
  }

  public static NexusVersion getNexusVersion(NexusArtifactDelegateConfig nexusArtifactDelegateConfig) {
    NexusConnectorDTO nexusConnectorDTO =
        (NexusConnectorDTO) nexusArtifactDelegateConfig.getConnectorDTO().getConnectorConfig();
    String version = nexusConnectorDTO.getVersion();

    if (isEmpty(version)) {
      throw new InvalidRequestException("Nexus version cannot be null or empty");
    }

    char firstVersionChar = version.charAt(0);
    if (firstVersionChar == '2') {
      return NexusVersion.NEXUS2;
    } else if (firstVersionChar == '3') {
      return NexusVersion.NEXUS3;
    }
    throw new InvalidRequestException(format("Unsupported Nexus version, %s", version));
  }
}
