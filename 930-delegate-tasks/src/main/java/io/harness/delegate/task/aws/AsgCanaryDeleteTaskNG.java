/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.aws.asg.AsgCanaryDeleteResponse;
import io.harness.delegate.task.aws.asg.AsgCommandResponse;
import io.harness.delegate.task.common.AbstractDelegateRunnableTask;
import io.harness.logging.CommandExecutionStatus;
import io.harness.secret.SecretSanitizerThreadLocal;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.CDP)
public class AsgCanaryDeleteTaskNG extends AbstractDelegateRunnableTask {
  public AsgCanaryDeleteTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);

    SecretSanitizerThreadLocal.addAll(delegateTaskPackage.getSecrets());
  }

  @Override
  public AsgCommandResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AsgCommandResponse run(TaskParameters parameters) {
    // TODO

    return AsgCanaryDeleteResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
