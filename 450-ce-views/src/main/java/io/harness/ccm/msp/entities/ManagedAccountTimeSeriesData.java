/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.msp.entities;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.dto.TimeSeriesDataPoints;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CE;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CE)
public class ManagedAccountTimeSeriesData {
  List<TimeSeriesDataPoints> totalMarkupStats;
  List<TimeSeriesDataPoints> totalSpendStats;
}
