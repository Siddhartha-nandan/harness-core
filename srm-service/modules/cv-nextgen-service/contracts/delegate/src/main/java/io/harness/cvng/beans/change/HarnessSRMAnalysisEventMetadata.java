/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessSRMAnalysisEventMetadata extends ChangeEventMetadata {
  long analysisStartTime;

  long analysisEndTime;
  String planExecutionId;
  String pipelineId;
  String stageStepId;
  String stageId;
  String artifactType;
  String artifactTag;
  String status;
  SRMAnalysisStatus analysisStatus;
  String pipelinePath;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.SRM_STEP_ANALYSIS;
  }
}
