/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.workflow;

import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.step.StepMapperFactory;

import software.wings.beans.BuildWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.ngmigration.CgEntityId;
import software.wings.service.impl.yaml.handler.workflow.BuildWorkflowYamlHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class BuildWorkflowHandlerImpl extends WorkflowHandler {
  @Inject BuildWorkflowYamlHandler buildWorkflowYamlHandler;
  @Inject private StepMapperFactory stepMapperFactory;

  @Override
  public List<GraphNode> getSteps(Workflow workflow) {
    BuildWorkflow orchestrationWorkflow = (BuildWorkflow) workflow.getOrchestrationWorkflow();
    return getSteps(orchestrationWorkflow.getWorkflowPhases(), orchestrationWorkflow.getPreDeploymentSteps(),
        orchestrationWorkflow.getPostDeploymentSteps());
  }

  @Override
  public boolean areSimilar(Workflow workflow1, Workflow workflow2) {
    return areSimilar(stepMapperFactory, workflow1, workflow2);
  }

  @Override
  public JsonNode getTemplateSpec(Map<CgEntityId, NGYamlFile> migratedEntities, Workflow workflow) {
    return getCustomStageTemplateSpec(migratedEntities, workflow, stepMapperFactory);
  }
}
