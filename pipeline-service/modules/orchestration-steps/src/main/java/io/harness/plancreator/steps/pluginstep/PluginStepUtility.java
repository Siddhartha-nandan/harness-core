/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.pluginstep;

import io.harness.delegate.beans.scheduler.ExecutionStatus;
import io.harness.delegate.beans.scheduler.InitializeExecutionInfraResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.Status;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PluginStepUtility {
  public void validateInitExecutionInfraResponseAndThrow(InitializeExecutionInfraResponse initResponse) {
    if (initResponse.getStatus() != ExecutionStatus.SUCCESS) {
      throw new InvalidRequestException(
          String.format("Failed to initialize K8s infrastructure, infraRefId: %s, error: %s",
              initResponse.getInfraRefId(), initResponse.getErrorMessage()));
    }
  }

  public Status getStatus(InitializeExecutionInfraResponse initResponse) {
    ExecutionStatus executionStatus = initResponse.getStatus();
    if (executionStatus == ExecutionStatus.SUCCESS) {
      return Status.SUCCEEDED;
    } else if (executionStatus == ExecutionStatus.FAILED) {
      return Status.FAILED;
    }

    return Status.UNRECOGNIZED;
  }
}
