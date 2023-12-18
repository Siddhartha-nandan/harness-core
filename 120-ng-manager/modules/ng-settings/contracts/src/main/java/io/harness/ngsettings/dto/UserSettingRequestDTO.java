package io.harness.ngsettings.dto;

import static io.harness.ngsettings.SettingConstants.*;
import static io.harness.ngsettings.SettingConstants.UPDATE_TYPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ngsettings.SettingUpdateType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class UserSettingRequestDTO {
  @Schema(description = IDENTIFIER) @NotBlank @EntityIdentifier String identifier;
  @Schema(description = VALUE) String value;
  @NotNull @NotBlank @Schema(description = UPDATE_TYPE) SettingUpdateType updateType;
  @Builder.Default Boolean enableAcrossAccounts = Boolean.FALSE;
}
