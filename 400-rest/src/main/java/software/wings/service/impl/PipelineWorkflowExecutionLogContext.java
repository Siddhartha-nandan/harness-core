/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.PipelineExecution;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PipelineWorkflowExecutionLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(PipelineExecution.class);

  public PipelineWorkflowExecutionLogContext(String workflowExecutionId, OverrideBehavior behavior) {
    super(ID, workflowExecutionId, behavior);
  }
}
