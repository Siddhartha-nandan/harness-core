/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.NotificationRequest;
import io.harness.notification.entities.NotificationEntity;
import io.harness.notification.entities.NotificationEvent;
import io.harness.notification.entities.NotificationRule;
import io.harness.notification.entities.eventmetadata.NotificationEventParameters;
import io.harness.notification.model.NotificationRuleReferenceDTO;
import io.harness.notification.remote.dto.NotificationRequestDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationRuleManagementService;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class ChannelResourceImpl implements ChannelResource {
  private final ChannelService channelService;
  private final NotificationRuleManagementService notificationRuleManagementService;

  public ResponseDTO<Boolean> testNotificationSetting(NotificationSettingDTO notificationSettingDTO) {
    log.info("Received test notification request for {} - notificationId: {}", notificationSettingDTO.getType(),
        notificationSettingDTO.getNotificationId());
    boolean result = channelService.sendTestNotification(notificationSettingDTO);
    return ResponseDTO.newResponse(result);
  }

  public ResponseDTO<NotificationTaskResponse> sendNotification(NotificationRequestDTO notificationRequestDTO)
      throws InvalidProtocolBufferException {
    NotificationRequest notificationRequest = NotificationRequest.parseFrom(notificationRequestDTO.getBytes());
    log.info("Received test notification request for {} - notificationId: {}", notificationRequest.getChannelCase(),
        notificationRequest.getId());
    NotificationTaskResponse taskResponse = channelService.sendSync(notificationRequest);
    return ResponseDTO.newResponse(taskResponse);
  }

  @Override
  public ResponseDTO<NotificationRuleReferenceDTO> notificationRule(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String notificationEntity, String notificationEvent) {
    NotificationEntity notificationEntityEnum = Enum.valueOf(NotificationEntity.class, notificationEntity);
    NotificationEvent notificationEventEnum = Enum.valueOf(NotificationEvent.class, notificationEvent);
    NotificationRule notificationRule = notificationRuleManagementService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, notificationEntityEnum, notificationEventEnum);
    NotificationEventParameters notificationEventParameters =
        notificationRule.getNotificationEventConfigs(notificationEventEnum).get(0).getNotificationEventParameters();
    return ResponseDTO.newResponse(NotificationRuleReferenceDTO.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .notificationEventParameters(notificationEventParameters)
                                       .build());
  }
}