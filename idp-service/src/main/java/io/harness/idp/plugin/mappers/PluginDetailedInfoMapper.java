/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.beans.PluginInfoEntity;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.PluginDetailedInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.IDP)
@UtilityClass
public class PluginDetailedInfoMapper {
  public PluginDetailedInfo toDTO(PluginInfoEntity pluginInfoEntity, AppConfig appConfig) {
    PluginDetailedInfo pluginDetailedInfo = new PluginDetailedInfo();
    boolean isEnabled = appConfig != null && appConfig.isEnabled();
    pluginDetailedInfo.setPluginDetails(PluginInfoMapper.toDTO(pluginInfoEntity, isEnabled));
    pluginDetailedInfo.setDescription(pluginInfoEntity.getDescription());
    pluginDetailedInfo.setCategory(pluginInfoEntity.getCategory());
    pluginDetailedInfo.setSource(pluginInfoEntity.getSource());
    String config =
        (appConfig != null && appConfig.isEnabled()) ? appConfig.getConfigs() : pluginInfoEntity.getConfig();
    pluginDetailedInfo.setConfig(config);
    pluginDetailedInfo.setLayout(pluginInfoEntity.getLayout());
    return pluginDetailedInfo;
  }
}
