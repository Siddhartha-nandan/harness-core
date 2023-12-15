package io.harness.ngsettings.remote;

import com.google.inject.Inject;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.services.UserSettingsService;
import lombok.AllArgsConstructor;

import java.util.List;
@AllArgsConstructor(onConstructor = @__({ @Inject}))
public class UserSettingsResourceImpl implements UserSettingResource{

    UserSettingsService userSettingsService;

    @Override
    public ResponseDTO<SettingValueResponseDTO> get(String identifier, String accountIdentifier, String userIdentifier) {
       return ResponseDTO.newResponse(userSettingsService.getUserSettingValueForIdentifier(identifier,accountIdentifier,userIdentifier));
    }

    @Override
    public ResponseDTO<List<UserSettingResponseDTO>> list(String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
        return null;
    }

    @Override
    public ResponseDTO<List<UserSettingUpdateResponseDTO>> update(String accountIdentifier, String userIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList) {
        return null;
    }
}
