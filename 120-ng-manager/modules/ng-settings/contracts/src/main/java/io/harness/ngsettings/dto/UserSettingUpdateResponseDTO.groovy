package io.harness.ngsettings.dto

import io.harness.annotations.dev.OwnedBy
import io.swagger.v3.oas.annotations.media.Schema
import lombok.AccessLevel
import lombok.Builder
import lombok.Data
import lombok.experimental.FieldDefaults

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

import static io.harness.annotations.dev.HarnessTeam.PL
import static io.harness.ngsettings.SettingConstants.BATCH_ITEM_ERROR_MESSAGE
import static io.harness.ngsettings.SettingConstants.BATCH_ITEM_RESPONSE_STATUS
import static io.harness.ngsettings.SettingConstants.IDENTIFIER
import static io.harness.ngsettings.SettingConstants.LAST_MODIFIED_AT

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
class UserSettingUpdateResponseDTO {

    @Schema(description = IDENTIFIER) @NotBlank String identifier;
    @NotNull UserSettingDTO userSettingDTO;
    @Schema(description = LAST_MODIFIED_AT) Long lastModifiedAt;
    @Schema(description = BATCH_ITEM_RESPONSE_STATUS) boolean updateStatus;
    @Schema(description = BATCH_ITEM_ERROR_MESSAGE) String errorMessage;
}
