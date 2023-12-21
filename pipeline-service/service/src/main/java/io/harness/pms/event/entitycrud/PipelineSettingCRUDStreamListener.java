/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.event.entitycrud;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SETTINGS;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SETTINGS_CATEGORY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entity_crud.settings.SettingsEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngtriggers.service.NGTriggerService;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;

public class PipelineSettingCRUDStreamListener implements MessageListener {
  private final NGTriggerService ngTriggerService;
  public String MANIFEST_COLLECTION_NG_INTERVAL_MINUTES = "manifest_collection_ng_interval_minutes";
  public String ARTIFACT_COLLECTION_NG_INTERVAL_MINUTES = "artifact_collection_ng_interval_minutes";
  @Inject
  public PipelineSettingCRUDStreamListener(NGTriggerService ngTriggerService) {
    this.ngTriggerService = ngTriggerService;
  }
  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null && isPipelineSettingEvent(metadataMap)) {
        SettingsEntityChangeDTO settingsEntityChangeDTO = getSettingsEntityChangeDTO(message);
        String action = metadataMap.get(ACTION);
        if (action != null) {
          return processSettingsChangeEvent(settingsEntityChangeDTO, action);
        }
      }
    }
    return true;
  }

  private boolean isPipelineSettingEvent(final Map<String, String> metadataMap) {
    return metadataMap != null && SETTINGS.equals(metadataMap.get(ENTITY_TYPE))
        && SettingCategory.PMS.name().equals(metadataMap.get(SETTINGS_CATEGORY));
  }

  private boolean processSettingsChangeEvent(SettingsEntityChangeDTO settingsEntityChangeDTO, String action) {
    switch (action) {
      case UPDATE_ACTION:
        return processUpdateEvent(settingsEntityChangeDTO);
      default:
    }
    return true;
  }

  private boolean processUpdateEvent(SettingsEntityChangeDTO settingsEntityChangeDTO) {
    if (settingsEntityChangeDTO.getSettingIdentifiersMap().containsKey(ARTIFACT_COLLECTION_NG_INTERVAL_MINUTES)) {
      return resetPollingInterval(settingsEntityChangeDTO, "ARTIFACT");
    } else if (settingsEntityChangeDTO.getSettingIdentifiersMap().containsKey(
                   MANIFEST_COLLECTION_NG_INTERVAL_MINUTES)) {
      return resetPollingInterval(settingsEntityChangeDTO, "MANIFEST");
    }
    return true;
  }

  private boolean resetPollingInterval(SettingsEntityChangeDTO settingsEntityChangeDTO, String triggerType) {
    ngTriggerService.updatePollingInterval(settingsEntityChangeDTO.getAccountIdentifier().getValue(),
        settingsEntityChangeDTO.getOrgIdentifier().getValue(),
        settingsEntityChangeDTO.getProjectIdentifier().getValue(), triggerType);
    return true;
  }

  private SettingsEntityChangeDTO getSettingsEntityChangeDTO(final Message message) {
    SettingsEntityChangeDTO settingsEntityChangeDTO;
    try {
      settingsEntityChangeDTO = SettingsEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (final InvalidProtocolBufferException ex) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking SettingsEntityChangeDTO for key %s", message.getId()), ex);
    }
    return settingsEntityChangeDTO;
  }
}
