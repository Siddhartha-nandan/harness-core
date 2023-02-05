/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.CUSTOM_ARTIFACT_NAME;
import static io.harness.ngmigration.utils.NGMigrationConstants.PLEASE_FIX_ME;

import io.harness.cdng.artifact.bean.yaml.CustomArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScriptInfo;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScriptSourceWrapper;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomArtifactScripts;
import io.harness.cdng.artifact.bean.yaml.customartifact.CustomScriptInlineSource;
import io.harness.cdng.artifact.bean.yaml.customartifact.FetchAllArtifacts;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.ngmigration.expressions.MigratorExpressionUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.shellscript.ShellType;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.utils.JsonPipelineUtils;

import software.wings.beans.template.Template;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.artifactsource.CustomArtifactSourceTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class CustomArtifactSourceTemplateService implements NgTemplateService {
  @Override
  public Set<String> getExpressions(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    CustomArtifactSourceTemplate customArtifactSourceTemplate =
        (CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource();
    if (StringUtils.isBlank(customArtifactSourceTemplate.getScript())) {
      return Collections.emptySet();
    }
    return MigratorExpressionUtils.extractAll(customArtifactSourceTemplate.getScript());
  }

  @Override
  public TemplateEntityType getTemplateEntityType() {
    return TemplateEntityType.ARTIFACT_SOURCE_TEMPLATE;
  }

  @Override
  public boolean isMigrationSupported() {
    return true;
  }

  @Override
  public JsonNode getNgTemplateConfigSpec(Template template, String orgIdentifier, String projectIdentifier) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();

    CustomArtifactSourceTemplate artifactSource =
        (CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource();
    List<NGVariable> inputs = new ArrayList<>();
    if (isNotEmpty(template.getVariables())) {
      template.getVariables().forEach(v
          -> inputs.add(StringNGVariable.builder()
                            .name(v.getName())
                            .type(NGVariableType.STRING)
                            .description(v.getDescription())
                            .value(ParameterField.createValueField(v.getValue()))
                            .build()));
    }

    List<NGVariable> attributes = new ArrayList<>();
    if (isNotEmpty(artifactSource.getCustomRepositoryMapping().getArtifactAttributes())) {
      attributes =
          artifactSource.getCustomRepositoryMapping()
              .getArtifactAttributes()
              .stream()
              .map(attribute
                  -> StringNGVariable.builder()
                         .name(isEmpty(attribute.getMappedAttribute()) ? PLEASE_FIX_ME : attribute.getMappedAttribute())
                         .value(ParameterField.createValueField(attribute.getRelativePath()))
                         .build())
              .collect(Collectors.toList());
    }

    CustomArtifactConfig customArtifactConfig =
        CustomArtifactConfig.builder()
            .version(ParameterField.createValueField("<+input>"))
            .timeout(ParameterField.createValueField(Timeout.fromString(artifactSource.getTimeoutSeconds() + "s")))
            .inputs(inputs)
            .scripts(CustomArtifactScripts.builder()
                         .fetchAllArtifacts(FetchAllArtifacts.builder()
                                                .artifactsArrayPath(ParameterField.createValueField(
                                                    artifactSource.getCustomRepositoryMapping().getArtifactRoot()))
                                                .versionPath(ParameterField.createValueField(
                                                    artifactSource.getCustomRepositoryMapping().getBuildNoPath()))
                                                .attributes(attributes)
                                                .shellScriptBaseStepInfo(
                                                    CustomArtifactScriptInfo.builder()
                                                        .shell(ShellType.Bash)
                                                        .source(CustomArtifactScriptSourceWrapper.builder()
                                                                    .type("Inline")
                                                                    .spec(CustomScriptInlineSource.builder()
                                                                              .script(ParameterField.createValueField(
                                                                                  artifactSource.getScript()))
                                                                              .build())
                                                                    .build())
                                                        .build())
                                                .build())
                         .build())
            .build();
    return JsonPipelineUtils.asTree(customArtifactConfig);
  }

  @Override
  public String getNgTemplateStepName(Template template) {
    return CUSTOM_ARTIFACT_NAME;
  }

  @Override
  public String getTimeoutString(Template template) {
    ArtifactSourceTemplate artifactSourceTemplate = (ArtifactSourceTemplate) template.getTemplateObject();
    String timeoutSeconds =
        ((CustomArtifactSourceTemplate) artifactSourceTemplate.getArtifactSource()).getTimeoutSeconds();
    try {
      return Integer.valueOf(timeoutSeconds) + "s";
    } catch (Exception e) {
      return "60s";
    }
  }
}
