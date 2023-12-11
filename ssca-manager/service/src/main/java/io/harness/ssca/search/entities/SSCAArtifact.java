/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.search.entities;

import io.harness.ssca.search.beans.RelationshipType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSCAArtifact {
  @Id String id;
  String artifactId;
  String orchestrationId;
  String artifactCorrelationId;
  @NotEmpty String url;
  String name;
  String type;
  String tag;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineExecutionId;
  String pipelineIdentifier;
  String stageIdentifier;
  String sequenceIdentifier;
  String stepIdentifier;
  String sbomName;
  Long createdOn;
  boolean isAttested;
  String attestedFileUrl;
  SSCAArtifact.Sbom sbom;
  Boolean invalid;
  Long lastUpdatedAt;
  Long componentsCount;
  Long prodEnvCount;
  Long nonProdEnvCount;

  SSCAArtifact.Scorecard scorecard;

  @Value
  @Builder
  public static class Sbom {
    String tool;
    String toolVersion;
    String sbomFormat;
    String sbomVersion;
  }

  @Value
  @Builder
  public static class Scorecard {
    String avgScore;
    String maxScore;
  }

  RelationshipType relation_type;
}
