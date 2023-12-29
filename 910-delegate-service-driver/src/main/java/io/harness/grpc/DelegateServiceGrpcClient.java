/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.Scope;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.AccountId;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.Capability;
import io.harness.delegate.CleanupInfraRequest;
import io.harness.delegate.CleanupInfraResponse;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.Execution;
import io.harness.delegate.ExecutionInfrastructure;
import io.harness.delegate.ObtainDocumentRequest;
import io.harness.delegate.ObtainDocumentResponse;
import io.harness.delegate.PerpetualTaskInfoRequest;
import io.harness.delegate.PerpetualTaskInfoResponse;
import io.harness.delegate.RegisterCallbackRequest;
import io.harness.delegate.RegisterCallbackResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.ScheduleTaskRequest;
import io.harness.delegate.ScheduleTaskResponse;
import io.harness.delegate.ScheduleTaskServiceGrpc.ScheduleTaskServiceBlockingStub;
import io.harness.delegate.SchedulingConfig;
import io.harness.delegate.SetupExecutionInfrastructureRequest;
import io.harness.delegate.SetupExecutionInfrastructureResponse;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.SupportedTaskTypeRequest;
import io.harness.delegate.SupportedTaskTypeResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskLogAbstractions;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.RunnerType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.tasks.ResponseData;

