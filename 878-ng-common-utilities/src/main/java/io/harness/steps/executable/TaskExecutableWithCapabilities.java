/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.executable;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.rollback.RollbackExecutableUtility;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
// Task Executable with RBAC, Rollback and postTaskValidation
public abstract class TaskExecutableWithCapabilities<T extends StepParameters, R extends ResponseData>
    implements TaskExecutableWithRbac<T, R> {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Override
  public void handleFailureInterrupt(Ambiance ambiance, T stepParameters, Map<String, String> metadata) {
    RollbackExecutableUtility.publishRollbackInfo(ambiance, stepParameters, metadata, executionSweepingOutputService);
  }
  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, T stepParameters, ThrowingSupplier<R> responseDataSupplier)
      throws Exception {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      StepResponse stepResponse = handleTaskResultWithSecurityContext(ambiance, stepParameters, responseDataSupplier);
      return postTaskValidate(ambiance, stepParameters, stepResponse);
    }
  }

  // evaluating policies added in advanced section of the steps and updating status and failure info in the step
  // response
  public StepResponse postTaskValidate(Ambiance ambiance, T stepParameters, StepResponse stepResponse) {
    return stepResponse;
  }
}
