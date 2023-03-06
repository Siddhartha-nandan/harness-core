/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraformcloud;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.terraformcloud.TerraformCloudConfigMapper;
import io.harness.connector.task.terraformcloud.TerraformCloudValidationHandler;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.terraformcloudconnector.TerraformCloudConnectorDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.beans.terraformcloud.PlanType;
import io.harness.delegate.beans.terraformcloud.RollbackType;
import io.harness.delegate.beans.terraformcloud.TerraformCloudTaskParams;
import io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.common.AbstractDelegateRunnableTask;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudDelegateTaskResponse;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudOrganizationsTaskResponse;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudRollbackTaskResponse;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudRunTaskResponse;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudValidateTaskResponse;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudWorkspacesTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.terraformcloud.TerraformCloudApiTokenCredentials;
import io.harness.terraformcloud.TerraformCloudConfig;
import io.harness.terraformcloud.model.PolicyCheckData;
import io.harness.terraformcloud.model.RunData;
import io.harness.terraformcloud.model.RunRequest;
import io.harness.terraformcloud.model.RunStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public class TerraformCloudTaskNG extends AbstractDelegateRunnableTask {
  private static final String TFC_PLAN_FILE_OUTPUT_NAME = "tfcplan.json";
  private static final String TFC_DESTROY_PLAN_FILE_OUTPUT_NAME = "tfcdestroyplan.json";
  private static final String TFC_POLICY_CHECK_FILE_NAME = "tfcpolicychecks.json";

  @Inject private TerraformCloudValidationHandler terraformCloudValidationHandler;
  @Inject private TerraformCloudConfigMapper terraformCloudConfigMapper;
  @Inject private TerraformCloudTaskHelper terraformCloudTaskHelper;
  @Inject private RunRequestCreator runRequestCreator;

  public TerraformCloudTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("Object Array parameters not supported");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) throws IOException {
    TerraformCloudTaskParams taskParameters = (TerraformCloudTaskParams) parameters;

    TerraformCloudConnectorDTO terraformCloudConnectorDTO = taskParameters.getTerraformCloudConnectorDTO();
    TerraformCloudConfig terraformCloudConfig = terraformCloudConfigMapper.mapTerraformCloudConfigWithDecryption(
        terraformCloudConnectorDTO, taskParameters.getEncryptionDetails());

    CommandUnitsProgress commandUnitsProgress = taskParameters.getCommandUnitsProgress() != null
        ? taskParameters.getCommandUnitsProgress()
        : CommandUnitsProgress.builder().build();
    TerraformCloudDelegateTaskResponse taskResponse;
    switch (taskParameters.getTerraformCloudTaskType()) {
      case VALIDATE:
        ConnectorValidationResult connectorValidationResult =
            terraformCloudValidationHandler.validate(terraformCloudConfig);
        connectorValidationResult.setDelegateId(getDelegateId());
        taskResponse =
            TerraformCloudValidateTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
        break;
      case GET_ORGANIZATIONS:
        taskResponse = TerraformCloudOrganizationsTaskResponse.builder()
                           .organizations(terraformCloudTaskHelper.getOrganizationsMap(terraformCloudConfig))
                           .build();
        break;
      case GET_WORKSPACES:
        taskResponse = TerraformCloudWorkspacesTaskResponse.builder()
                           .workspaces(terraformCloudTaskHelper.getWorkspacesMap(
                               terraformCloudConfig, taskParameters.getOrganization()))
                           .build();
        break;
      case RUN_REFRESH_STATE:
        refreshState((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
            taskParameters, commandUnitsProgress);
        taskResponse = TerraformCloudRunTaskResponse.builder().build();
        break;
      case RUN_PLAN_ONLY:
      case RUN_PLAN:
        taskResponse = plan((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
            taskParameters, commandUnitsProgress);
        break;
      case RUN_PLAN_AND_APPLY:
      case RUN_PLAN_AND_DESTROY:
        taskResponse =
            autoApply((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
                taskParameters, commandUnitsProgress);
        break;
      case RUN_APPLY:
        taskResponse = apply((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
            taskParameters, commandUnitsProgress);
        break;
      case ROLLBACK:
        taskResponse = rollback((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
            taskParameters, commandUnitsProgress);
        break;
      case GET_LAST_APPLIED_RUN:
        taskResponse =
            getLastAppliedRun((TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials(),
                taskParameters, commandUnitsProgress);
        break;
      default:
        throw new InvalidRequestException("Terraform Cloud Task type not identified");
    }
    taskResponse.setUnitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
    taskResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    return taskResponse;
  }

  private void refreshState(TerraformCloudApiTokenCredentials credentials, TerraformCloudTaskParams taskParameters,
      CommandUnitsProgress commandUnitsProgress) throws IOException {
    String url = credentials.getUrl();
    String token = credentials.getToken();

    RunData runData = terraformCloudTaskHelper.createRun(url, token, runRequestCreator.createRunRequest(taskParameters),
        taskParameters.isDiscardPendingRuns(), taskParameters.getTerraformCloudTaskType(),
        getLogCallback(TerraformCloudCommandUnit.PLAN.getDisplayName(), commandUnitsProgress));

    policyCheckInternal(url, token, taskParameters, runData.getId(), commandUnitsProgress);
  }

  private TerraformCloudRunTaskResponse plan(TerraformCloudApiTokenCredentials credentials,
      TerraformCloudTaskParams taskParameters, CommandUnitsProgress commandUnitsProgress) throws IOException {
    String url = credentials.getUrl();
    String token = credentials.getToken();

    RunData runData = terraformCloudTaskHelper.createRun(url, token, runRequestCreator.createRunRequest(taskParameters),
        taskParameters.isDiscardPendingRuns(), taskParameters.getTerraformCloudTaskType(),
        getLogCallback(TerraformCloudCommandUnit.PLAN.getDisplayName(), commandUnitsProgress));

    String tfPolicyCheckFileId = policyCheckInternal(url, token, taskParameters, runData.getId(), commandUnitsProgress);

    String tfPlanJsonFileId = null;
    if (taskParameters.isExportJsonTfPlan()) {
      String jsonPlan = terraformCloudTaskHelper.getJsonPlan(credentials.getUrl(), credentials.getToken(), runData);
      if (jsonPlan != null) {
        tfPlanJsonFileId = terraformCloudTaskHelper.uploadJsonFile(taskParameters.getAccountId(), getDelegateId(),
            getTaskId(), taskParameters.getEntityId(),
            taskParameters.getPlanType() == PlanType.APPLY ? TFC_PLAN_FILE_OUTPUT_NAME
                                                           : TFC_DESTROY_PLAN_FILE_OUTPUT_NAME,
            jsonPlan, FileBucket.TERRAFORM_PLAN_JSON);
      }
    }
    return TerraformCloudRunTaskResponse.builder()
        .runId(runData.getId())
        .tfPlanJsonFileId(tfPlanJsonFileId)
        .policyChecksJsonFileId(tfPolicyCheckFileId)
        .build();
  }

  private TerraformCloudRunTaskResponse autoApply(TerraformCloudApiTokenCredentials credentials,
      TerraformCloudTaskParams taskParameters, CommandUnitsProgress commandUnitsProgress) throws IOException {
    String url = credentials.getUrl();
    String token = credentials.getToken();

    RunData runData = terraformCloudTaskHelper.createRun(url, token, runRequestCreator.createRunRequest(taskParameters),
        taskParameters.isDiscardPendingRuns(), taskParameters.getTerraformCloudTaskType(),
        getLogCallback(TerraformCloudCommandUnit.PLAN.getDisplayName(), commandUnitsProgress));

    String tfPolicyCheckFileId = policyCheckInternal(url, token, taskParameters, runData.getId(), commandUnitsProgress);
    String output = applyInternal(url, token, taskParameters.isPolicyOverride(), runData, commandUnitsProgress);
    return TerraformCloudRunTaskResponse.builder()
        .runId(runData.getId())
        .tfOutput(output)
        .policyChecksJsonFileId(tfPolicyCheckFileId)
        .build();
  }

  public TerraformCloudRunTaskResponse apply(TerraformCloudApiTokenCredentials credentials,
      TerraformCloudTaskParams taskParameters, CommandUnitsProgress commandUnitsProgress) throws IOException {
    LogCallback logCallback = getLogCallback(TerraformCloudCommandUnit.APPLY.getDisplayName(), commandUnitsProgress);
    String url = credentials.getUrl();
    String token = credentials.getToken();
    String runId = taskParameters.getRunId();

    RunData runData = terraformCloudTaskHelper.getRun(url, token, runId);
    RunStatus status = runData.getAttributes().getStatus();
    if (status == RunStatus.POLICY_OVERRIDE) {
      List<PolicyCheckData> policyCheckData = terraformCloudTaskHelper.getPolicyCheckData(url, token, runId);
      terraformCloudTaskHelper.overridePolicy(url, token, policyCheckData, logCallback);
      status = terraformCloudTaskHelper.getRunStatus(url, token, runId);
    }
    if (status == RunStatus.POLICY_CHECKED) {
      String output = terraformCloudTaskHelper.applyRun(url, token, runId, taskParameters.getMessage(), logCallback);
      return TerraformCloudRunTaskResponse.builder().runId(runId).tfOutput(output).build();
    } else if (!runData.getAttributes().isHasChanges()) {
      logCallback.saveExecutionLog("Apply will not run. No changes.", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return TerraformCloudRunTaskResponse.builder().runId(runId).build();
    } else {
      logCallback.saveExecutionLog(format("Apply can't be done when run is in status %s", status.name()),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      // toDo custom exception will be thrown here
      throw new InvalidRequestException(format("Apply can't be done when run is in status %s", status.name()));
    }
  }

  private TerraformCloudRollbackTaskResponse rollback(TerraformCloudApiTokenCredentials credentials,
      TerraformCloudTaskParams taskParameters, CommandUnitsProgress commandUnitsProgress) throws IOException {
    String url = credentials.getUrl();
    String token = credentials.getToken();
    String runId = taskParameters.getRunId();
    String workspaceId = taskParameters.getWorkspace();
    RollbackType rollbackType = taskParameters.getRollbackType();

    LogCallback logCallback =
        getLogCallback(TerraformCloudCommandUnit.FETCH_LAST_APPLIED_RUN.getDisplayName(), commandUnitsProgress);
    logCallback.saveExecutionLog(
        format("Check last applied run in workspace: %s", workspaceId), INFO, CommandExecutionStatus.RUNNING);
    String lastAppliedRunId = terraformCloudTaskHelper.getLastAppliedRunId(url, token, workspaceId);
    if (lastAppliedRunId == null || lastAppliedRunId.equals(runId)) {
      logCallback.saveExecutionLog(
          "No run wasn't applied in this stage. Therefore skipping rollback.", INFO, CommandExecutionStatus.SUCCESS);
      return TerraformCloudRollbackTaskResponse.builder().build();
    }

    RunData run =
        terraformCloudTaskHelper.getRun(url, token, rollbackType == RollbackType.APPLY ? runId : lastAppliedRunId);
    logCallback.saveExecutionLog(rollbackType == RollbackType.APPLY
            ? format("Rolling back to version config version from run: %s", runId)
            : format("There wasn't any run before execution. Destroy resources in workspace: %s", workspaceId),
        INFO, CommandExecutionStatus.SUCCESS);

    RunRequest runRequest = runRequestCreator.mapRunDataToRunRequest(run, taskParameters.getMessage(), rollbackType);

    RunData runData = terraformCloudTaskHelper.createRun(url, token, runRequest, taskParameters.isDiscardPendingRuns(),
        taskParameters.getTerraformCloudTaskType(),
        getLogCallback(TerraformCloudCommandUnit.PLAN.getDisplayName(), commandUnitsProgress));

    String tfPolicyCheckFileId = policyCheckInternal(url, token, taskParameters, runData.getId(), commandUnitsProgress);
    String output = applyInternal(url, token, taskParameters.isPolicyOverride(), runData, commandUnitsProgress);

    return TerraformCloudRollbackTaskResponse.builder()
        .policyChecksJsonFileId(tfPolicyCheckFileId)
        .runId(runData.getId())
        .tfOutput(output)
        .build();
  }

  private String policyCheckInternal(String url, String token, TerraformCloudTaskParams taskParameters, String runId,
      CommandUnitsProgress commandUnitsProgress) throws IOException {
    List<PolicyCheckData> policyCheckData = terraformCloudTaskHelper.getPolicyCheckData(url, token, runId);
    terraformCloudTaskHelper.streamSentinelPolicies(url, token, policyCheckData,
        getLogCallback(TerraformCloudCommandUnit.POLICY_CHECK.getDisplayName(), commandUnitsProgress));

    if (taskParameters.getTerraformCloudTaskType() == TerraformCloudTaskType.RUN_REFRESH_STATE) {
      return null;
    }
    String policyChecksJsonData = new ObjectMapper().writeValueAsString(policyCheckData);
    return terraformCloudTaskHelper.uploadJsonFile(taskParameters.getAccountId(), getDelegateId(), getTaskId(),
        taskParameters.getEntityId(), TFC_POLICY_CHECK_FILE_NAME, policyChecksJsonData,
        FileBucket.TERRAFORM_CLOUD_POLICY_CHECKS);
  }

  private String applyInternal(String url, String token, boolean isPolicyOverride, RunData runData,
      CommandUnitsProgress commandUnitsProgress) throws IOException {
    LogCallback logCallback = getLogCallback(TerraformCloudCommandUnit.APPLY.getDisplayName(), commandUnitsProgress);
    if (runData.getAttributes().getStatus() == RunStatus.POLICY_OVERRIDE) {
      if (isPolicyOverride) {
        List<PolicyCheckData> policyCheckData =
            terraformCloudTaskHelper.getPolicyCheckData(url, token, runData.getId());
        terraformCloudTaskHelper.overridePolicy(url, token, policyCheckData, logCallback);
      } else {
        logCallback.saveExecutionLog("Policy check failed and not overridden", INFO, CommandExecutionStatus.FAILURE);
        // ToDo throw custom exception here
        throw new InvalidRequestException("Policy check failed and not overridden");
      }
    }
    terraformCloudTaskHelper.streamApplyLogs(url, token, runData, logCallback);
    return terraformCloudTaskHelper.getApplyOutput(url, token, runData);
  }

  private TerraformCloudDelegateTaskResponse getLastAppliedRun(
      TerraformCloudApiTokenCredentials terraformCloudCredentials, TerraformCloudTaskParams params,
      CommandUnitsProgress commandUnitsProgress) throws IOException {
    LogCallback logCallback =
        getLogCallback(TerraformCloudCommandUnit.FETCH_LAST_APPLIED_RUN.getDisplayName(), commandUnitsProgress);
    String url = terraformCloudCredentials.getUrl();
    String token = terraformCloudCredentials.getToken();
    String workspace;
    if (params.getWorkspace() != null) {
      workspace = params.getWorkspace();
    } else if (params.getRunId() != null) {
      RunData runData = terraformCloudTaskHelper.getRun(url, token, params.getRunId());
      workspace = terraformCloudTaskHelper.getRelationshipId(runData, "workspace");
    } else {
      logCallback.saveExecutionLog(
          "Workspace or run id must be provided to fetch last applied run", ERROR, CommandExecutionStatus.FAILURE);
      // ToDo throw custom exception here
      throw new InvalidRequestException("Run id not provided");
    }
    logCallback.saveExecutionLog(
        format("Fetch last applied run in workspace: %s", workspace), INFO, CommandExecutionStatus.RUNNING);
    String lastAppliedId = terraformCloudTaskHelper.getLastAppliedRunId(url, token, workspace);
    logCallback.saveExecutionLog(
        format("Last applied run was: %s", lastAppliedId), INFO, CommandExecutionStatus.SUCCESS);
    return TerraformCloudRunTaskResponse.builder().lastAppliedRun(lastAppliedId).workspaceId(workspace).build();
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }

  public LogCallback getLogCallback(String unitName, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(getLogStreamingTaskClient(), unitName, true, commandUnitsProgress);
  }
}
