/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step.azure.webapp;

import io.harness.cdng.azure.webapp.AzureWebAppRollbackStepInfo;
import io.harness.cdng.azure.webapp.AzureWebAppRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.ngmigration.beans.SupportStatus;
import io.harness.ngmigration.beans.WorkflowMigrationContext;
import io.harness.ngmigration.service.step.StepMapper;
import io.harness.plancreator.steps.AbstractStepNode;

import software.wings.beans.GraphNode;
import software.wings.sm.State;
import software.wings.sm.states.pcf.PcfRollbackState;

import java.util.Map;

public class AzureSlotRollbackStepMapperImpl extends StepMapper {
  @Override
  public SupportStatus stepSupportStatus(GraphNode graphNode) {
    return SupportStatus.SUPPORTED;
  }

  @Override
  public String getStepType(GraphNode stepYaml) {
    return StepSpecTypeConstants.AZURE_WEBAPP_ROLLBACK;
  }

  @Override
  public State getState(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    PcfRollbackState state = new PcfRollbackState(stepYaml.getName());
    state.parseProperties(properties);
    return state;
  }

  @Override
  public AbstractStepNode getSpec(WorkflowMigrationContext context, GraphNode graphNode) {
    PcfRollbackState state = (PcfRollbackState) getState(graphNode);
    AzureWebAppRollbackStepInfo azureWebAppRollbackStepInfo = AzureWebAppRollbackStepInfo.infoBuilder().build();

    AzureWebAppRollbackStepNode azureWebAppRollbackStepNode = new AzureWebAppRollbackStepNode();
    azureWebAppRollbackStepNode.setAzureWebAppRollbackStepInfo(azureWebAppRollbackStepInfo);

    baseSetup(state, azureWebAppRollbackStepNode);

    return azureWebAppRollbackStepNode;
  }
  @Override
  public boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2) {
    return true;
  }
}
