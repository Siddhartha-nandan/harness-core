/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.downtime.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecurringDowntimeSpecDetails extends DowntimeSpecDetails {
  @Valid String recurrenceEndTime;
  @ApiModelProperty(required = true) @NotNull DowntimeDuration downtimeDuration;
  @ApiModelProperty(required = true) @NotNull DowntimeRecurrence downtimeRecurrence;

  @Override
  public DowntimeType getType() {
    return DowntimeType.RECURRING;
  }
}
