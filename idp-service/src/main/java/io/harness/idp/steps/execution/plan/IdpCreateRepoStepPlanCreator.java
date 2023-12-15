/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.steps.execution.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.idp.steps.Constants;
import io.harness.idp.steps.beans.stepnode.IdpCreateRepoStepNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.HarnessYamlVersion;

import com.google.common.collect.Sets;
import java.util.Set;
@OwnedBy(HarnessTeam.IDP)
public class IdpCreateRepoStepPlanCreator extends CIPMSStepPlanCreatorV2<IdpCreateRepoStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(Constants.CREATE_REPO);
  }

  @Override
  public Class<IdpCreateRepoStepNode> getFieldClass() {
    return IdpCreateRepoStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, IdpCreateRepoStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V0);
  }
}