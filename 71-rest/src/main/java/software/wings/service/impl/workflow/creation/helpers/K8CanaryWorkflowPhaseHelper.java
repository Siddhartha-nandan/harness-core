package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.model.K8sExpressions.canaryWorkloadExpression;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;
import static software.wings.sm.StateType.K8S_DELETE;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.common.WorkflowConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;

import java.util.ArrayList;
import java.util.List;

public class K8CanaryWorkflowPhaseHelper extends PhaseHelper {
  public List<PhaseStep> getWorkflowPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getCanaryDeployPhaseStep());
    phaseSteps.add(getCanaryVerifyPhaseStep());
    phaseSteps.add(getCanaryWrapUpPhaseStep());
    return phaseSteps;
  }

  public List<PhaseStep> getRollbackPhaseSteps() {
    List<PhaseStep> rollbackPhaseSteps = new ArrayList<>();
    rollbackPhaseSteps.add(getCanaryRollbackDeployPhaseStep());
    rollbackPhaseSteps.add(getCanaryRollbackWrapUpPhaseStep());
    return rollbackPhaseSteps;
  }

  // Steps for Canary
  private PhaseStep getCanaryDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_CANARY_DEPLOY.name())
                     .name(WorkflowConstants.K8S_CANARY_DEPLOY)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("instances", "1")
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }

  private PhaseStep getCanaryVerifyPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, "Verify").build();
  }

  private PhaseStep getCanaryWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_DELETE.name())
                     .name("Canary Delete")
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("resources", canaryWorkloadExpression)
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }

  // Steps for Canary Rollback
  private PhaseStep getCanaryRollbackDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
        .withStatusForRollback(ExecutionStatus.SUCCESS)
        .withRollback(true)
        .build();
  }

  private PhaseStep getCanaryRollbackWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .withPhaseStepNameForRollback(WorkflowServiceHelper.WRAP_UP)
        .withRollback(true)
        .build();
  }
}