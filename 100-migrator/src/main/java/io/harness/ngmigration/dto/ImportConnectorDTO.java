/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.beans.ImportMechanism;
import io.harness.ngmigration.beans.InputDefaults;

import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CONNECTOR")
public class ImportConnectorDTO extends BaseImportDTO {
  @Parameter(
      description =
          "ALL: To migrate all connectors. TYPE: To migrate only specific type of connectors(eg: Docker, AWS etc). ID: TO migrate only specific connectors")
  @NotNull
  private ImportMechanism mechanism;

  @Parameter(description = "To be provided if mechanism is ID") private List<String> ids;

  @Parameter(description = "To be provided if mechanism is TYPE") private Set<SettingVariableTypes> types;

  @Parameter(description = "The defaults for every entity. By default every entity is scoped to project.")
  private Map<NGMigrationEntityType, InputDefaults> defaults;
}
