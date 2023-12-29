/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.utils;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.scheduler.ExecutionStatus;
import io.harness.delegate.beans.scheduler.InitializeExecutionInfraResponse;
import io.harness.eraro.Level;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class DelegateResponseUtils {
  public static StepResponse prepareFailureResponse(Exception ex) {
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(ExceptionUtils.getMessage(ex))
                                  .build();
    return StepResponse.builder()
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().addFailureData(failureData).build())
        .build();
  }

  public static DelegateResponseData getDelegateResponseDataAndThrow(Map<String, ResponseData> responseDataMap) {
    Iterator<ResponseData> dataIterator = responseDataMap.values().iterator();
    if (!dataIterator.hasNext()) {
      throw new InvalidRequestException("No Delegate Response received. Failed to complete Infrastructure step.");
    }
    return (DelegateResponseData) dataIterator.next();
  }

  @NotNull
  public static List<ErrorNotifyResponseData> getErrorNotifyResponseData(Map<String, ResponseData> responseDataMap) {
    return responseDataMap.values()
        .stream()
        .filter(ErrorNotifyResponseData.class ::isInstance)
        .map(ErrorNotifyResponseData.class ::cast)
        .collect(Collectors.toList());
  }
  public static void validateInitExecutionInfraResponseAndThrow(InitializeExecutionInfraResponse initResponse) {
    if (initResponse.getStatus() != ExecutionStatus.SUCCESS) {
      throw new InvalidRequestException(
          String.format("Failed to initialize K8s infrastructure, infraRefId: %s, error: %s",
              initResponse.getInfraRefId(), initResponse.getErrorMessage()));
    }
  }

  public static Status getStatus(InitializeExecutionInfraResponse initResponse) {
    ExecutionStatus executionStatus = initResponse.getStatus();
    if (executionStatus == ExecutionStatus.SUCCESS) {
      return Status.SUCCEEDED;
    } else if (executionStatus == ExecutionStatus.FAILED) {
      return Status.FAILED;
    }

    return Status.UNRECOGNIZED;
  }
}
