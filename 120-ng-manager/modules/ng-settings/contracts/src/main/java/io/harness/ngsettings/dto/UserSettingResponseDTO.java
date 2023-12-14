package io.harness.ngsettings.dto;

import static io.harness.ngsettings.SettingConstants.LAST_MODIFIED_AT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class UserSettingResponseDTO {
    @NotNull UserSettingDTO userSetting;
    @Schema(description = LAST_MODIFIED_AT) Long lastModifiedAt;
}