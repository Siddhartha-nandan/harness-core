package io.harness.ngsettings.services.impl;

import com.google.inject.Inject;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.repositories.ngsettings.spring.SettingRepository;
import io.harness.repositories.ngsettings.spring.UserSettingConfigurationRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.harness.annotations.dev.HarnessTeam.PL;

@Slf4j
@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject}))

public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserSettingConfigurationRepository userSettingConfigurationRepository;
    private final SettingRepository settingRepository;

    @Override
    public List<UserSettingConfiguration> listDefaultSettings() {
        List<UserSettingConfiguration> userSettingConfigurations = new ArrayList<>();
        Criteria criteria = Criteria.where("_class").is("UserSettingConfiguration");
        for(UserSettingConfiguration userSettingConfiguration : userSettingConfigurationRepository.findAll(criteria))
        {
            userSettingConfigurations.add(userSettingConfiguration);
        }

        return userSettingConfigurations;
    }

    @Override
    public UserSettingConfiguration upsertSettingConfiguration(UserSettingConfiguration userSettingConfiguration) {
        return userSettingConfigurationRepository.save(userSettingConfiguration);
    }

    @Override
    public void removeSetting(String identifier) {
        Optional<UserSettingConfiguration> existingUserSettingConfig = userSettingConfigurationRepository.findByIdentifier(identifier);
        if(existingUserSettingConfig.isPresent())
        {
            userSettingConfigurationRepository.delete(existingUserSettingConfig.get());
        }
        List<Setting> existingUserSetting = settingRepository.findByIdentifier(identifier);
        settingRepository.deleteAll(existingUserSetting);
        if (existingUserSettingConfig.isPresent()) {
            log.info("Deleted the user setting configuration from the database- {}", existingUserSettingConfig.get());
        }
    }
}
