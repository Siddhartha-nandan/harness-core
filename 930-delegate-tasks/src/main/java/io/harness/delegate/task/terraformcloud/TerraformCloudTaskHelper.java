/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraformcloud;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType.ROLLBACK;
import static io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType.RUN_PLAN_AND_APPLY;
import static io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType.RUN_PLAN_AND_DESTROY;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.terraformcloud.PlanType;
import io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.terraformcloud.TerraformCloudApiTokenCredentials;
import io.harness.terraformcloud.TerraformCloudClient;
import io.harness.terraformcloud.TerraformCloudConfig;
import io.harness.terraformcloud.model.ApplyData;
import io.harness.terraformcloud.model.Data;
import io.harness.terraformcloud.model.OrganizationData;
import io.harness.terraformcloud.model.PlanData;
import io.harness.terraformcloud.model.RunActionRequest;
import io.harness.terraformcloud.model.RunData;
import io.harness.terraformcloud.model.RunRequest;
import io.harness.terraformcloud.model.RunStatus;
import io.harness.terraformcloud.model.StateVersionOutputData;
import io.harness.terraformcloud.model.TerraformCloudResponse;
import io.harness.terraformcloud.model.WorkspaceData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.io.FileUtils;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
@Singleton
public class TerraformCloudTaskHelper {
  // toDo Exception handling

  private static final int CHUNK_SIZE = 100000;
  private static final String TFC_PLAN_FILE_OUTPUT_NAME = "tfcplan.json";
  private static final String TFC_DESTROY_PLAN_FILE_OUTPUT_NAME = "tfcdestroyplan.json";

  @Inject TerraformCloudClient terraformCloudClient;
  @Inject DelegateFileManagerBase delegateFileManager;

  public Map<String, String> getOrganizationsMap(TerraformCloudConfig terraformCloudConfig) throws IOException {
    TerraformCloudApiTokenCredentials credentials =
        (TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials();

    List<OrganizationData> organizationsData = getAllOrganizations(credentials);
    Map<String, String> organizations = new HashMap<>();
    if (isNotEmpty(organizationsData)) {
      organizationsData.forEach(
          organizationData -> organizations.put(organizationData.getId(), organizationData.getAttributes().getName()));
    }
    return organizations;
  }

  public Map<String, String> getWorkspacesMap(TerraformCloudConfig terraformCloudConfig, String organization)
      throws IOException {
    TerraformCloudApiTokenCredentials credentials =
        (TerraformCloudApiTokenCredentials) terraformCloudConfig.getTerraformCloudCredentials();
    List<WorkspaceData> workspacesData = getAllWorkspaces(credentials, organization);

    Map<String, String> workspaces = new HashMap<>();
    if (isNotEmpty(workspacesData)) {
      workspacesData.forEach(
          workspaceData -> workspaces.put(workspaceData.getId(), workspaceData.getAttributes().getName()));
    }
    return workspaces;
  }

  public List<WorkspaceData> getAllWorkspaces(TerraformCloudApiTokenCredentials credentials, String organization)
      throws IOException {
    int pageNumber = 1;
    TerraformCloudResponse<List<WorkspaceData>> response;
    List<WorkspaceData> workspacesData = new ArrayList<>();
    do {
      response =
          terraformCloudClient.listWorkspaces(credentials.getUrl(), credentials.getToken(), organization, pageNumber);
      workspacesData.addAll(response.getData());
      pageNumber++;
    } while (response.getLinks().hasNonNull("next"));
    return workspacesData;
  }

  public List<OrganizationData> getAllOrganizations(TerraformCloudApiTokenCredentials credentials) throws IOException {
    int pageNumber = 1;
    List<OrganizationData> organizationsData = new ArrayList<>();
    TerraformCloudResponse<List<OrganizationData>> response;
    do {
      response = terraformCloudClient.listOrganizations(credentials.getUrl(), credentials.getToken(), pageNumber);
      organizationsData.addAll(response.getData());
      pageNumber++;
    } while (response.getLinks().hasNonNull("next"));
    return organizationsData;
  }

  public void streamLogs(LogCallback logCallback, String logReadUrl) throws IOException {
    int lastIndex = 0;
    boolean isEndOfText = false;
    String incompleteLine = "";

    while (!isEndOfText) {
      int finalLastIndex = lastIndex;
      String logs = Failsafe.with(getRetryPolicy())
                        .get(() -> terraformCloudClient.getLogs(logReadUrl, finalLastIndex, CHUNK_SIZE));
      if (isNotEmpty(logs)) {
        lastIndex = lastIndex + logs.length();
        String[] logLines = (incompleteLine + logs).split("\n");
        for (int i = 0; i < logLines.length - 1; i++) {
          logCallback.saveExecutionLog(logLines[i]);
        }
        if (isEndOfText(logs)) {
          isEndOfText = true;
          logCallback.saveExecutionLog(logLines[logLines.length - 1]);
        } else {
          incompleteLine = logLines[logLines.length - 1];
        }
      }
      if (logs != null && logs.length() < CHUNK_SIZE) {
        sleep(ofSeconds(2));
      }
    }
  }

  private RetryPolicy<Object> getRetryPolicy() {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(Duration.ofSeconds(1))
        .withMaxAttempts(5)
        .onFailedAttempt(event -> log.info("Failed to get logs: {}", event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event
            -> log.error("Failed to get logs after retrying {} times", event.getAttemptCount(), event.getFailure()));
  }

  private boolean isEndOfText(String string) {
    return string.endsWith(String.valueOf((char) 3));
  }

  RunData createRun(String url, String token, RunRequest runRequest, boolean forceExecute,
      TerraformCloudTaskType terraformCloudTaskType, LogCallback logCallback) throws IOException {
    logCallback.saveExecutionLog("Creating execution run ...", LogLevel.INFO, CommandExecutionStatus.RUNNING);
    RunData runData = terraformCloudClient.createRun(url, token, runRequest).getData();
    String runId = runData.getId();
    logCallback.saveExecutionLog(format("Run created: %s", runId), LogLevel.INFO, CommandExecutionStatus.RUNNING);

    if (forceExecute && getRunStatus(url, token, runId) == RunStatus.PENDING) {
      logCallback.saveExecutionLog(format("Force execute: %s", runId), LogLevel.INFO, CommandExecutionStatus.RUNNING);
      terraformCloudClient.forceExecuteRun(url, token, runId);
    }

    String planId = getRelationshipId(runData, "plan");
    PlanData plan = terraformCloudClient.getPlan(url, token, planId).getData();
    // stream plan logs
    logCallback.saveExecutionLog("Plan execution...", LogLevel.INFO, CommandExecutionStatus.RUNNING);
    streamLogs(logCallback, plan.getAttributes().getLogReadUrl());

    runData = getRun(url, token, runId);
    if (terraformCloudTaskType == RUN_PLAN_AND_APPLY || terraformCloudTaskType == RUN_PLAN_AND_DESTROY
        || terraformCloudTaskType == ROLLBACK) {
      streamApplyLogs(url, token, runData, logCallback);
    }

    return runData;
  }

  private void streamApplyLogs(String url, String token, RunData runData, LogCallback logCallback) throws IOException {
    // stream apply logs
    String applyId = getRelationshipId(runData, "apply");
    ApplyData apply = terraformCloudClient.getApply(url, token, applyId).getData();
    if (!apply.getAttributes().getStatus().equals("unreachable")) {
      logCallback.saveExecutionLog(
          format("Apply %s execution...", runData.getId()), LogLevel.INFO, CommandExecutionStatus.RUNNING);
      streamLogs(logCallback, apply.getAttributes().getLogReadUrl());
    }
  }

  public String uploadTfPlanJson(String accountId, String delegateId, String taskId, String entityId, PlanType planType,
      String content) throws IOException {
    final DelegateFile delegateFile =
        aDelegateFile()
            .withAccountId(accountId)
            .withDelegateId(delegateId)
            .withTaskId(taskId)
            .withEntityId(entityId)
            .withBucket(FileBucket.TERRAFORM_PLAN_JSON)
            .withFileName(planType == PlanType.APPLY ? TFC_PLAN_FILE_OUTPUT_NAME : TFC_DESTROY_PLAN_FILE_OUTPUT_NAME)
            .build();

    File file = Files.createTempFile("compressedTfPlan", ".gz").toFile();
    try (FileOutputStream output = new FileOutputStream(file);
         Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), StandardCharsets.UTF_8)) {
      writer.write(content);
    }
    try (InputStream fileStream = new FileInputStream(file)) {
      delegateFileManager.upload(delegateFile, fileStream);
    } finally {
      FileUtils.deleteQuietly(file);
    }
    return delegateFile.getFileId();
  }

