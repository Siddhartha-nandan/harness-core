/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.Execution;
import io.harness.delegate.ExecutionInfrastructure;
import io.harness.delegate.TaskSelector;
import io.harness.grpc.DelegateServiceGrpcClient;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class BijouDelegateTaskExecutor {
  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  @Inject
  public BijouDelegateTaskExecutor(DelegateServiceGrpcClient delegateServiceGrpcClient,
      Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier) {
    this.delegateServiceGrpcClient = delegateServiceGrpcClient;
    this.delegateCallbackTokenSupplier = delegateCallbackTokenSupplier;
  }

  public String queueInitTask(Scope scope, ExecutionInfrastructure executionInfrastructure,
      List<TaskSelector> selectors, Duration executionTimeout) {
    return Failsafe.with(getRetryPolicy())
        .get(()
                 -> delegateServiceGrpcClient.submitInitTaskAsync(scope, delegateCallbackTokenSupplier.get(),
                     executionInfrastructure, selectors, Duration.ZERO, executionTimeout));
  }

  public String queueExecuteTask(
      String accountId, Execution stepExecution, List<TaskSelector> selectors, Duration executionTimeout) {
    return Failsafe.with(getRetryPolicy())
        .get(()
                 -> delegateServiceGrpcClient.submitExecuteTaskAsync(delegateCallbackTokenSupplier.get(), accountId,
                     stepExecution, Duration.ZERO, selectors, executionTimeout));
  }

  public String queueCleanupTask(String accountId, String infraRefId) {
    return Failsafe.with(getRetryPolicy())
        .get(()
                 -> delegateServiceGrpcClient.submitCleanupTaskAsync(
                     delegateCallbackTokenSupplier.get(), accountId, infraRefId, Duration.ZERO));
  }

  private RetryPolicy<Object> getRetryPolicy() {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event
            -> log.info("[Retrying failed call to submit delegate execute task attempt: {}", event.getAttemptCount(),
                event.getLastFailure()))
        .onFailure(event
            -> log.error("Failed to submit delegate execute task  after retrying {} times", event.getAttemptCount(),
                event.getFailure()));
  }
}
