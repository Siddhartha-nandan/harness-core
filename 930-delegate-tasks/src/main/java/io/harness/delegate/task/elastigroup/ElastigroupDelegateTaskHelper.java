/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.elastigroup;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.ecs.EcsCommandTaskNGHandler;
import io.harness.delegate.elastigroup.ElastigroupCommandTaskNGHandler;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.ecs.EcsInfraConfigHelper;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.delegate.task.elastigroup.request.ElastigroupCommandRequest;
import io.harness.delegate.task.elastigroup.response.ElastigroupCommandResponse;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ElastigroupDelegateTaskHelper {
  @Inject private Map<String, ElastigroupCommandTaskNGHandler> commandTaskTypeToTaskHandlerMap;

  public ElastigroupCommandResponse getElastigroupCommandResponse(
          ElastigroupCommandRequest elastigroupCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient) {
    CommandUnitsProgress commandUnitsProgress = elastigroupCommandRequest.getCommandUnitsProgress() != null
        ? elastigroupCommandRequest.getCommandUnitsProgress()
        : CommandUnitsProgress.builder().build();
    log.info("Starting task execution for command: {}", elastigroupCommandRequest.getEcsCommandType().name());
//    decryptRequestDTOs(elastigroupCommandRequest);

    ElastigroupCommandTaskNGHandler commandTaskHandler =
        commandTaskTypeToTaskHandlerMap.get(elastigroupCommandRequest.getEcsCommandType().name());
    try {
      ElastigroupCommandResponse elastigroupCommandResponse =
          commandTaskHandler.executeTask(elastigroupCommandRequest, iLogStreamingTaskClient, commandUnitsProgress);
      elastigroupCommandResponse.setCommandUnitsProgress(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
      return elastigroupCommandResponse;
    } catch (Exception e) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      log.error("Exception in processing elastigroup task [{}]",
          elastigroupCommandRequest.getCommandName() + ":" + elastigroupCommandRequest.getEcsCommandType(), sanitizedException);
      throw new TaskNGDataException(
          UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress), sanitizedException);
    }
  }

//  private void decryptRequestDTOs(EcsCommandRequest ecsCommandRequest) {
//    ecsInfraConfigHelper.decryptEcsInfraConfig(ecsCommandRequest.getEcsInfraConfig());
//  }
}
