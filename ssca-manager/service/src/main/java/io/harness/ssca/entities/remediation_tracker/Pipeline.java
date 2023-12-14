/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.entities.remediation_tracker;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pipeline {
  @NotNull String pipelineExecutionId;
  @NotNull String pipelineId;
  @NotNull String pipelineName;
  @NotNull String triggeredBy;
  @NotNull String triggeredById;
  @NotNull Long triggeredAt;
}
