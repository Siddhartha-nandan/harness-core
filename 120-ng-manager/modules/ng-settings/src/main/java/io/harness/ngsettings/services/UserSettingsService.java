package io.harness.ngsettings.services;

import io.harness.ngsettings.entities.UserSettingConfiguration;

import java.util.List;

public interface UserSettingsService {


    List<UserSettingConfiguration> listDefaultSettings();

    UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration);

    void removeSetting(String identifier);
}
