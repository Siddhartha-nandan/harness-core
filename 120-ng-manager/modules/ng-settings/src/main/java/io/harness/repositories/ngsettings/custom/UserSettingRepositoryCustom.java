package io.harness.repositories.ngsettings.custom;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.UserSetting;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface UserSettingRepositoryCustom {

    UserSetting getUserSettingForAccount(String accountIdentifier, String userId, String identifier);
}
