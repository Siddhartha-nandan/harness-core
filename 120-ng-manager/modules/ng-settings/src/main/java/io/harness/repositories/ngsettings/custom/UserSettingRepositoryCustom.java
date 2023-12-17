package io.harness.repositories.ngsettings.custom;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.entities.UserSetting;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface UserSettingRepositoryCustom {

    UserSetting getUserSettingByIdentifier(String accountIdentifier, String userId, String identifier);

    List<UserSetting> listUserSettingForAccount(String accountIdentifier, String userIdentifier, SettingCategory category, String groupIdentifier);
}
