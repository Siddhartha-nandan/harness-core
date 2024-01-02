package io.harness.ngsettings.mapper;
import static io.harness.rule.OwnerRule.SAHIBA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;
import io.harness.ngsettings.dto.UserSettingDTO;
import io.harness.ngsettings.dto.UserSettingResponseDTO;
import io.harness.ngsettings.dto.UserSettingUpdateResponseDTO;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class UserSettingsMapperTest extends CategoryTest {
  @Spy private UserSettingMapper userSettingMapper;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void writeToUserSettingFromUserSettingConfiguration() {
    String accountIdentifier = randomAlphabetic(10);
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    UserSettingConfiguration userSettingConfiguration = new UserSettingConfiguration();
    userSettingConfiguration.setIdentifier(identifier);
    userSettingConfiguration.setCategory(SettingCategory.CORE);
    userSettingConfiguration.setDefaultValue("true");
    userSettingConfiguration.setValueType(SettingValueType.BOOLEAN);

    UserSetting userSetting = userSettingMapper.toUserSetting(accountIdentifier, userId, userSettingConfiguration);
    assertThat(userSetting)
        .hasFieldOrPropertyWithValue("userID", userId)
        .hasFieldOrPropertyWithValue("identifier", userSettingConfiguration.getIdentifier())
        .hasFieldOrPropertyWithValue("category", userSettingConfiguration.getCategory())
        .hasFieldOrPropertyWithValue("accountIdentifier", accountIdentifier)
        .hasFieldOrPropertyWithValue("value", userSettingConfiguration.getDefaultValue())
        .hasFieldOrPropertyWithValue("valueType", userSettingConfiguration.getValueType())
        .hasFieldOrPropertyWithValue("groupIdentifier", userSettingConfiguration.getGroupIdentifier());
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void writeUserSettingResponseDTO() {
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    UserSetting userSetting = UserSetting.builder().build();
    userSetting.setIdentifier(identifier);
    userSetting.setUserID(userId);
    userSetting.setValue("true");
    userSetting.setCategory(SettingCategory.CORE);

    UserSettingConfiguration userSettingConfiguration = new UserSettingConfiguration();
    userSettingConfiguration.setIdentifier(identifier);
    userSettingConfiguration.setCategory(SettingCategory.CORE);
    userSettingConfiguration.setDefaultValue("true");
    userSettingConfiguration.setValueType(SettingValueType.BOOLEAN);

    UserSettingResponseDTO userSettingResponseDTO =
        userSettingMapper.writeUserSettingResponseDTO(userSetting, userSettingConfiguration);
    assertThat(userSettingResponseDTO)
        .hasFieldOrPropertyWithValue(
            "userSetting", userSettingMapper.writeUserSettingDTO(userSetting, userSettingConfiguration))
        .hasFieldOrPropertyWithValue("lastModifiedAt", userSetting.getLastModifiedAt());
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void writeUserSettingUpdateDTO() {
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    UserSetting userSetting = UserSetting.builder().build();
    userSetting.setIdentifier(identifier);
    userSetting.setUserID(userId);
    userSetting.setValue("true");
    userSetting.setCategory(SettingCategory.CORE);
    Set<String> allowedValues = new HashSet<>(Arrays.asList("true", "false"));
    UserSettingConfiguration userSettingConfiguration = new UserSettingConfiguration();
    userSettingConfiguration.setAllowedValues(allowedValues);
    userSettingConfiguration.setValueType(SettingValueType.BOOLEAN);

    UserSettingUpdateResponseDTO userSettingUpdateResponseDTO =
        userSettingMapper.writeUserSettingUpdateDTO(userSetting, userSettingConfiguration);
    assertThat(userSettingUpdateResponseDTO)
        .hasFieldOrPropertyWithValue(
            "userSettingDTO", userSettingMapper.writeUserSettingDTO(userSetting, userSettingConfiguration))
        .hasFieldOrPropertyWithValue("lastModifiedAt", userSetting.getLastModifiedAt())
        .hasFieldOrPropertyWithValue("identifier", userSetting.getIdentifier())
        .hasFieldOrPropertyWithValue("updateStatus", true);
  }

  @Test
  @Owner(developers = SAHIBA)
  @Category(UnitTests.class)
  public void writeUserSettingDTO() {
    String userId = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    UserSetting userSetting = UserSetting.builder().build();
    userSetting.setIdentifier(identifier);
    userSetting.setUserID(userId);
    userSetting.setValue("true");
    userSetting.setCategory(SettingCategory.CORE);

    UserSettingConfiguration userSettingConfiguration = new UserSettingConfiguration();
    Set<String> allowedValues = new HashSet<>(Arrays.asList("true", "false"));
    userSettingConfiguration.setValueType(SettingValueType.BOOLEAN);
    userSettingConfiguration.setAllowedValues(allowedValues);
    UserSettingDTO userSettingDTO = userSettingMapper.writeUserSettingDTO(userSetting, userSettingConfiguration);
    assertThat(userSettingDTO)
        .hasFieldOrPropertyWithValue("identifier", userSetting.getIdentifier())
        .hasFieldOrPropertyWithValue("userID", userSetting.getUserID())
        .hasFieldOrPropertyWithValue("allowedValues", userSettingConfiguration.getAllowedValues())
        .hasFieldOrPropertyWithValue("category", userSetting.getCategory())
        .hasFieldOrPropertyWithValue("groupIdentifier", userSetting.getGroupIdentifier())
        .hasFieldOrPropertyWithValue("value", userSetting.getValue())
        .hasFieldOrPropertyWithValue("valueType", userSettingConfiguration.getValueType());
  }
}
