/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.change;

import io.harness.cvng.core.beans.monitoredService.RiskData;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class MSHealthReport {
  RiskData currentHealthScore;
  ChangeSummaryDTO changeSummary;
  List<AssociatedSLOsDetails> associatedSLOsDetails;
  String internalLinkToEntity;

  @Value
  @Builder
  public static class AssociatedSLOsDetails {
    String identifier;
    String name;
    Double sloTarget;
    String scopedMonitoredServiceIdentifier;
    Double pastSLOPerformance;
    Double currentSLOPerformance;
    Double errorBudgetBurned;

    Double errorBudgetRemaining;
  }
}
