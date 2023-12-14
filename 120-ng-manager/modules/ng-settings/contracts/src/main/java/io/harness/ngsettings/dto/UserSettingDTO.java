package io.harness.ngsettings.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

import static io.harness.ngsettings.SettingConstants.*;
import static io.harness.ngsettings.SettingConstants.GROUP_ID;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class UserSettingDTO implements YamlDTO {
    @Schema(description = IDENTIFIER) @NotNull @NotBlank
    @EntityIdentifier
    String identifier;
    @Schema(description = NAME) @NotNull @NotBlank @EntityIdentifier String name;
    @NotNull @NotBlank @Schema(description = CATEGORY)
    SettingCategory category;
    @NotNull @NotBlank @Schema(description = VALUE_TYPE)
    SettingValueType valueType;
    @Schema(description = ALLOWED_VALUES)
    Set<String> allowedValues;
    @Schema(description = VALUE) String value;
    @Schema(description = USER_ID) String userID;
    @NotNull @NotBlank @Schema(description = GROUP_ID) String groupIdentifier;
}
