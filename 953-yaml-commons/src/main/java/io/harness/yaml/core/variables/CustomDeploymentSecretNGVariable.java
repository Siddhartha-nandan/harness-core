package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.core.VariableExpression.IteratePolicy.REGULAR_WITH_CUSTOM_FIELD;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.encryption.SecretRefData;
import io.harness.pms.yaml.ParameterField;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.validator.NGVariableName;
import io.harness.yaml.core.VariableExpression;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(CustomDeploymentNGVariableConstants.SECRET_TYPE)
@TypeAlias("io.harness.yaml.core.variables.CustomDeploymentSecretNGVariable")
@OwnedBy(CDP)
public class CustomDeploymentSecretNGVariable implements CustomDeploymentNGVariable {
  @NGVariableName
  @Pattern(regexp = NGRegexValidatorConstants.IDENTIFIER_PATTERN)
  @VariableExpression(skipVariableExpression = true)
  String name;
  @ApiModelProperty(allowableValues = CustomDeploymentNGVariableConstants.SECRET_TYPE)
  @VariableExpression(skipVariableExpression = true)
  CustomDeploymentNGVariableType type = CustomDeploymentNGVariableType.SECRET;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @VariableExpression(policy = REGULAR_WITH_CUSTOM_FIELD, skipInnerObjectTraversal = true)
  ParameterField<SecretRefData> value;
  @VariableExpression(skipVariableExpression = true) String description;
  @VariableExpression(skipVariableExpression = true) boolean required;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;
  @Override
  public ParameterField<?> getCurrentValue() {
    return value;
  }
}
