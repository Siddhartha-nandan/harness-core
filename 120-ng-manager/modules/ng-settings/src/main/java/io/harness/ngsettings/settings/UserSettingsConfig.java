package io.harness.ngsettings.settings;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.Set;



@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class UserSettingsConfig {
    @NotEmpty String name;
    int version;
    @Valid Set<UserSettingConfiguration> userSettings;
}
