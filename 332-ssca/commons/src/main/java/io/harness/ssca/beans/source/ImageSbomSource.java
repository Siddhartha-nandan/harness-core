/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.source;

import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;

import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageSbomSource implements SbomSourceSpec {
  @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> connector;

  @ApiModelProperty(dataType = STRING_CLASSPATH) @NotEmpty ParameterField<String> image;
}
