/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class NodeRiskCountDTO {
  private Integer totalNodeCount;
  private Integer anomalousNodeCount;
  @Singular private List<RiskCount> riskCounts;

  @Value
  @Builder
  public static class RiskCount {
    Risk risk;
    Integer count;

    @JsonIgnore
    public Risk getNodeRisk() {
      return risk;
    }

    public String getDisplayName() {
      return risk.getDisplayName();
    }
  }
}
