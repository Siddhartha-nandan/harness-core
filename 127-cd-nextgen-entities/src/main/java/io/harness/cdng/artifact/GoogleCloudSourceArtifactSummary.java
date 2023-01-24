package io.harness.cdng.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@JsonTypeName(ArtifactSourceConstants.GOOGLE_CLOUD_SOURCE_ARTIFACT_NAME)
@RecasterAlias("io.harness.ngpipeline.pipeline.executions.beans.GoogleCloudSourceArtifactSummary")
public class GoogleCloudSourceArtifactSummary implements ArtifactSummary {
  String repository;
  String sourceDirectory;

  @Override
  public String getDisplayName() {
    return repository + ":" + sourceDirectory;
  }

  @Override
  public String getType() {
    return ArtifactSourceConstants.GOOGLE_CLOUD_SOURCE_ARTIFACT_NAME;
  }
}
