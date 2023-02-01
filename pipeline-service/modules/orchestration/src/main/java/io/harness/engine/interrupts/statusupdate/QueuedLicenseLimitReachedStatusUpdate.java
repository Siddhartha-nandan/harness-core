/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.NodeStatusUpdateHandler;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;

@OwnedBy(PIPELINE)
public class QueuedLicenseLimitReachedStatusUpdate implements NodeStatusUpdateHandler {
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleNodeStatusUpdate(NodeUpdateInfo nodeStatusUpdateInfo) {
    planExecutionService.updateStatus(nodeStatusUpdateInfo.getPlanExecutionId(), Status.QUEUED_LICENSE_LIMIT_REACHED);
  }
}
