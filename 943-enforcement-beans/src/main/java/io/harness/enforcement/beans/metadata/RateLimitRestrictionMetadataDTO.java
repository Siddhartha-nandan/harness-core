package io.harness.enforcement.beans.metadata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.GTM)
@Schema(name = "RateLimitRestrictionMetadata",
    description = "This contains metadata of the Rate Limit Restriction in Harness")
public class RateLimitRestrictionMetadataDTO extends RestrictionMetadataDTO {
  private Long limit;
  private TimeUnit timeUnit;
  private boolean allowedIfEqual;
}
