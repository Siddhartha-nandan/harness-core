/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.outcome;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.ArtifactSummary;
import io.harness.cdng.artifact.BambooArtifactSummary;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@TypeAlias("BambooArtifactOutcome")
@JsonTypeName("bambooArtifactOutcome")
@OwnedBy(CDC)
@RecasterAlias("io.harness.ngpipeline.artifact.bean.BambooArtifactOutcome")
public class BambooArtifactOutcome implements ArtifactOutcome {
  /** Jenkins connector. */
  String connectorRef;

  /** jobName */
  String planKey;

  /** artifactPath */
  List<String> artifactPath;

  /** Build */
  String build;

  /** Identifier for artifact. */
  String identifier;

  /** Artifact type. */
  String type;

  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;

  /** Jenkins Artifact Metadata.*/
  Map<String, String> metadata;

  @Override
  public ArtifactSummary getArtifactSummary() {
    return BambooArtifactSummary.builder().planKey(planKey).build(build).build();
  }

  @Override
  public String getArtifactType() {
    return type;
  }

  @Override
  public String getTag() {
    return build;
  }
}
