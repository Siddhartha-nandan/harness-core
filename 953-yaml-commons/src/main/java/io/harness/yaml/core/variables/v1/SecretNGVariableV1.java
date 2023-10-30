/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.core.variables.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.common.NGExpressionUtils;
import io.harness.encryption.SecretRefData;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.yaml.core.variables.NGVariableV1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGVariableConstantsV1.SECRET_TYPE)
@OwnedBy(PIPELINE)
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
public class SecretNGVariableV1 implements NGVariableV1 {
  NGVariableTypeV1 type = NGVariableTypeV1.SECRET;
  boolean execution_input;
  ParameterField<SecretRefData> value;
  String desc;
  boolean required;
  @JsonProperty("default") SecretRefData defaultValue;
  @JsonProperty(YamlNode.UUID_FIELD_NAME) String uuid;

  @JsonIgnore
  @Override
  public ParameterField<?> getCurrentValue() {
    return ParameterField.isNull(value)
            || (value.isExpression() && NGExpressionUtils.matchesInputSetPattern(value.getExpressionValue()))
            || (!value.isExpression() && (value.getValue() == null || value.getValue().isNull()))
        ? ParameterField.createValueField(defaultValue)
        : value;
  }
  @JsonIgnore
  @Override
  public ParameterField<?> fetchValue() {
    return value;
  }
}
