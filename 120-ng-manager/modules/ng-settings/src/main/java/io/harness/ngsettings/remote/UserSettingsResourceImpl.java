/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.remote;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.utils.UserHelperService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class UserSettingsResourceImpl implements UserSettingResource {
  UserSettingsService userSettingsService;
  private final UserHelperService userHelperService;

  @Override
  public ResponseDTO<SettingValueResponseDTO> get(String identifier, String accountIdentifier) {
    return ResponseDTO.newResponse(
        userSettingsService.get(identifier, accountIdentifier, userHelperService.getUserId()));
  }

  @Override
  public ResponseDTO<List<UserSettingResponseDTO>> list(
      String accountIdentifier, SettingCategory category, String groupIdentifier) {
    return ResponseDTO.newResponse(
        userSettingsService.list(accountIdentifier, userHelperService.getUserId(), category, groupIdentifier));
  }

  @Override
  public ResponseDTO<List<UserSettingUpdateResponseDTO>> update(
      String accountIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList) {
    return ResponseDTO.newResponse(
        userSettingsService.update(accountIdentifier, userHelperService.getUserId(), userSettingRequestDTOList));
  }

  @Override
  public ResponseDTO<Map<String, String>> userPreferences(String accountIdentifier) {
    return ResponseDTO.newResponse(
        userSettingsService.getUserPreferences(accountIdentifier, userHelperService.getUserId()));
  }
}
