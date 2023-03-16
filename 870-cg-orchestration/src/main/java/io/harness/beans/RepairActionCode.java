/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
public enum RepairActionCode {
  MANUAL_INTERVENTION,
  ROLLBACK_WORKFLOW,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  ROLLBACK_PHASE,
  IGNORE,
  RETRY,
  END_EXECUTION,
  CONTINUE_WITH_DEFAULTS,
  ABORT_WORKFLOW_EXECUTION,
  MARK_AS_FAILURE;

  private static final Set<RepairActionCode> pipelineRuntimeInputsTimeoutAction =
      EnumSet.of(END_EXECUTION, CONTINUE_WITH_DEFAULTS);

  public static boolean isPipelineRuntimeTimeoutAction(RepairActionCode actionCode) {
    return actionCode != null && pipelineRuntimeInputsTimeoutAction.contains(actionCode);
  }
}
