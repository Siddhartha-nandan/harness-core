/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import com.google.common.collect.Sets;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.tas.TasRollingRollbackStepNode;
import io.harness.cdng.tas.TasRollingRollbackStepParameters;
import io.harness.cdng.tas.TasSwapRollbackStepNode;
import io.harness.cdng.tas.TasSwapRollbackStepParameters;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Set;

import static io.harness.executions.steps.StepSpecTypeConstants.TAS_APP_RESIZE;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_BASIC_APP_SETUP;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_BG_APP_SETUP;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_CANARY_APP_SETUP;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_ROLLING_DEPLOY;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_ROLLING_ROLLBACK;
import static io.harness.executions.steps.StepSpecTypeConstants.TAS_SWAP_ROUTES;

@OwnedBy(HarnessTeam.CDP)
public class TasRollingRollbackStepPlanCreator extends CDPMSStepPlanCreatorV2<TasRollingRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.TAS_ROLLING_ROLLBACK);
  }

  @Override
  public Class<TasRollingRollbackStepNode> getFieldClass() {
    return TasRollingRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, TasRollingRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, TasRollingRollbackStepNode stepElement) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepElement);
    TasRollingRollbackStepParameters tasRollingRollbackStepParameters =
        (TasRollingRollbackStepParameters) ((StepElementParameters) stepParameters).getSpec();
    String tasRollingDeployFqn = getExecutionStepFqn(ctx.getCurrentField(), TAS_ROLLING_DEPLOY);
    tasRollingRollbackStepParameters.setTasRollingDeployFqn(tasRollingDeployFqn);
    return stepParameters;
  }
}
