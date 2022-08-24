/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.provision.azure.AzureCreateBPStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class AzureCreateBPResourceStepPlanCreator extends CDPMSStepPlanCreatorV2<AzureCreateBPStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.AZURE_CREATE_BP_RESOURCE);
  }

  @Override
  public Class<AzureCreateBPStepNode> getFieldClass() {
    return AzureCreateBPStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, AzureCreateBPStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
