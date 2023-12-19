/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.pipeline.executions;

import static io.harness.executions.steps.StepSpecTypeConstants.CLOUDFORMATION_DELETE_STACK;
import static io.harness.executions.steps.StepSpecTypeConstants.K8S_ROLLING_ROLLBACK;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_APPLY;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_DESTROY;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_PLAN;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_ROLLBACK;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.EXPIRED;
import static io.harness.reflection.ReflectionUtils.getFieldValuesByType;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.cdng.execution.StageExecutionInfo.StageExecutionInfoKeys;
import io.harness.cdng.execution.service.StageExecutionInfoService;
import io.harness.cdng.instance.InstanceDeploymentInfoStatus;
import io.harness.cdng.instance.service.InstanceDeploymentInfoService;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.logstreaming.ILogStreamingStepClient;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.plancreator.NGCommonUtilPlanCreationConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.utils.StageStatus;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_COMMON_STEPS, HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class CdngPipelineExecutionUpdateEventHandler implements OrchestrationEventHandler {
  private static final Set<String> STEPS_TO_UPDATE_LOG_STREAMS =
      getFieldValuesByType(StepSpecTypeConstants.class, String.class);
  private static final Set<String> STAGES_TO_UPDATE =
      Sets.newHashSet(ExecutionNodeType.CUSTOM_STAGE.getName(), ExecutionNodeType.DEPLOYMENT_STAGE_STEP.getName());

  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private StepHelper stepHelper;
  @Inject private AccountService accountService;
  @Inject private StageExecutionInfoService stageExecutionInfoService;
  @Inject private InstanceDeploymentInfoService instanceDeploymentInfoService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    if (isDeploymentOrCustomStageStep(event.getAmbiance())) {
      processDeploymentOrCustomStageEvent(event);
    } else if (isRollbackStepNode(event.getAmbiance())) {
      processRollbackStepEvent(event);
    }

    try {
      if (isExpiredOrAborted(event.getStatus()) && (isK8sOrTerraformRollback(event.getAmbiance()))) {
        String accountName = accountService.getAccount(AmbianceUtils.getAccountId(event.getAmbiance())).getName();
        stepHelper.sendRollbackTelemetryEvent(event.getAmbiance(), event.getStatus(), accountName);
      }

      if (updateLogStreams(event)) {
        List<String> logKeys = StepUtils.generateLogKeys(event.getAmbiance(), Collections.emptyList());
        if (EmptyPredicate.isNotEmpty(logKeys)) {
          String logKey = logKeys.get(0);
          ILogStreamingStepClient logStreamingStepClient =
              logStreamingStepClientFactory.getLogStreamingStepClient(event.getAmbiance());
          logStreamingStepClient.closeLogStreamsWithPrefix(
              logKey, treatLogKeyAsPrefixForClosingStreams(event.getAmbiance()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to close log streams", ex);
    }
  }

  private void processRollbackStepEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String stageExecutionId = ambiance.getStageExecutionId();
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    try {
      long startTs = AmbianceUtils.getCurrentLevelStartTs(event.getAmbiance());
      long endTs = event.getEndTs();
      long rollbackDuration = endTs - startTs;

      Map<String, Object> updates = new HashMap<>();
      updates.put(StageExecutionInfoKeys.rollbackDuration, rollbackDuration);
      stageExecutionInfoService.update(scope, stageExecutionId, updates);
    } catch (Exception ex) {
      log.error(
          String.format(
              "Unable to update stage execution summary, accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s, "
                  + "stageExecutionId: %s",
              accountIdentifier, orgIdentifier, projectIdentifier, stageExecutionId),
          ex);
    }
  }

  private void processDeploymentOrCustomStageEvent(@NotNull OrchestrationEvent event) {
    Status status = event.getStatus();
    Ambiance ambiance = event.getAmbiance();
    String stageExecutionId = ambiance.getStageExecutionId();
    StageStatus stageStatus = status.equals(Status.SUCCEEDED) ? StageStatus.SUCCEEDED : StageStatus.FAILED;
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    String failureToUpdateStageExecutionSummary = String.format(
        "Unable to update stage execution summary, accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s, "
            + "stageExecutionId: %s, stageStatus: %s",
        accountIdentifier, orgIdentifier, projectIdentifier, stageExecutionId, stageStatus);
    if (StatusUtils.isFinalStatus(status)) {
      try {
        stageExecutionInfoService.updateStatus(scope, stageExecutionId, stageStatus);
      } catch (Exception ex) {
        log.error(
            String.format(
                "Unable to update stage execution status, accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s, "
                    + "stageExecutionId: %s, stageStatus: %s",
                accountIdentifier, orgIdentifier, projectIdentifier, stageExecutionId, stageStatus),
            ex);
      }

      try {
        stageExecutionInfoService.deleteStageStatusKeyLock(scope, stageExecutionId);
      } catch (Exception ex) {
        // after expire time set on LoadingCache concurrent map, the stage status key locks will be auto-deleted
        log.warn(
            String.format(
                "Unable to delete stage status key lock, accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s, "
                    + "stageExecutionId: %s, stageStatus: %s",
                accountIdentifier, orgIdentifier, projectIdentifier, stageExecutionId, stageStatus),
            ex);
      }

      try {
        Map<String, Object> updates = new HashMap<>();
        updates.put(StageExecutionInfoKeys.status, status);
        updates.put(StageExecutionInfoKeys.endts, event.getEndTs());
        stageExecutionInfoService.update(scope, stageExecutionId, updates);
      } catch (Exception ex) {
        log.error(failureToUpdateStageExecutionSummary, ex);
      }

      InstanceDeploymentInfoStatus instanceDeploymentInfoStatus = status.equals(Status.SUCCEEDED)
          ? InstanceDeploymentInfoStatus.SUCCEEDED
          : InstanceDeploymentInfoStatus.FAILED;
      try {
        // TODO execute this async by using ExecutionService and then calculate execution key
        instanceDeploymentInfoService.updateStatus(scope, stageExecutionId, instanceDeploymentInfoStatus);
      } catch (Exception ex) {
        log.error(
            String.format(
                "Unable to update instance status, accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s, "
                    + "stageExecutionId: %s, instanceDeploymentInfoStatus: %s",
                accountIdentifier, orgIdentifier, projectIdentifier, stageExecutionId, instanceDeploymentInfoStatus),
            ex);
      }
    }
  }

  private boolean treatLogKeyAsPrefixForClosingStreams(Ambiance ambiance) {
    return !isStepType(ambiance, CLOUDFORMATION_DELETE_STACK);
  }

  private boolean isDeploymentOrCustomStageStep(Ambiance ambiance) {
    Level currentLevel = AmbianceUtils.obtainCurrentLevel(ambiance);
    return currentLevel != null
        && (STAGES_TO_UPDATE.contains(Objects.requireNonNull(currentLevel.getStepType().getType())));
  }

  private boolean isRollbackStepNode(Ambiance ambiance) {
    return Objects.equals(AmbianceUtils.getCurrentStepType(ambiance), RollbackOptionalChildChainStep.STEP_TYPE);
  }

  private boolean isK8sOrTerraformRollback(Ambiance ambiance) {
    return isK8sRollingRollbackStep(ambiance) || isTerraformRollbackStep(ambiance);
  }

  private boolean isK8sRollingRollbackStep(Ambiance ambiance) {
    return isStepType(ambiance, K8S_ROLLING_ROLLBACK);
  }

  private boolean isTerraformRollbackStep(Ambiance ambiance) {
    return isStepType(ambiance, TERRAFORM_ROLLBACK);
  }

  // currently not used, but left here for future use.
  private boolean isInfrastructureRollback(Ambiance ambiance) {
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    return level.getIdentifier().equals(NGCommonUtilPlanCreationConstants.INFRA_ROLLBACK_NODE_IDENTIFIER);
  }

  // currently not used, but left here for future use.
  private boolean isDeploymentRollback(Ambiance ambiance) {
    boolean hasInfraRollbackNodeStep = false;
    boolean hasRollbackStepsStep = false;

    for (Level level : ambiance.getLevelsList()) {
      if (!hasRollbackStepsStep && level.getIdentifier().equals(YAMLFieldNameConstants.ROLLBACK_STEPS)) {
        hasRollbackStepsStep = true;
      }

      // if we find the infra rollback node identifier then it must be an infrastructure rollback and not the deployment
      // one
      if (level.getIdentifier().equals(NGCommonUtilPlanCreationConstants.INFRA_ROLLBACK_NODE_IDENTIFIER)) {
        hasInfraRollbackNodeStep = true;
        break;
      }
    }

    return hasRollbackStepsStep && !hasInfraRollbackNodeStep;
  }

  // currently not used, but left here for future use.
  private boolean isTerraformInfrastructureRollback(Ambiance ambiance) {
    boolean isTFStep = isStepType(ambiance, TERRAFORM_ROLLBACK) || isStepType(ambiance, TERRAFORM_DESTROY)
        || isStepType(ambiance, TERRAFORM_PLAN) || isStepType(ambiance, TERRAFORM_APPLY);

    if (isTFStep) {
      for (Level level : ambiance.getLevelsList()) {
        if (level.getIdentifier().equals(NGCommonUtilPlanCreationConstants.INFRA_ROLLBACK_NODE_IDENTIFIER)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isExpiredOrAborted(Status status) {
    return EXPIRED.equals(status) || ABORTED.equals(status);
  }

  private boolean isStepType(Ambiance ambiance, String stepSpecType) {
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    return level != null && level.getStepType() != null && stepSpecType.equals(level.getStepType().getType());
  }

  private boolean updateLogStreams(OrchestrationEvent event) {
    return StatusUtils.isFinalStatus(event.getStatus())
        && STEPS_TO_UPDATE_LOG_STREAMS.contains(
            Objects.requireNonNull(AmbianceUtils.obtainCurrentLevel(event.getAmbiance())).getStepType().getType());
  }
}
