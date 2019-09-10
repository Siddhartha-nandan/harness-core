package software.wings.service.impl.workflow.creation;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;

@Slf4j
public abstract class WorkflowCreator {
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;

  public abstract Workflow createWorkflow(Workflow clientWorkflow);

  public abstract void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase);

  void addLinkedPreOrPostDeploymentSteps(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPreDeploymentSteps(), null);
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPostDeploymentSteps(), null);
  }
}