import software.wings.beans.SerializationFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceGrpcClient {
  private final DelegateServiceBlockingStub delegateServiceBlockingStub;
  private final ScheduleTaskServiceBlockingStub scheduleTaskServiceBlockingStub;
  private final DelegateAsyncService delegateAsyncService;
  private final KryoSerializer referenceFalseKryoSerializer;
  private final DelegateSyncService delegateSyncService;
  private final boolean isDriverInstalledInNgService;
  @Inject private ObjectMapper objectMapper;

  @Inject
  public DelegateServiceGrpcClient(DelegateServiceBlockingStub delegateServiceBlockingStub,
      DelegateAsyncService delegateAsyncService,
      @Named("referenceFalseKryoSerializer") KryoSerializer referenceFalseKryoSerializer,
      DelegateSyncService delegateSyncService,
      @Named("driver-installed-in-ng-service") BooleanSupplier isDriverInstalledInNgService,
      ScheduleTaskServiceBlockingStub scheduleTaskServiceBlockingStub) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
    this.delegateAsyncService = delegateAsyncService;
    this.referenceFalseKryoSerializer = referenceFalseKryoSerializer;
    this.delegateSyncService = delegateSyncService;
    this.isDriverInstalledInNgService = isDriverInstalledInNgService.getAsBoolean();
    this.scheduleTaskServiceBlockingStub = scheduleTaskServiceBlockingStub;
  }

  public String submitAsyncTaskV2(DelegateTaskRequest taskRequest, DelegateCallbackToken delegateCallbackToken,
      Duration holdFor, Boolean delegateSelectionTrackingLogEnabled) {
    final SubmitTaskResponse submitTaskResponse = submitTaskInternalV2(
        TaskMode.ASYNC, taskRequest, delegateCallbackToken, holdFor, delegateSelectionTrackingLogEnabled);
    return submitTaskResponse.getTaskId().getId();
  }

  public <T extends ResponseData> Pair<String, T> executeSyncTaskReturningResponseDataV2(
      DelegateTaskRequest taskRequest, DelegateCallbackToken delegateCallbackToken) {
    final SubmitTaskResponse submitTaskResponse =
        submitTaskInternalV2(TaskMode.SYNC, taskRequest, delegateCallbackToken, Duration.ZERO, false);
    final String taskId = submitTaskResponse.getTaskId().getId();
    return Pair.of(taskId,
        delegateSyncService.waitForTask(taskId,
            HarnessStringUtils.defaultIfEmpty(taskRequest.getTaskDescription(), taskRequest.getTaskType()),
            Duration.ofMillis(HTimestamps.toMillis(submitTaskResponse.getTotalExpiry()) - currentTimeMillis()), null));
  }

  public SubmitTaskResponse submitTaskV2(DelegateCallbackToken delegateCallbackToken, AccountId accountId,
      TaskSetupAbstractions taskSetupAbstractions, TaskLogAbstractions taskLogAbstractions, TaskDetails taskDetails,
      List<ExecutionCapability> capabilities, List<String> taskSelectors, Duration holdFor, boolean forceExecute,
      boolean executeOnHarnessHostedDelegates, List<String> eligibleToExecuteDelegateIds, boolean emitEvent,
      String stageId, Boolean delegateSelectionTrackingLogEnabled, List<TaskSelector> selectors) {
    try {
      if (taskSetupAbstractions == null || taskSetupAbstractions.getValuesCount() == 0) {
        Map<String, String> setupAbstractions = new HashMap<>();
        setupAbstractions.put("ng", String.valueOf(isDriverInstalledInNgService));

        taskSetupAbstractions = TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build();
      } else if (taskSetupAbstractions.getValuesMap().get("ng") == null) {
        // This should allow a consumer of the client to override the value, if the one provided by this client is not
        // appropriate
        taskSetupAbstractions = TaskSetupAbstractions.newBuilder()
                                    .putAllValues(taskSetupAbstractions.getValuesMap())
                                    .putValues("ng", String.valueOf(isDriverInstalledInNgService))
                                    .build();
      }

      SubmitTaskRequest.Builder submitTaskRequestBuilder =
          SubmitTaskRequest.newBuilder()
              .setCallbackToken(delegateCallbackToken)
              .setAccountId(accountId)
              .setSetupAbstractions(taskSetupAbstractions)
              .setLogAbstractions(taskLogAbstractions)
              .setExecuteOnHarnessHostedDelegates(executeOnHarnessHostedDelegates)
              .setEmitEvent(emitEvent)
              .setSelectionTrackingLogEnabled(delegateSelectionTrackingLogEnabled)
              .setDetails(taskDetails)
              .setForceExecute(forceExecute);

      if (isNotEmpty(stageId)) {
        submitTaskRequestBuilder.setStageId(stageId);
      }

      if (isNotEmpty(capabilities)) {
        submitTaskRequestBuilder.addAllCapabilities(
            capabilities.stream()
                .map(capability
                    -> Capability.newBuilder()
                           .setKryoCapability(
                               ByteString.copyFrom(referenceFalseKryoSerializer.asDeflatedBytes(capability)))
                           .build())
                .collect(toList()));
      }

      if (isNotEmpty(selectors)) {
        submitTaskRequestBuilder.addAllSelectors(selectors);
      } else if (isNotEmpty(taskSelectors)) {
        submitTaskRequestBuilder.addAllSelectors(
            taskSelectors.stream()
                .map(selector -> TaskSelector.newBuilder().setSelector(selector).build())
                .collect(toList()));
      }

      if (isNotEmpty(eligibleToExecuteDelegateIds)) {
        submitTaskRequestBuilder.addAllEligibleToExecuteDelegateIds(
            eligibleToExecuteDelegateIds.stream().collect(toList()));
      }

      SubmitTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                        .submitTaskV2(submitTaskRequestBuilder.build());

      if (taskDetails.getMode() == TaskMode.ASYNC) {
        delegateAsyncService.setupTimeoutForTask(response.getTaskId().getId(),
            Timestamps.toMillis(response.getTotalExpiry()), currentTimeMillis() + holdFor.toMillis());
      }

      return response;
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() != null && isNotEmpty(ex.getStatus().getDescription())) {
        throw new DelegateServiceDriverException(ex.getStatus().getDescription());
      }
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting task.", ex);
    }
  }

  private SubmitTaskResponse submitTaskInternalV2(TaskMode taskMode, DelegateTaskRequest taskRequest,
      DelegateCallbackToken delegateCallbackToken, Duration holdFor, Boolean delegateSelectionTrackingLogEnabled) {
    final TaskParameters taskParameters = taskRequest.getTaskParameters();

    final List<ExecutionCapability> capabilities = (taskParameters instanceof ExecutionCapabilityDemander)
        ? ListUtils.emptyIfNull(((ExecutionCapabilityDemander) taskParameters).fetchRequiredExecutionCapabilities(null))
        : Collections.emptyList();

    TaskDetails.Builder taskDetailsBuilder =
        TaskDetails.newBuilder()
            .setParked(taskRequest.isParked())
            .setMode(taskMode)
            .setExpressionFunctorToken(taskRequest.getExpressionFunctorToken())
            .setType(TaskType.newBuilder().setType(taskRequest.getTaskType()).build())
            .setExecutionTimeout(Durations.fromSeconds(taskRequest.getExecutionTimeout().getSeconds()));

    if (taskRequest.getSerializationFormat().equals(SerializationFormat.JSON)) {
      try {
        taskDetailsBuilder.setJsonParameters(ByteString.copyFrom(objectMapper.writeValueAsBytes(taskParameters)));
      } catch (JsonProcessingException e) {
        throw new InvalidRequestException("Could not serialize the task request", e);
      }
    } else {
      taskDetailsBuilder.setKryoParameters(
          ByteString.copyFrom(referenceFalseKryoSerializer.asDeflatedBytes(taskParameters)));
    }
    TaskLogAbstractions.Builder builder =
        TaskLogAbstractions.newBuilder().putAllValues(getAbstractionsMap(taskRequest.getLogStreamingAbstractions()));

    builder.setShouldSkipOpenStream(taskRequest.isShouldSkipOpenStream());
    builder.setBaseLogKey(taskRequest.getBaseLogKey() == null ? "" : taskRequest.getBaseLogKey());

    return submitTaskV2(delegateCallbackToken, AccountId.newBuilder().setId(taskRequest.getAccountId()).build(),
        TaskSetupAbstractions.newBuilder()
            .putAllValues(getAbstractionsMap(taskRequest.getTaskSetupAbstractions()))
            .build(),
        builder.build(), taskDetailsBuilder.build(), capabilities, taskRequest.getTaskSelectors(), holdFor,
        taskRequest.isForceExecute(), taskRequest.isExecuteOnHarnessHostedDelegates(),
        taskRequest.getEligibleToExecuteDelegateIds(), taskRequest.isEmitEvent(), taskRequest.getStageId(),
        delegateSelectionTrackingLogEnabled, taskRequest.getSelectors());
  }

  public String submitInitTaskAsync(Scope scope, DelegateCallbackToken delegateCallbackToken,
      ExecutionInfrastructure executionInfrastructure, List<TaskSelector> selectors, Duration holdFor,
      Duration executionTimeout) {
    SetupExecutionInfrastructureResponse setupExecutionInfrastructureResponse = submitInitTask(
        scope, delegateCallbackToken, executionInfrastructure, selectors, TaskMode.ASYNC, holdFor, executionTimeout);
    return setupExecutionInfrastructureResponse.getTaskId().getId();
  }

  public SetupExecutionInfrastructureResponse submitInitTask(Scope scope, DelegateCallbackToken delegateCallbackToken,
      ExecutionInfrastructure executionInfrastructure, List<TaskSelector> selectors, TaskMode mode, Duration holdFor,
      Duration executionTimeout) {
    SchedulingConfig schedulingConfig =
        SchedulingConfig.newBuilder()
            .setCallbackToken(delegateCallbackToken)
            .addAllSelectors(CollectionUtils.emptyIfNull(selectors))
            .setRunnerType(RunnerType.RUNNER_TYPE_K8S)
            .setOrgId(scope.getOrgIdentifier())
            .setProjectId(scope.getProjectIdentifier())
            .setExecutionTimeout(
                com.google.protobuf.Duration.newBuilder().setSeconds(executionTimeout.getSeconds()).build())
            .setSelectionTrackingLogEnabled(true)
            .build();

    SetupExecutionInfrastructureRequest setupExecutionInfrastructureRequest =
        SetupExecutionInfrastructureRequest.newBuilder()
            .setConfig(schedulingConfig)
            .setAccountId(scope.getAccountIdentifier())
            .setInfra(executionInfrastructure)
            .build();

    try {
      return scheduleTaskServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .initTask(setupExecutionInfrastructureRequest);
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() != null && isNotEmpty(ex.getStatus().getDescription())) {
        throw new DelegateServiceDriverException(ex.getStatus().getDescription());
      }
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting init task.", ex);
    }
  }

  public String submitExecuteTaskAsync(DelegateCallbackToken delegateCallbackToken, String accountId,
      Execution stepExecution, Duration holdFor, List<TaskSelector> selectors, Duration executionTimeout) {
    ScheduleTaskResponse scheduleTaskResponse = submitExecuteTask(
        delegateCallbackToken, accountId, stepExecution, TaskMode.ASYNC, holdFor, selectors, executionTimeout);
    return scheduleTaskResponse.getTaskId().getId();
  }

  public ScheduleTaskResponse submitExecuteTask(DelegateCallbackToken delegateCallbackToken, String accountId,
      Execution stepExecution, TaskMode mode, Duration holdFor, List<TaskSelector> selectors,
      Duration executionTimeout) {
    SchedulingConfig schedulingConfig =
        SchedulingConfig.newBuilder()
            .addAllSelectors(CollectionUtils.emptyIfNull(selectors))
            .setRunnerType(RunnerType.RUNNER_TYPE_K8S)
            .setCallbackToken(delegateCallbackToken)
            .setExecutionTimeout(
                com.google.protobuf.Duration.newBuilder().setSeconds(executionTimeout.getSeconds()).build())
            .setSelectionTrackingLogEnabled(true)
            .build();

    ScheduleTaskRequest scheduleTaskRequest = ScheduleTaskRequest.newBuilder()
                                                  .setExecution(stepExecution)
                                                  .setAccountId(accountId)
                                                  .setConfig(schedulingConfig)
                                                  .build();

    try {
      return scheduleTaskServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).executeTask(scheduleTaskRequest);
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() != null && isNotEmpty(ex.getStatus().getDescription())) {
        throw new DelegateServiceDriverException(ex.getStatus().getDescription());
      }
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting execute task.", ex);
    }
  }

  public String submitCleanupTaskAsync(
      DelegateCallbackToken delegateCallbackToken, String accountId, String infraRefId, Duration holdFor) {
    CleanupInfraResponse cleanupInfraResponse =
        submitCleanupTask(delegateCallbackToken, accountId, infraRefId, TaskMode.ASYNC, holdFor);
    return cleanupInfraResponse.getTaskId().getId();
  }

  public CleanupInfraResponse submitCleanupTask(DelegateCallbackToken delegateCallbackToken, String accountId,
      String infraRefId, TaskMode mode, Duration holdFor) {
    CleanupInfraRequest cleanupInfraRequest =
        CleanupInfraRequest.newBuilder().setAccountId(accountId).setInfraRefId(infraRefId).build();
    try {
      return scheduleTaskServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).cleanupInfra(cleanupInfraRequest);
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() != null && isNotEmpty(ex.getStatus().getDescription())) {
        throw new DelegateServiceDriverException(ex.getStatus().getDescription());
      }
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting cleanup task.", ex);
    }
  }

  public TaskExecutionStage cancelTaskV2(AccountId accountId, TaskId taskId) {
    try {
      CancelTaskResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .cancelTaskV2(CancelTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

      return response.getCanceledAtStage();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while cancelling task.", ex);
    }
  }

  public TaskExecutionStage taskProgress(AccountId accountId, TaskId taskId) {
    try {
      TaskProgressResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .taskProgress(TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

      return response.getCurrentlyAtStage();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while checking task progress.", ex);
    }
  }

  public void taskProgressUpdate(
      AccountId accountId, TaskId taskId, Consumer<TaskExecutionStage> taskExecutionStageConsumer) {
    throw new NotImplementedException(
        "Temporarily removed the implementation until we find more effective way of doing this.");
  }

  public PerpetualTaskId createPerpetualTask(AccountId accountId, String type, PerpetualTaskSchedule schedule,
      PerpetualTaskClientContextDetails context, boolean allowDuplicate, String taskDescription) {
    try {
      CreatePerpetualTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                                 .createPerpetualTask(CreatePerpetualTaskRequest.newBuilder()
                                                                          .setAccountId(accountId)
                                                                          .setType(type)
                                                                          .setSchedule(schedule)
                                                                          .setContext(context)
                                                                          .setAllowDuplicate(allowDuplicate)
                                                                          .setTaskDescription(taskDescription)
                                                                          .build());

      return response.getPerpetualTaskId();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while creating perpetual task.", ex);
    }
  }

  public PerpetualTaskId createPerpetualTask(AccountId accountId, String type, PerpetualTaskSchedule schedule,
      PerpetualTaskClientContextDetails context, boolean allowDuplicate, String taskDescription, String clientTaskId) {
    try {
      CreatePerpetualTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                                 .createPerpetualTask(CreatePerpetualTaskRequest.newBuilder()
                                                                          .setAccountId(accountId)
                                                                          .setType(type)
                                                                          .setClientTaskId(clientTaskId)
                                                                          .setSchedule(schedule)
                                                                          .setContext(context)
                                                                          .setAllowDuplicate(allowDuplicate)
                                                                          .setTaskDescription(taskDescription)
                                                                          .build());

      return response.getPerpetualTaskId();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while creating perpetual task.", ex);
    }
  }

  public void deletePerpetualTask(AccountId accountId, PerpetualTaskId perpetualTaskId) {
    try {
      delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .deletePerpetualTask(DeletePerpetualTaskRequest.newBuilder()
                                   .setAccountId(accountId)
                                   .setPerpetualTaskId(perpetualTaskId)
                                   .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while deleting perpetual task.", ex);
    }
  }

  public void resetPerpetualTask(
      AccountId accountId, PerpetualTaskId perpetualTaskId, PerpetualTaskExecutionBundle taskExecutionBundle) {
    try {
      delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .resetPerpetualTask(ResetPerpetualTaskRequest.newBuilder()
                                  .setAccountId(accountId)
                                  .setPerpetualTaskId(perpetualTaskId)
                                  .setTaskExecutionBundle(taskExecutionBundle)
                                  .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while resetting perpetual task.", ex);
    }
  }

  public PerpetualTaskInfoResponse getPerpetualTask(TaskId taskId) {
    try {
      return delegateServiceBlockingStub.getPerpetualTask(
          PerpetualTaskInfoRequest.newBuilder().setTaskId(taskId.getId()).build());

    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while getting the perpetual task.", ex);
    }
  }

  public DelegateCallbackToken registerCallback(DelegateCallback delegateCallback) {
    try {
      RegisterCallbackResponse response = delegateServiceBlockingStub.registerCallback(
          RegisterCallbackRequest.newBuilder().setCallback(delegateCallback).build());
      return response.getCallbackToken();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while registering callback.", ex);
    }
  }

  public ObtainDocumentResponse obtainDocument(ObtainDocumentRequest request) {
    return delegateServiceBlockingStub.obtainDocument(request);
  }

  public boolean isTaskTypeSupported(AccountId accountId, TaskType taskType) {
    try {
      SupportedTaskTypeResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                               .isTaskTypeSupported(SupportedTaskTypeRequest.newBuilder()
                                                                        .setAccountId(accountId.getId())
                                                                        .setTaskType(taskType.getType())
                                                                        .build());

      return response.getIsTaskTypeSupported();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while checking if task is supported.", ex);
    }
  }

  static Map<String, String> getAbstractionsMap(Map<String, String> map) {
    return MapUtils.emptyIfNull(map)
        .entrySet()
        .stream()
        .filter(entry -> !Objects.isNull(entry.getValue()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
