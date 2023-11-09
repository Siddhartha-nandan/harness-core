/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.ami;

import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class AMIArtifactDelegateResponse extends ArtifactDelegateResponse {
  /**
   * Exact Version of the artifact
   */
  String version;

  /**
   * Version Regex
   */
  String versionRegex;

  /**
   * AMI ID
   */
  String amiId;

  /**
   * Version Regex
   */
  Map<String, String> metadata;

  @Builder
  public AMIArtifactDelegateResponse(ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType, String version,
      String versionRegex, String amiId, Map<String, String> metadata, String artifactUrl) {
    super(buildDetails, sourceType);

    this.version = version;
    this.versionRegex = versionRegex;
    this.amiId = amiId;
    this.metadata = metadata;
  }

  @Override
  public String describe() {
    String metadataKeys = (getMetadata() != null) ? String.valueOf(getMetadata().keySet()) : null;

    return "type: " + (getSourceType() != null ? getSourceType().getDisplayName() : null) + "\nversion: " + getVersion()
        + "\nAmiId: " + getAmiId() + "\nMetadata keys: " + metadataKeys;
  }
}
