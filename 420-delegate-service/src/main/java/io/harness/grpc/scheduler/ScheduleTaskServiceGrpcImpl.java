/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc.scheduler;

import static io.harness.beans.DelegateTask.DelegateTaskBuilder;
import static io.harness.beans.DelegateTask.builder;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.SchedulingTaskEvent.EventType;

import static java.util.stream.Collectors.toMap;

import io.harness.delegate.CleanupInfraRequest;
import io.harness.delegate.CleanupInfraResponse;
import io.harness.delegate.Execution;
import io.harness.delegate.K8sInfraSpec;
import io.harness.delegate.LogConfig;
import io.harness.delegate.ScheduleTaskRequest;
import io.harness.delegate.ScheduleTaskResponse;
import io.harness.delegate.ScheduleTaskServiceGrpc.ScheduleTaskServiceImplBase;
import io.harness.delegate.SchedulingConfig;
import io.harness.delegate.Secret;
import io.harness.delegate.SetupExecutionInfrastructureRequest;
import io.harness.delegate.SetupExecutionInfrastructureResponse;
import io.harness.delegate.StepSpec;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.NoDelegatesException;
import io.harness.delegate.beans.RunnerType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.core.beans.K8sExecution;
import io.harness.delegate.utils.DelegateTaskMigrationHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.executionInfra.ExecutionInfraNotAvailableException;
import io.harness.executionInfra.ExecutionInfrastructureService;
import io.harness.grpc.scheduler.mapper.EntryPointMapper;
import io.harness.grpc.scheduler.mapper.K8sInfraMapper;
import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.network.SafeHttpCall;
import io.harness.taskclient.TaskClient;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.util.Durations;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ScheduleTaskServiceGrpcImpl extends ScheduleTaskServiceImplBase {
  private final DelegateTaskMigrationHelper delegateTaskMigrationHelper;
  private final TaskClient taskClient;
  private final ExecutionInfrastructureService infraService;
  private final LogStreamingServiceRestClient logServiceClient;

  // TODO: Reuse cache once it's extracted from service classic, at which point this guice binding should be removed
  @Named("logServiceSecret") private final String logServiceSecret;

  @Override
  public void initTask(final SetupExecutionInfrastructureRequest request,
      final StreamObserver<SetupExecutionInfrastructureResponse> responseObserver) {
    if (StringUtils.isEmpty(request.getAccountId())) {
      log.error("accountId is empty");
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("accountId is mandatory").asRuntimeException());
    }

    if (!request.hasConfig()) {
      log.error("Scheduling config is empty");
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("Scheduling config is mandatory").asRuntimeException());
    }

    final SchedulingConfig schedulingConfig = request.getConfig();
    if (RunnerType.RUNNER_TYPE_K8S.equals(schedulingConfig.getRunnerType())) {
      try {
        responseObserver.onNext(sendInitK8SInfraTask(
            request.getAccountId(), request.getInfra().getK8S(), request.getInfra().getLogConfig(), schedulingConfig));
        responseObserver.onCompleted();
      } catch (IOException e) {
        log.error("Failed to fetch logging token for account {} while scheduling task", request.getAccountId(), e);
        responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
      } catch (NoDelegatesException e) {
        log.error("No delegate exception received while submitting the task request for account {}. Reason {}",
            request.getAccountId(), ExceptionUtils.getMessage(e), e);
        responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
      } catch (Exception ex) {
        log.error("Unexpected error occurred while processing submit task request.", ex);
        responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
      }
    } else {
      log.error("Unsupported runner type {}", schedulingConfig.getRunnerType());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("Unsupported runner type " + schedulingConfig.getRunnerType())
              .asRuntimeException());
    }
  }

  @Override
  public void executeTask(
      final ScheduleTaskRequest request, final StreamObserver<ScheduleTaskResponse> responseObserver) {
    final Execution execution = request.getExecution();
    if (StringUtils.isEmpty(request.getAccountId())) {
      log.error("accountId is empty");
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("accountId is mandatory").asRuntimeException());
    }

    if (!request.hasExecution() && StringUtils.isEmpty(execution.getInfraRefId())) {
      log.error("Infra ref id is empty for account {}", request.getAccountId());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("infra_ref_id is mandatory").asRuntimeException());
      return;
    }

    try {
      responseObserver.onNext(sendExecuteTask(request.getAccountId(), execution, request.getConfig()));
      responseObserver.onCompleted();
    } catch (NoDelegatesException e) {
      log.error("No delegate exception found while processing submit task request for account {}. reason {}",
          request.getAccountId(), ExceptionUtils.getMessage(e));
      responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing submit task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void cleanupInfra(
      final CleanupInfraRequest request, final StreamObserver<CleanupInfraResponse> responseObserver) {
    if (StringUtils.isEmpty(request.getAccountId())) {
      log.error("accountId is empty");
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("accountId is mandatory").asRuntimeException());
    }

    if (StringUtils.isEmpty(request.getInfraRefId())) {
      log.error("Infra ref id is empty for account {}", request.getAccountId());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("infra_ref_id is mandatory").asRuntimeException());
      return;
    }

    try {
      responseObserver.onNext(sendCleanupTask(request.getAccountId(), request.getInfraRefId()));
      responseObserver.onCompleted();
    } catch (NoDelegatesException e) {
      log.error("No delegate exception found while processing submit task request for account {}. reason {}",
          request.getAccountId(), ExceptionUtils.getMessage(e));
      responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
    } catch (ExecutionInfraNotAvailableException ex) {
      responseObserver.onError(Status.UNAVAILABLE.withDescription(ex.getMessage()).asRuntimeException());
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing submit task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  private SetupExecutionInfrastructureResponse sendInitK8SInfraTask(final String accountId, final K8sInfraSpec infra,
      final LogConfig logConfig, final SchedulingConfig config) throws IOException {
    final var loggingToken = getLoggingToken(accountId);
    final var taskId = delegateTaskMigrationHelper.generateDelegateTaskUUID();
    final var executionTaskIds =
        infra.getStepsList()
            .stream()
            .map(StepSpec::getStepId)
            .collect(toMap(Function.identity(), stepId -> delegateTaskMigrationHelper.generateDelegateTaskUUID()));

    infraService.createExecutionInfra(accountId, taskId, executionTaskIds, config.getRunnerType());

    final var k8SInfra = K8sInfraMapper.map(infra, executionTaskIds, logConfig, loggingToken);

    final var capabilities = mapSelectorCapability(config);
    capabilities.add(mapRunnerCapability(config));

    final var task = buildDelegateTask(accountId, config, EventType.SETUP, taskId, taskId)
                         .executionCapabilities(capabilities)
                         .runnerData(k8SInfra.toByteArray())
                         // Check if we can do with baseLogKey itself only.
                         .secretsToDecrypt(getSecretsFromK8sInfraSpec(infra))
                         .build();

    taskClient.sendTask(task);

    return SetupExecutionInfrastructureResponse.newBuilder()
        .setTaskId(TaskId.newBuilder().setId(taskId).build())
        .setInfraRefId(taskId)
        .putAllStepTaskIds(executionTaskIds)
        .build();
  }

  private List<String> getSecretsFromK8sInfraSpec(final K8sInfraSpec k8sInfraSpec) {
    return k8sInfraSpec.getStepsList()
        .stream()
        .flatMap(containerSpec -> containerSpec.getSecrets().getSecretsList().stream())
        .map(Secret::getScopeSecretIdentifier)
        .collect(Collectors.toList());
  }

  private ScheduleTaskResponse sendExecuteTask(
      final String accountId, final Execution execution, final SchedulingConfig config) {
    final var taskId = delegateTaskMigrationHelper.generateDelegateTaskUUID();
    final var taskData = execution.getK8S().getInput();

    var k8sExecutionBuilder = K8sExecution.newBuilder().setCallBackToken(taskId).setLogKey(execution.getStepLogKey());
    if (execution.getK8S().getEnvVarOutputsCount() > 0) {
      k8sExecutionBuilder.addAllEnvVarOutputs(
          execution.getK8S().getEnvVarOutputsList().stream().collect(Collectors.toList()));
    }
    if (execution.getK8S().hasEntryPoint()) {
      k8sExecutionBuilder.setEntryPoint(EntryPointMapper.INSTANCE.map(execution.getK8S().getEntryPoint()));
    }

    final var capabilities = mapSelectorCapability(config);
    capabilities.add(mapInfraCapability(accountId, execution.getInfraRefId()));

    final var task = buildDelegateTask(accountId, config, EventType.EXECUTE, execution.getInfraRefId(), taskId)
                         .executionCapabilities(capabilities)
                         .runnerData(k8sExecutionBuilder.build().toByteArray())
                         .taskData(Objects.nonNull(taskData) ? taskData.getData().toByteArray() : null)
                         .build();

    taskClient.sendTask(task);

    return ScheduleTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build();
  }

  private CleanupInfraResponse sendCleanupTask(final String accountId, final String infraRefId) {
    final var taskId = delegateTaskMigrationHelper.generateDelegateTaskUUID();
    final var executionInfra = infraService.getExecutionInfra(accountId, infraRefId);

    final var task = builder()
                         .uuid(taskId)
                         .eventType(EventType.CLEANUP.name())
                         .infraId(infraRefId)
                         .runnerType(executionInfra.getRunnerType())
                         .waitId(taskId)
                         .accountId(accountId)
                         .selectionLogsTrackingEnabled(true)
                         .executionTimeout(Duration.ofMinutes(10).toMillis())
                         .executeOnHarnessHostedDelegates(false)
                         .emitEvent(false)
                         .async(true)
                         .forceExecute(false)
                         .build();

    taskClient.sendTask(task);

    return CleanupInfraResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build();
  }

  private static DelegateTaskBuilder buildDelegateTask(final String accountId, final SchedulingConfig config,
      final EventType eventType, final String infraRefId, final String taskId) {
    return builder()
        .uuid(taskId)
        .eventType(eventType.name())
        .runnerType(config.getRunnerType())
        .infraId(infraRefId)
        .driverId(config.hasCallbackToken() ? config.getCallbackToken().getToken() : null)
        .waitId(taskId)
        .accountId(accountId)
        .orgId(config.getOrgId())
        .projectId(config.getProjectId())
        .selectionLogsTrackingEnabled(config.getSelectionTrackingLogEnabled())
        .executionTimeout(Durations.toMillis(config.getExecutionTimeout()))
        .executeOnHarnessHostedDelegates(false)
        .emitEvent(false)
        .async(true)
        .forceExecute(false);
  }

  private static SelectorCapability mapRunnerCapability(final SchedulingConfig schedulingConfig) {
    return SelectorCapability.builder().selectors(Set.of(schedulingConfig.getRunnerType())).build();
  }

  private List<ExecutionCapability> mapSelectorCapability(final SchedulingConfig schedulingConfig) {
    return schedulingConfig.getSelectorsList()
        .stream()
        .filter(s -> isNotEmpty(s.getSelector()))
        .map(this::toSelectorCapability)
        .collect(Collectors.toList());
  }

  private ExecutionCapability mapInfraCapability(final String accountId, final String infraRefId) {
    final var locationInfo = infraService.getExecutionInfra(accountId, infraRefId);
    if (Objects.isNull(locationInfo.getDelegateGroupName())) {
      final var errMsg = String.format("Execution infra %s is not ready", infraRefId);
      log.error(errMsg);
      throw new ExecutionInfraNotAvailableException(errMsg);
    }
    return SelectorCapability.builder().selectors(Set.of(locationInfo.getDelegateGroupName())).build();
  }

  private String getLoggingToken(final String accountId) throws IOException {
    return SafeHttpCall.executeWithExceptions(logServiceClient.retrieveAccountToken(logServiceSecret, accountId));
  }

  private SelectorCapability toSelectorCapability(TaskSelector taskSelector) {
    final var origin = isNotEmpty(taskSelector.getOrigin()) ? taskSelector.getOrigin() : "default";
    return SelectorCapability.builder()
        .selectors(Sets.newHashSet(taskSelector.getSelector()))
        .selectorOrigin(origin)
        .build();
  }
}