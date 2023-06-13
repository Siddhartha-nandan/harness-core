/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.handlermapping.handlers;

import io.harness.delegate.core.beans.TaskPayload;
import io.harness.delegate.service.handlermapping.context.Context;
import io.harness.delegate.service.runners.RunnersFactory;
import io.harness.delegate.service.runners.itfc.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ExecutionHandler implements Handler {
  private final RunnersFactory runnersFactory;

  @Override
  public void handle(Map<String, String> urlParams, TaskPayload taskPayload, Context context) {
    String runnerType = urlParams.get("runnerType");
    Runner runner = runnersFactory.get(runnerType);
    // TODO: define task response interface. Also, move this to VM/Docker runner, as some tasks directly sends response
    // to delegate agent.
    runner.execute(taskPayload.getExecutionInfraId(), taskPayload.getTaskData(), context);
  }
}
