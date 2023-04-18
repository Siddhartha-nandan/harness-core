/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.beans.dto;

import io.harness.assessment.settings.beans.entities.ScoreType;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreDTO {
  @NotNull ScoreType scoreType;
  @NotNull String entityId; //
  @NotNull @DecimalMin(value = "0.00", message = "Score cannot be less than zero.") Double score;
  @Min(0) Long maxScore;
}
