/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.jira.update;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.APPROVAL_STEP_NG_ERROR;
import static io.harness.jira.JiraConstantsNG.ISSUE_TYPE_NAME;
import static io.harness.jira.JiraConstantsNG.STATUS_NAME;

import io.harness.EntityType;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.engine.executions.step.StepExecutionEntityService;
import io.harness.eraro.Level;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.step.jira.update.JiraUpdateStepExecutionDetails;
import io.harness.jira.JiraActionNG;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.StepUtils;
import io.harness.steps.executables.PipelineTaskExecutable;
import io.harness.steps.jira.JiraStepHelperService;
import io.harness.steps.jira.JiraStepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_DASHBOARD})
@OwnedBy(CDC)
public class JiraUpdateStep extends PipelineTaskExecutable<JiraTaskNGResponse> {
  public static final StepType STEP_TYPE = StepSpecTypeConstants.JIRA_UPDATE_STEP_TYPE;

  @Inject private JiraStepHelperService jiraStepHelperService;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private StepExecutionEntityService stepExecutionEntityService;
  @Inject @Named("DashboardExecutorService") ExecutorService dashboardExecutorService;

  @Override
  public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    JiraUpdateSpecParameters specParameters = (JiraUpdateSpecParameters) stepParameters.getSpec();
    String connectorRef = specParameters.getConnectorRef().getValue();
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorRef, accountIdentifier, orgIdentifier, projectIdentifier);
    EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
    List<EntityDetail> entityDetailList = new ArrayList<>();
    entityDetailList.add(entityDetail);
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepBaseParameters stepParameters, StepInputPackage inputPackage) {
    JiraUpdateSpecParameters specParameters = (JiraUpdateSpecParameters) stepParameters.getSpec();
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder()
            .action(JiraActionNG.UPDATE_ISSUE)
            .issueKey(specParameters.getIssueKey().getValue())
            .transitionToStatus(specParameters.getTransitionTo() == null
                    ? null
                    : (String) specParameters.getTransitionTo().getStatus().fetchFinalValue())
            .transitionName(specParameters.getTransitionTo() == null
                    ? null
                    : (String) specParameters.getTransitionTo().getTransitionName().fetchFinalValue())
            .fields(JiraStepUtils.processJiraFieldsInParameters(specParameters.getFields()))
            .delegateSelectors(
                StepUtils.getDelegateSelectorListFromTaskSelectorYaml(specParameters.getDelegateSelectors()));
    return jiraStepHelperService.prepareTaskRequest(paramsBuilder, ambiance,
        specParameters.getConnectorRef().getValue(), stepParameters.getTimeout().getValue(), "Jira Task: Update Issue",
        TaskSelectorYaml.toTaskSelector(specParameters.getDelegateSelectors()));
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepBaseParameters stepParameters,
      ThrowingSupplier<JiraTaskNGResponse> responseSupplier) throws Exception {
    dashboardExecutorService.submit(
        () -> updateJiraUpdateStepExecutionDetailsFromResponse(ambiance, responseSupplier, stepParameters.getName()));
    return jiraStepHelperService.prepareStepResponse(responseSupplier);
  }

  private void updateJiraUpdateStepExecutionDetailsFromResponse(
      Ambiance ambiance, ThrowingSupplier<JiraTaskNGResponse> responseSupplier, String stepName) {
    try {
      JiraTaskNGResponse taskResponse = responseSupplier.get();
      if (taskResponse != null && taskResponse.getIssue() != null) {
        JiraUpdateStepExecutionDetails jiraUpdateStepExecutionDetails =
            JiraUpdateStepExecutionDetails.builder()
                .url(taskResponse.getIssue().getUrl())
                .issueType(taskResponse.getIssue().getFields().getOrDefault(ISSUE_TYPE_NAME, "").toString())
                .ticketStatus(taskResponse.getIssue().getFields().getOrDefault(STATUS_NAME, "").toString())
                .build();
        stepExecutionEntityService.updateStepExecutionEntity(
            ambiance, null, jiraUpdateStepExecutionDetails, stepName, Status.RUNNING);
      }
    } catch (Exception ex) {
      FailureInfo failureInfo = FailureInfo.newBuilder()
                                    .addFailureData(FailureData.newBuilder()
                                                        .setLevel(Level.ERROR.name())
                                                        .setCode(APPROVAL_STEP_NG_ERROR.name())
                                                        .setMessage(ExceptionUtils.getMessage(ex))
                                                        .build())
                                    .build();
      stepExecutionEntityService.updateStepExecutionEntity(ambiance, failureInfo, null, stepName, Status.RUNNING);
    }
  }

  @Override
  public Class<StepBaseParameters> getStepParametersClass() {
    return StepBaseParameters.class;
  }
}
