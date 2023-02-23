/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.ecs;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.EcsNGException;
import io.harness.delegate.task.ecs.EcsTaskHelperBase;
import io.harness.delegate.task.ecs.request.EcsCanaryDeployRequest;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.google.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class EcsCanaryDeployCommandTaskHandler extends EcsCommandTaskNGHandler {
  @Inject private EcsTaskHelperBase ecsTaskHelperBase;
  @Inject private EcsDeploymentHelper ecsDeploymentHelper;

  @Override
  protected EcsCommandResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(ecsCommandRequest instanceof EcsCanaryDeployRequest)) {
      throw new InvalidArgumentsException(Pair.of("ecsCommandRequest", "Must be instance of EcsCanaryDeployRequest"));
    }
    EcsCanaryDeployRequest ecsCanaryDeployRequest = (EcsCanaryDeployRequest) ecsCommandRequest;

    LogCallback deployLogCallback = ecsTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, EcsCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    try {
      CreateServiceRequest createServiceRequest = ecsDeploymentHelper.createServiceDefinitionRequest(deployLogCallback,
          ecsCommandRequest.getEcsInfraConfig(), ecsCanaryDeployRequest.getEcsTaskDefinitionManifestContent(),
          ecsCanaryDeployRequest.getEcsServiceDefinitionManifestContent(),
          ecsCanaryDeployRequest.getEcsScalableTargetManifestContentList(),
          ecsCanaryDeployRequest.getEcsScalingPolicyManifestContentList(), null);
      return ecsDeploymentHelper.deployCanaryService(deployLogCallback, createServiceRequest, ecsCommandRequest,
          ecsCanaryDeployRequest.getEcsScalableTargetManifestContentList(),
          ecsCanaryDeployRequest.getEcsScalingPolicyManifestContentList(),
          ecsCanaryDeployRequest.getDesiredCountOverride(), ecsCanaryDeployRequest.getEcsServiceNameSuffix());
    } catch (Exception ex) {
      deployLogCallback.saveExecutionLog(color(format("%n Deployment Failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new EcsNGException(ex);
    }
  }
}
