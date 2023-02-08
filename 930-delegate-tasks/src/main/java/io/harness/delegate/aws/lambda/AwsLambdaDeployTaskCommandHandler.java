/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.aws.lambda;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.lambda.AwsLambdaCommandUnitConstants;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.task.aws.lambda.request.AwsLambdaCommandRequest;
import io.harness.delegate.task.aws.lambda.response.AwsLambdaCommandResponse;
import io.harness.delegate.task.aws.lambda.response.AwsLambdaDeployResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class AwsLambdaDeployTaskCommandHandler extends AwsLambdaCommandTaskHandler {
  @Override
  protected AwsLambdaCommandResponse executeTaskInternal(AwsLambdaCommandRequest awsLambdaCommandRequest,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(awsLambdaCommandRequest instanceof AwsLambdaCommandRequest)) {
      throw new InvalidArgumentsException(Pair.of("awsLambdaCommandRequest",
          "Must be instance of "
              + "AwsLambdaCommandRequest"));
    }

    LogCallback executionLogCallback = new NGDelegateLogCallback(
        iLogStreamingTaskClient, AwsLambdaCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);

    try {
      executionLogCallback.saveExecutionLog(format("Deploying..%n%n"), LogLevel.INFO);
      return AwsLambdaDeployResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    } catch (Exception exception) {
      executionLogCallback.saveExecutionLog(color(format("%n Deployment Failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new InvalidArgumentsException(String.valueOf(exception));
    }
  }
}
