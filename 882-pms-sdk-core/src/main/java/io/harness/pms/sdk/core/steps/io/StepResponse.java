/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Wither;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class StepResponse {
  @NonNull Status status;
  @Wither @Singular Collection<StepOutcome> stepOutcomes;

  FailureInfo failureInfo;
  List<UnitProgress> unitProgressList;

  @Value
  @Builder
  public static class StepOutcome {
    String group;
    @NonNull String name;
    Outcome outcome;
  }

  public Map<String, StepOutcome> stepOutcomeMap() {
    Map<String, StepOutcome> stepOutcomeMap = new HashMap<>();
    if (isEmpty(stepOutcomes)) {
      return stepOutcomeMap;
    }
    for (StepOutcome stepOutcome : stepOutcomes) {
      stepOutcomeMap.put(stepOutcome.getName(), stepOutcome);
    }
    return stepOutcomeMap;
  }
}