  String getRelationshipId(Data data, String relationshipName) {
    // toDo custom exceptions
    return data.getRelationships()
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().equals(relationshipName))
        .findFirst()
        .orElseThrow(()
                         -> new InvalidRequestException(
                             format("Terraform Cloud response invalid. Cant find relationship: %s", relationshipName)))
        .getValue()
        .getData()
        .stream()
        .findFirst()
        .orElseThrow(()
                         -> new InvalidRequestException(format(
                             "Terraform Cloud response invalid. Relationship: %s data is empty", relationshipName)))
        .getId();
  }

  String getApplyOutput(String url, String token, RunData runData) throws IOException {
    ApplyData applyData = terraformCloudClient.getApply(url, token, getRelationshipId(runData, "apply")).getData();
    if (applyData.getAttributes().getStatus().equals("finished")) {
      String svId = getRelationshipId(applyData, "state-versions");

      int pageNumber = 1;
      TerraformCloudResponse<List<StateVersionOutputData>> response;
      List<StateVersionOutputData> stateVersionOutputData = new ArrayList<>();
      do {
        response = terraformCloudClient.getStateVersionOutputs(url, token, svId, pageNumber);
        stateVersionOutputData.addAll(response.getData());
        pageNumber++;
      } while (response.getLinks().hasNonNull("next"));

      return format("{ %s }",
          stateVersionOutputData.stream()
              .map(out
                  -> format("\"%s\" : { \"value\" : %s, \"sensitive\" : %s }", out.getAttributes().getName(),
                      out.getAttributes().getValue(), out.getAttributes().isSensitive()))
              .collect(Collectors.joining(",")));
    }
    return null;
  }

  public String getJsonPlan(String url, String token, RunData runData) throws IOException {
    return terraformCloudClient.getPlanJsonOutput(url, token, getRelationshipId(runData, "plan"));
  }

  public RunStatus getRunStatus(String url, String token, String runId) throws IOException {
    return getRun(url, token, runId).getAttributes().getStatus();
  }

  public String applyRun(String url, String token, String runId, String message, LogCallback logCallback)
      throws IOException {
    terraformCloudClient.applyRun(url, token, runId, RunActionRequest.builder().comment(message).build());
    RunData runData = getRun(url, token, runId);
    streamApplyLogs(url, token, runData, logCallback);
    return getApplyOutput(url, token, runData);
  }

  public RunData getRun(String url, String token, String runId) throws IOException {
    return terraformCloudClient.getRun(url, token, runId).getData();
  }
}
