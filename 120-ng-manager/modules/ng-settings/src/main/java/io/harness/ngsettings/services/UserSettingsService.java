package io.harness.ngsettings.services;

import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingRequestDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;

import java.util.List;

public interface UserSettingsService {


    SettingValueResponseDTO get(String identifier, String accountIdentifier, String userIdentifier);

    List<UserSettingConfiguration> listDefaultSettings();

    UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration);

    void removeSetting(String identifier);

    List<UserSettingResponseDTO> list(String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier);

    List<UserSettingUpdateResponseDTO> update(String accountIdentifier, String userIdentifier, List<UserSettingRequestDTO> userSettingRequestDTOList);
}
