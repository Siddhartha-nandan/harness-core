/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps.stepinfo.security.shared;

import static io.harness.annotations.dev.HarnessTeam.STO;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.sto.variables.STOYamlTargetType;
import io.harness.yaml.sto.variables.STOYamlTargetNamingMode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(STO)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class STOYamlTarget {
  @NotNull
  @YamlSchemaTypes(value = {runtime})
  @ApiModelProperty(dataType = "io.harness.yaml.sto.variables.STOYamlTargetType")
  protected STOYamlTargetType type;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) protected ParameterField<String> name;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) protected ParameterField<String> variant;

  @NotNull
  @ApiModelProperty(dataType = "io.harness.yaml.sto.variables.STOYamlTargetNamingMode")
  protected ParameterField<STOYamlTargetNamingMode> namingMode;

  @ApiModelProperty(dataType = STRING_CLASSPATH) protected ParameterField<String> workspace;

  public STOYamlTargetNamingMode getNamingMode() {
    if (namingMode == null) {
      return null;
    }
    if (namingMode.fetchFinalValue() instanceof String) {
      String mode = (String) namingMode.fetchFinalValue();
      return STOYamlTargetNamingMode.getValue(mode);
    } else {
      return (STOYamlTargetNamingMode) namingMode.fetchFinalValue();
    }
  }
}
