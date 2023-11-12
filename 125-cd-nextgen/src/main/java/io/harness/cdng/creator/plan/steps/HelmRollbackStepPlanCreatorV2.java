/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.visitor.YamlTypes.HELM_DEPLOY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.helm.HelmRollbackStep;
import io.harness.cdng.helm.HelmRollbackStepNode;
import io.harness.cdng.helm.async.HelmRollbackStepV2;
import io.harness.cdng.helm.rollback.HelmRollbackStepParams;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Set;

@OwnedBy(CDP)
public class HelmRollbackStepPlanCreatorV2 extends CDPMSStepPlanCreatorV2<HelmRollbackStepNode> {
  @Inject private CDFeatureFlagHelper featureFlagService;

  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.HELM_ROLLBACK);
  }

  @Override
  public Class<HelmRollbackStepNode> getFieldClass() {
    return HelmRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, HelmRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, HelmRollbackStepNode stepElement) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepElement);

    String nativeHelmFqn = getExecutionStepFqn(ctx.getCurrentField(), HELM_DEPLOY);
    ((HelmRollbackStepParams) ((StepElementParameters) stepParameters).getSpec()).setHelmRollbackFqn(nativeHelmFqn);

    return stepParameters;
  }

  @Override
  public StepType getStepSpecType(PlanCreationContext ctx, HelmRollbackStepNode stepElement) {
    return featureFlagService.isEnabled(
               ctx.getMetadata().getAccountIdentifier(), FeatureName.CDS_K8S_ASYNC_STEP_STRATEGY)
        ? HelmRollbackStepV2.STEP_TYPE
        : HelmRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType(PlanCreationContext ctx, HelmRollbackStepNode stepElement) {
    return featureFlagService.isEnabled(
               ctx.getMetadata().getAccountIdentifier(), FeatureName.CDS_K8S_ASYNC_STEP_STRATEGY)
        ? OrchestrationFacilitatorType.ASYNC
        : OrchestrationFacilitatorType.TASK;
  }
}
