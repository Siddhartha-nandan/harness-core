package io.harness.ngsettings.services;

import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;

import java.util.List;

public interface UserSettingsService {


    SettingValueResponseDTO getUserSettingValueForIdentifier(String identifier, String accountIdentifier, String userIdentifier);

    List<UserSettingConfiguration> listDefaultSettings();

    UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration);

    void removeSetting(String identifier);

    List<UserSettingResponseDTO> list(String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier);
}
