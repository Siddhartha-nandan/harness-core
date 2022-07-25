/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml.kinds.kustomize;

import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;

import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("OptimizeManifestFetch")
@Schema(name = "OptimizeManifestFetch", description = "This contains Kustomize Manifest Optimize File Fetch details")
public class OptimizeManifestFetchDTO {
  @NotNull
  @ApiModelProperty(dataType = STRING_CLASSPATH)
  @SkipAutoEvaluation
  @Wither
  private ParameterField<String> kustomizeYamlPath;

  @Builder
  public OptimizeManifestFetchDTO(ParameterField<String> kustomizeYamlPath) {
    this.kustomizeYamlPath = kustomizeYamlPath;
  }
}
