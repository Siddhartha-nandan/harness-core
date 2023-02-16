/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.beans.summary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.beans.TypeSummary;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSummary extends BaseSummary {
  Map<String, Long> typeSummary;
  Map<String, Long> stepTypeSummary;
  Set<String> expressions;

  Map<String, TypeSummary> stepsSummary;

  @Builder
  public WorkflowSummary(int count, Map<String, Long> typeSummary, Map<String, Long> stepTypeSummary,
      Set<String> expressions, Map<String, TypeSummary> stepsSummary) {
    super(count);
    this.typeSummary = typeSummary;
    this.stepTypeSummary = stepTypeSummary;
    this.expressions = expressions;
    this.stepsSummary = stepsSummary;
  }
}
