/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.beans;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.LogAnalysisResult.RadarChartTag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
public class AnalyzedRadarChartLogDataDTO implements Comparable<AnalyzedRadarChartLogDataDTO> {
  String projectIdentifier;
  String orgIdentifier;
  String environmentIdentifier;
  String serviceIdentifier;

  RadarChartLogData logData;

  @Data
  @Builder
  public static class RadarChartLogData {
    String text;
    String clusterId;
    Long label;
    int count;
    @JsonIgnore Double angle;
    @JsonIgnore Double radius;
    double riskScore;
    Risk riskStatus;
    List<AnalyzedLogDataDTO.FrequencyDTO> trend;
    RadarChartTag clusterType;
  }

  @Data
  @Builder
  public static class FrequencyDTO {
    private long timestamp;
    private int count;
  }

  @Override
  public int compareTo(@NotNull AnalyzedRadarChartLogDataDTO o) {
    int result = o.getLogData().getClusterType().compareTo(logData.getClusterType());
    if (result == 0) {
      result = Integer.compare(o.logData.getCount(), logData.getCount());
    }
    return result;
  }
}
