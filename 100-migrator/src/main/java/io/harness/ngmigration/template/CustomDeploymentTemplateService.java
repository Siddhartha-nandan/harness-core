/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.executions.steps.StepSpecTypeConstants.DEPLOYMENT_TYPE_CUSTOM_DEPLOYMENT;

import io.harness.cdng.creator.plan.customDeployment.CustomDeploymentInstanceAttributes;
import io.harness.cdng.manifest.yaml.InlineStoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.ngmigration.beans.MigrationContext;
import io.harness.ngmigration.expressions.MigratorExpressionUtils;
import io.harness.plancreator.customDeployment.CustomDeploymentExecutionConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.utils.JsonPipelineUtils;

import software.wings.beans.template.Template;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class CustomDeploymentTemplateService implements NgTemplateService {
  @Override
  public Set<String> getExpressions(Template template) {
    CustomDeploymentTypeTemplate customDeploymentTypeTemplate =
        (CustomDeploymentTypeTemplate) template.getTemplateObject();
    if (StringUtils.isBlank(customDeploymentTypeTemplate.getFetchInstanceScript())) {
      return Collections.emptySet();
    }
    return MigratorExpressionUtils.extractAll(customDeploymentTypeTemplate.getFetchInstanceScript());
  }

  @Override
  public TemplateEntityType getTemplateEntityType() {
    return TemplateEntityType.CUSTOM_DEPLOYMENT_TEMPLATE;
  }

  @Override
  public boolean isMigrationSupported() {
    return true;
  }

  @Override
  public JsonNode getNgTemplateConfigSpec(
      MigrationContext context, Template template, String orgIdentifier, String projectIdentifier) {
    CustomDeploymentTypeTemplate customDeploymentTypeTemplate =
        (CustomDeploymentTypeTemplate) template.getTemplateObject();

    List<Map<String, String>> variables = new ArrayList<>();
    if (isNotEmpty(template.getVariables())) {
      template.getVariables().forEach(variable -> {
        variables.add(ImmutableMap.of("name", valueOrDefaultEmpty(variable.getName()), "type", "String", "value",
            valueOrDefaultRuntime(variable.getValue()), "description", variable.getDescription()));
      });
    }

    StoreConfigWrapper storeConfigWrapper = null;
    if (isNotEmpty(customDeploymentTypeTemplate.getFetchInstanceScript())) {
      storeConfigWrapper =
          StoreConfigWrapper.builder()
              .type(StoreConfigType.INLINE)
              .spec(InlineStoreConfig.builder()
                        .content(ParameterField.createValueField(customDeploymentTypeTemplate.getFetchInstanceScript()))
                        .build())
              .build();
    }

    List<CustomDeploymentInstanceAttributes> attributes = new ArrayList<>();
    attributes.add(
        CustomDeploymentInstanceAttributes.builder().name("instancename").jsonPath("__PLEASE_FIX_ME__").build());
    if (isNotEmpty(customDeploymentTypeTemplate.getHostAttributes())) {
      customDeploymentTypeTemplate.getHostAttributes().forEach(
          (k, v) -> { attributes.add(CustomDeploymentInstanceAttributes.builder().name(k).jsonPath(v).build()); });
    }

    Map<String, Object> infrastructureSpec =
        ImmutableMap.<String, Object>builder()
            .put("variables", variables)
            .put("fetchInstancesScript", ImmutableMap.of("store", storeConfigWrapper))
            .put("instanceAttributes", attributes)
            .put("instancesListPath", customDeploymentTypeTemplate.getHostObjectArrayPath())
            .build();
    return JsonPipelineUtils.asTree(ImmutableMap.of("infrastructure", infrastructureSpec, "execution",
        CustomDeploymentExecutionConfig.builder().stepTemplateRefs(new ArrayList<>()).build()));
  }

  @Override
  public String getNgTemplateStepName(Template template) {
    return DEPLOYMENT_TYPE_CUSTOM_DEPLOYMENT;
  }

  @Override
  public String getTimeoutString(Template template) {
    return "60s";
  }

  static String valueOrDefaultEmpty(String val) {
    return StringUtils.isNotBlank(val) ? val : "";
  }

  static String valueOrDefaultRuntime(String val) {
    return StringUtils.isNotBlank(val) ? val : "<+input>";
  }
}
