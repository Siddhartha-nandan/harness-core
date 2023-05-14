/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.serviceoverridev2.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
@Schema(name = "OrgLevelOverrideMigrationResponseDTO",
    description = "This is the Org Level Service Override Migration Response")
public class OrgLevelOverrideMigrationResponseDTO {
  String accountId;
  String orgIdentifier;
  boolean isSuccessful;
  long totalEnvironmentsCount;
  long totalServiceOverridesCount;
  long migratedEnvironmentsCount;
  long migratedServiceOverridesCount;
  List<String> environmentsInfo;
  List<SingleServiceOverrideMigrationResponse> serviceOverridesInfo;
}
