/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@OwnedBy(CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@TargetModule(HarnessModule._960_API_SERVICES)
public class BuildOperationDetails {
  private String name;
  @JsonProperty("metadata") private OperationMeta operationMeta;
  @JsonProperty("done") private Boolean isDone;
  private OperationError error;
}
