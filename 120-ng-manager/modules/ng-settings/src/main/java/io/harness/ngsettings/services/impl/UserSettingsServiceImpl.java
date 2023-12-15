package io.harness.ngsettings.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ngsettings.SettingConstants.GLOBAL_ACCOUNT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.repositories.ngsettings.spring.SettingRepository;
import io.harness.repositories.ngsettings.spring.UserSettingConfigurationRepository;
import io.harness.repositories.ngsettings.spring.UserSettingRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))

public class UserSettingsServiceImpl implements UserSettingsService {
  private final UserSettingConfigurationRepository userSettingConfigurationRepository;
  private final SettingRepository settingRepository;

  private final UserSettingRepository userSettingRepository;


  @Override
  public SettingValueResponseDTO getUserSettingValueForIdentifier(
      String identifier, String accountIdentifier, String userIdentifier) {
    UserSetting userSettingForAccount =
            userSettingRepository.getUserSettingForAccount(accountIdentifier, userIdentifier, identifier);
    if (userSettingForAccount != null) {
      return SettingValueResponseDTO.builder()
          .valueType(userSettingForAccount.getValueType())
          .value(userSettingForAccount.getValue())
          .build();
    }
      UserSetting globalUserSetting =
              userSettingRepository.getUserSettingForAccount(GLOBAL_ACCOUNT, userIdentifier, identifier);
      if (globalUserSetting != null) {
          return SettingValueResponseDTO.builder()
                  .valueType(globalUserSetting.getValueType())
                  .value(globalUserSetting.getValue())
                  .build();
      }

      Optional<UserSettingConfiguration> defaultUserSetting =  userSettingConfigurationRepository.findByIdentifier(identifier);
            if(defaultUserSetting.isPresent())
            {
                return SettingValueResponseDTO.builder()
                        .valueType(defaultUserSetting.get().getValueType())
                        .value(defaultUserSetting.get().getDefaultValue())
                        .build();
            }
        log.error("Invalid user setting identifier {}", identifier);
            return null;
  }

  @Override
  public List<UserSettingConfiguration> listDefaultSettings() {
    List<UserSettingConfiguration> userSettingConfigurations = new ArrayList<>();
    Criteria criteria = Criteria.where("_class").is("UserSettingConfiguration");
    for (UserSettingConfiguration userSettingConfiguration : userSettingConfigurationRepository.findAll(criteria)) {
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
    Optional<UserSettingConfiguration> existingUserSettingConfig =
        userSettingConfigurationRepository.findByIdentifier(identifier);
    if (existingUserSettingConfig.isPresent()) {
      userSettingConfigurationRepository.delete(existingUserSettingConfig.get());
    }
    List<Setting> existingUserSetting = settingRepository.findByIdentifier(identifier);
    settingRepository.deleteAll(existingUserSetting);
    if (existingUserSettingConfig.isPresent()) {
      log.info("Deleted the user setting configuration from the database- {}", existingUserSettingConfig.get());
    }
  }
}
