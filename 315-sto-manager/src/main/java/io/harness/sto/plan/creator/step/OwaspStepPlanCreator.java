/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.sto.plan.creator.step;

import io.harness.beans.steps.STOStepType;
import io.harness.beans.steps.nodes.security.OwaspScanNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class OwaspStepPlanCreator extends CIPMSStepPlanCreatorV2<OwaspScanNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(STOStepType.OWASP.getName());
  }

  @Override
  public Class<OwaspScanNode> getFieldClass() {
    return OwaspScanNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, OwaspScanNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
