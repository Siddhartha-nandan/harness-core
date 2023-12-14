package io.harness.ngsettings.dto;

import static io.harness.ngsettings.SettingConstants.*;
import static io.harness.ngsettings.SettingConstants.UPDATE_TYPE;

import io.harness.data.validator.EntityIdentifier;
import io.harness.ngsettings.SettingUpdateType;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserSettingRequestDTO {
  @Schema(description = IDENTIFIER) @NotBlank @EntityIdentifier String identifier;
  @Schema(description = VALUE) String value;
  @NotNull @NotBlank @Schema(description = UPDATE_TYPE) SettingUpdateType updateType;
}
