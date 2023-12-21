package io.harness.licensing.beans.modules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.List;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "BatchModuleLicenseRequest", description = "This contains the data required for batch call.")
public class BatchModuleLicenseRequestDTO {
    @Schema(description = "Specifies the list of account Id.") @NotNull List<String> accountIds;
}
