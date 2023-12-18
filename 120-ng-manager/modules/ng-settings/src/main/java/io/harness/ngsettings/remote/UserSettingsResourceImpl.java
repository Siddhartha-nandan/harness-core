package io.harness.ngsettings.remote;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.services.UserSettingsService;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class UserSettingsResourceImpl implements UserSettingResource {
  UserSettingsService userSettingsService;

  @Override
  public ResponseDTO<SettingValueResponseDTO> get(String identifier, String accountIdentifier, String userIdentifier) {
    return ResponseDTO.newResponse(
        userSettingsService.get(identifier, accountIdentifier, userIdentifier));
  }

  @Override
  public ResponseDTO<List<UserSettingResponseDTO>> list(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    return ResponseDTO.newResponse(
        userSettingsService.list(accountIdentifier, userIdentifier, category, groupIdentifier));
  }

  @Override
  public ResponseDTO<List<UserSettingUpdateResponseDTO>> update(
      String accountIdentifier, String userIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList) {
    return ResponseDTO.newResponse(userSettingsService.update(accountIdentifier,userIdentifier,userSettingRequestDTOList));
  }
}
