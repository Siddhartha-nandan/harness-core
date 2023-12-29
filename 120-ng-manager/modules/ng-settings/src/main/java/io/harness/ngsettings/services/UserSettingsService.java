/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.services;

import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.UserSettingConfiguration;

import java.util.List;
import java.util.Map;

public interface UserSettingsService {
  SettingValueResponseDTO get(String identifier, String accountIdentifier, String userIdentifier);

  List<UserSettingConfiguration> listDefaultSettings();

  UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration);

  void removeSetting(String identifier);

  List<UserSettingResponseDTO> list(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier);

  List<UserSettingUpdateResponseDTO> update(
      String accountIdentifier, String userIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList);

  Map<String, String> getUserPreferences(String accountIdentifier, String userIdentifier);
}
