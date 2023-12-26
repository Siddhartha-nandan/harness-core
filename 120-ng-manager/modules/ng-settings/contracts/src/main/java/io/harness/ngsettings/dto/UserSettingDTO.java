/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.dto;

import static io.harness.ngsettings.SettingConstants.*;
import static io.harness.ngsettings.SettingConstants.GROUP_ID;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.SettingValueType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class UserSettingDTO implements YamlDTO {
  @Schema(description = IDENTIFIER) @NotNull @NotBlank @EntityIdentifier String identifier;
  @NotNull @NotBlank @Schema(description = CATEGORY) SettingCategory category;
  @NotNull @NotBlank @Schema(description = VALUE_TYPE) SettingValueType valueType;
  @Schema(description = ALLOWED_VALUES) Set<String> allowedValues;
  @Schema(description = VALUE) String value;
  @Schema(description = USER_ID) String userID;
  @NotNull @NotBlank @Schema(description = GROUP_ID) String groupIdentifier;
}
