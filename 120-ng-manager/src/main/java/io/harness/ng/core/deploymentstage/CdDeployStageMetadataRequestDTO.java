/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.deploymentstage;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@OwnedBy(HarnessTeam.CDC)
@Schema(
    name = "CdDeployStageMetadataRequestDTO", description = "Deploy Stage Metadata Request details defined in Harness.")
public class CdDeployStageMetadataRequestDTO {
  @Schema(description = "Stage Identifier") @NotNull String stageIdentifier;
  @Schema(description = "Pipeline yaml string to be parsed") @NotNull String pipelineYaml;
}
