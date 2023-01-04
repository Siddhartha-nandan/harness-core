/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step;

import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.data.structure.CollectionUtils;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.plancreator.steps.internal.PmsAbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.template.TemplateStepNode;
import io.harness.template.yaml.TemplateLinkConfig;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.GraphNode;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.sm.State;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public interface StepMapper {
  default List<CgEntityId> getReferencedEntities(GraphNode graphNode) {
    return Collections.emptyList();
  }

  String getStepType(GraphNode stepYaml);

  State getState(GraphNode stepYaml);

  AbstractStepNode getSpec(Map<CgEntityId, NGYamlFile> migratedEntities, GraphNode graphNode);

  default TemplateStepNode getTemplateSpec(Map<CgEntityId, NGYamlFile> migratedEntities, GraphNode graphNode) {
    return null;
  }

  default TemplateStepNode defaultTemplateSpecMapper(
      Map<CgEntityId, NGYamlFile> migratedEntities, GraphNode graphNode) {
    String templateId = graphNode.getTemplateUuid();
    if (StringUtils.isBlank(templateId)) {
      return null;
    }
    NGYamlFile template =
        migratedEntities.get(CgEntityId.builder().id(templateId).type(NGMigrationEntityType.TEMPLATE).build());
    TemplateLinkConfig templateLinkConfig = new TemplateLinkConfig();
    templateLinkConfig.setTemplateRef(MigratorUtility.getIdentifierWithScope(template.getNgEntityDetail()));
    templateLinkConfig.setVersionLabel("v1");

    TemplateStepNode templateStepNode = new TemplateStepNode();
    templateStepNode.setIdentifier(MigratorUtility.generateIdentifier(graphNode.getName()));
    templateStepNode.setName(graphNode.getName());
    templateStepNode.setDescription(getDescription(graphNode));
    templateStepNode.setTemplate(templateLinkConfig);
    return templateStepNode;
  }

  boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2);

  default ParameterField<Timeout> getTimeout(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);

    String timeoutString = "10m";
    if (properties.containsKey("timeoutMillis")) {
      long t = Long.parseLong(properties.get("timeoutMillis").toString()) / 1000;
      timeoutString = t + "s";
    }
    return ParameterField.createValueField(Timeout.builder().timeoutString(timeoutString).build());
  }

  default ParameterField<Timeout> getTimeout(State state) {
    return MigratorUtility.getTimeout(state.getTimeoutMillis());
  }

  default String getDescription(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    return properties.getOrDefault("description", "").toString();
  }

  default Map<String, Object> getProperties(GraphNode stepYaml) {
    return CollectionUtils.emptyIfNull(stepYaml.getProperties());
  }

  default void baseSetup(GraphNode graphNode, AbstractStepNode stepNode) {
    stepNode.setIdentifier(MigratorUtility.generateIdentifier(graphNode.getName()));
    stepNode.setName(graphNode.getName());
    stepNode.setDescription(getDescription(graphNode));
    if (stepNode instanceof PmsAbstractStepNode) {
      PmsAbstractStepNode pmsAbstractStepNode = (PmsAbstractStepNode) stepNode;
      pmsAbstractStepNode.setTimeout(getTimeout(graphNode));
    }
    if (stepNode instanceof CdAbstractStepNode) {
      CdAbstractStepNode cdAbstractStepNode = (CdAbstractStepNode) stepNode;
      cdAbstractStepNode.setTimeout(getTimeout(graphNode));
    }
  }

  default void baseSetup(State state, AbstractStepNode stepNode) {
    stepNode.setIdentifier(MigratorUtility.generateIdentifier(state.getName()));
    stepNode.setName(state.getName());
    if (stepNode instanceof PmsAbstractStepNode) {
      PmsAbstractStepNode pmsAbstractStepNode = (PmsAbstractStepNode) stepNode;
      pmsAbstractStepNode.setTimeout(getTimeout(state));
    }
    if (stepNode instanceof CdAbstractStepNode) {
      CdAbstractStepNode cdAbstractStepNode = (CdAbstractStepNode) stepNode;
      cdAbstractStepNode.setTimeout(getTimeout(state));
    }
  }
}
