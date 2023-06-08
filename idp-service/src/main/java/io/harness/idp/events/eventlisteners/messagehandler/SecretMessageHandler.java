/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.events.eventlisteners.messagehandler;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.envvariable.service.BackstageEnvVariableService;
import io.harness.idp.events.eventlisteners.utility.EventListenerLogger;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class SecretMessageHandler implements EventMessageHandler {
  private static final String ACCOUNT_ID = "accountId";
  private BackstageEnvVariableService backstageEnvVariableService;

  @Override
  public void handleMessage(Message message, String action) {
    EntityChangeDTO entityChangeDTO;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for id %s", message.getId()), e);
    }
    EventListenerLogger.logForEventReceived(message);
    if (entityChangeDTO != null) {
      switch (action) {
        case UPDATE_ACTION:
          backstageEnvVariableService.processSecretUpdate(entityChangeDTO);
          break;
        default:
          log.warn("ACTION - {} is not to be handled by IDP secret event handler", action);
      }
    }
  }
}
