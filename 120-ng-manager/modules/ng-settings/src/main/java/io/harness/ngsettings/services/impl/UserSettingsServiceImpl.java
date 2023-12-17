package io.harness.ngsettings.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ngsettings.SettingConstants.GLOBAL_ACCOUNT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.dto.SettingValueResponseDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.mapper.SettingsMapper;
import io.harness.ngsettings.mapper.UserSettingMapper;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.repositories.ngsettings.spring.SettingRepository;
import io.harness.repositories.ngsettings.spring.UserSettingConfigurationRepository;
import io.harness.repositories.ngsettings.spring.UserSettingRepository;

import com.google.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
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

  private final UserSettingMapper userSettingMapper;

  @Override
  public SettingValueResponseDTO getUserSettingValueForIdentifier(
      String identifier, String accountIdentifier, String userIdentifier) {
    UserSetting userSettingForAccount =
        userSettingRepository.getUserSettingByIdentifier(accountIdentifier, userIdentifier, identifier);
    if (userSettingForAccount != null) {
      return SettingValueResponseDTO.builder()
          .valueType(userSettingForAccount.getValueType())
          .value(userSettingForAccount.getValue())
          .build();
    }
    UserSetting globalUserSetting =
        userSettingRepository.getUserSettingByIdentifier(GLOBAL_ACCOUNT, userIdentifier, identifier);
    if (globalUserSetting != null) {
      return SettingValueResponseDTO.builder()
          .valueType(globalUserSetting.getValueType())
          .value(globalUserSetting.getValue())
          .build();
    }

    Optional<UserSettingConfiguration> defaultUserSetting =
        userSettingConfigurationRepository.findByIdentifier(identifier);
    if (defaultUserSetting.isPresent()) {
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

  @Override
  public List<UserSettingResponseDTO> list(String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    List<UserSettingResponseDTO> userSettingResponseDTOS = new ArrayList<>();
    List<UserSetting> userSettingForAccount = listUserSettingForAccount(accountIdentifier,  userIdentifier, category, groupIdentifier);
    List<UserSettingConfiguration> defaultUserSettings = listDefaultSettings();
    defaultUserSettings.stream()
            .filter(defaultUserSetting -> userSettingForAccount.stream()
                    .anyMatch(accountSetting -> accountSetting.getIdentifier().equals(defaultUserSetting.getIdentifier())))
            .map(defaultUserSetting -> {
              UserSetting accountSetting = userSettingForAccount.stream()
                      .filter(account -> account.getIdentifier().equals(defaultUserSetting.getIdentifier()))
                      .findFirst()
                      .orElse(null);
              if (accountSetting != null) {
                return userSettingMapper.writeUserSettingResponseDTO(accountSetting, defaultUserSetting);
              }
              return null;
            })
            .filter(Objects::nonNull)
            .forEach(userSettingResponseDTOS::add);
    return userSettingResponseDTOS;
  }

  private List<UserSetting> listUserSettingForAccount(
      String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier) {
    List<UserSettingConfiguration> defaultUserSettings = listDefaultSettings();

    List<UserSetting> userSettingForAccount =
        userSettingRepository.listUserSettingForAccount(accountIdentifier, userIdentifier, category, groupIdentifier);
    if (!checkIfAllUserSettingPresent(userSettingForAccount, defaultUserSettings)) {
      List<UserSetting> userSettingForGlobalAccount =
          userSettingRepository.listUserSettingForAccount(GLOBAL_ACCOUNT, userIdentifier, category, groupIdentifier);
      userSettingForGlobalAccount.stream()
          .filter(globalSetting
              -> !userSettingForAccount.stream().anyMatch(
                  accountSetting -> accountSetting.getIdentifier().equals(globalSetting.getIdentifier())))
          .forEach(userSettingForAccount::add);
    }
    if (!checkIfAllUserSettingPresent(userSettingForAccount, defaultUserSettings)) {
      Set<String> userSettingIdentifiers =
          userSettingForAccount.stream().map(UserSetting::getIdentifier).collect(Collectors.toSet());
      defaultUserSettings.stream()
          .filter(defaultUserSetting -> !userSettingIdentifiers.contains(defaultUserSetting.getIdentifier()))
          .map(defaultUserSetting -> userSettingMapper.toUserSetting(accountIdentifier, defaultUserSetting))
          .forEach(userSettingForAccount::add);
    }

    return userSettingForAccount;
  }

  private boolean checkIfAllUserSettingPresent(
      List<UserSetting> userSettingForAccount, List<UserSettingConfiguration> defaultUserSettings) {
    if (userSettingForAccount.size() == defaultUserSettings.size()) {
      return true;
    }
    return false;
  }
}
