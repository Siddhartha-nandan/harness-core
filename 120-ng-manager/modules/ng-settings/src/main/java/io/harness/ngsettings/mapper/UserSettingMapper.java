package io.harness.ngsettings.mapper;

import io.harness.ngsettings.dto.UserSettingDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.SettingConfiguration;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;

public class UserSettingMapper {
  public UserSetting toUserSetting(
      String accountIdentifier, String userId, UserSettingConfiguration userSettingConfiguration) {
    return UserSetting.builder()
        .userID(userId)
        .identifier(userSettingConfiguration.getIdentifier())
        .category(userSettingConfiguration.getCategory())
        .accountIdentifier(accountIdentifier)
        .value(userSettingConfiguration.getDefaultValue())
        .valueType(userSettingConfiguration.getValueType())
        .groupIdentifier(userSettingConfiguration.getGroupIdentifier())
        .build();
  }

  public UserSettingResponseDTO writeUserSettingResponseDTO(
      UserSetting userSetting, UserSettingConfiguration userSettingConfiguration) {
    return UserSettingResponseDTO.builder()
        .userSetting(writeUserSettingDTO(userSetting, userSettingConfiguration))
        .lastModifiedAt(userSetting.getLastModifiedAt())
        .build();
  }
  public UserSettingDTO writeUserSettingDTO(
      UserSetting userSetting, UserSettingConfiguration userSettingConfiguration) {
    return UserSettingDTO.builder()
        .identifier(userSetting.getIdentifier())
        .userID(userSetting.getUserID())
        .allowedValues(userSettingConfiguration.getAllowedValues())
        .category(userSetting.getCategory())
        .groupIdentifier(userSetting.getGroupIdentifier())
        .value(userSetting.getValue())
        .valueType(userSettingConfiguration.getValueType())
        .build();
  }

  public UserSettingUpdateResponseDTO writeUserSettingUpdateDTO(
      UserSetting userSetting, UserSettingConfiguration userSettingConfiguration) {
    return UserSettingUpdateResponseDTO.builder()
        .userSettingDTO(writeUserSettingDTO(userSetting, userSettingConfiguration))
        .lastModifiedAt(userSetting.getLastModifiedAt())
        .identifier(userSetting.getIdentifier())
        .updateStatus(true)
        .build();
  }
}
