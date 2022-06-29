/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.artifact.outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.ArtifactSummary;
import io.harness.cdng.artifact.EcrArtifactSummary;
import io.harness.cdng.artifact.S3ArtifactSummary;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@TypeAlias("ecrArtifactOutcome")
@JsonTypeName("ecrArtifactOutcome")
@OwnedBy(CDC)
@RecasterAlias("io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome")
public class S3ArtifactOutcome implements ArtifactOutcome {
  /** Docker hub registry connector. */
  String connectorRef;
  /** Images in repos need to be referenced via a path. */
  String bucketName;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String filePath;
  /** region */
  String region;
  /** Identifier for artifact. */
  String identifier;
  /** Artifact type. */
  String type;
  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;
  /** registryHostName/imagePath:tag */
  String image;
  /** imagePullSecret for Ecr credentials base encoded.*/
  String imagePullSecret;

  @Override
  public ArtifactSummary getArtifactSummary() {
    return S3ArtifactSummary.builder().bucketName(bucketName).filePath(filePath).build();
  }

  @Override
  public String getArtifactType() {
    return type;
  }
}
