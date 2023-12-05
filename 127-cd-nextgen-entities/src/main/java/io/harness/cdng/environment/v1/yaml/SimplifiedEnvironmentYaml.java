/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.environment.v1.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.yaml.YAMLFieldNameConstants.INFRA_DEFS;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.expressionEmptyStringAllowed;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtimeEmptyStringAllowed;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.environment.filters.FilterYaml;
import io.harness.cdng.environment.yaml.EnvironmentInfraUseFromStage;
import io.harness.cdng.environment.yaml.ServiceOverrideInputsYaml;
import io.harness.cdng.gitops.yaml.ClusterYaml;
import io.harness.cdng.infra.v1.yaml.SimplifiedInfraDefYaml;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.core.VariableExpression;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@Data
@Builder
@TypeAlias("simplifiedEnvironmentYaml")
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.environment.v1.yaml.SimplifiedEnvironmentYaml")
public class SimplifiedEnvironmentYaml implements Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @Pattern(regexp = NGRegexValidatorConstants.NON_EMPTY_STRING_PATTERN)
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  private ParameterField<String> ref;

  @JsonProperty(INFRA_DEFS)
  @ApiModelProperty(dataType = SwaggerConstants.INFRASTRUCTURE_DEFINITION_YAML_NODE_LIST_CLASSPATH_V1)
  @YamlSchemaTypes({expressionEmptyStringAllowed})
  ParameterField<List<SimplifiedInfraDefYaml>> infrastructureDefinitions;

  // environmentInputs
  @ApiModelProperty(dataType = SwaggerConstants.JSON_NODE_CLASSPATH)
  @YamlSchemaTypes({runtimeEmptyStringAllowed})
  ParameterField<Map<String, Object>> inputs;

  // Above are fields onboarded for yaml simplification
  // TODO : Add visitor helper

  /*
  Deploy to all underlying infrastructures (or gitops clusters)
   */
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  @YamlSchemaTypes({runtime})
  ParameterField<Boolean> deployToAll;

  private String gitBranch;

  @ApiModelProperty(dataType = SwaggerConstants.FILTER_YAML_LIST_CLASSPATH)
  @YamlSchemaTypes(runtime)
  ParameterField<List<FilterYaml>> filters;

  @Nullable @VariableExpression(skipVariableExpression = true) ExecutionElementConfig provisioner;

  @VariableExpression(skipVariableExpression = true) private EnvironmentInfraUseFromStage useFromStage;

  @ApiModelProperty(dataType = SwaggerConstants.JSON_NODE_CLASSPATH)
  @YamlSchemaTypes({runtimeEmptyStringAllowed})
  ParameterField<Map<String, Object>> serviceOverrideInputs;

  @ApiModelProperty(dataType = SwaggerConstants.CLUSTER_YAML_NODE_LIST_CLASSPATH)
  @YamlSchemaTypes({runtime})
  ParameterField<List<ClusterYaml>> gitOpsClusters;

  List<ServiceOverrideInputsYaml> servicesOverrides;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  public ParameterField<Boolean> getDeployToAll() {
    // default to false
    if (deployToAll == null) {
      return ParameterField.createValueField(false);
    }
    return !deployToAll.isExpression() && deployToAll.getValue() == null ? ParameterField.createValueField(false)
                                                                         : deployToAll;
  }

  public SimplifiedEnvironmentYaml clone() {
    ParameterField<List<FilterYaml>> filtersCloned = null;
    if (ParameterField.isNotNull(this.filters) && this.filters.getValue() != null) {
      filtersCloned = ParameterField.createValueField(
          this.filters.getValue().stream().map(FilterYaml::clone).collect(Collectors.toList()));
    }
    return SimplifiedEnvironmentYaml.builder()
        .inputs(this.inputs)
        .ref(this.ref)
        .deployToAll(this.deployToAll)
        .filters(filtersCloned)
        .gitOpsClusters(this.gitOpsClusters)
        .infrastructureDefinitions(this.infrastructureDefinitions)
        .provisioner(this.provisioner)
        .serviceOverrideInputs(this.serviceOverrideInputs)
        .build();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (ParameterField.isNotNull(infrastructureDefinitions) && infrastructureDefinitions.getValue() != null) {
      infrastructureDefinitions.getValue().forEach(id -> children.add("infrastructureDefinitions", id));
    }
    return children;
  }
}
