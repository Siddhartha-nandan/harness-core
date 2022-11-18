/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.group;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.steps.SdkCoreStepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.NGCommonUtilPlanCreationConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.executable.ChildExecutableWithRollbackAndRbac;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class GroupStepV1 extends ChildExecutableWithRollbackAndRbac<GroupStepParametersV1> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(NGCommonUtilPlanCreationConstants.GROUP).setStepCategory(StepCategory.STEP).build();

  @Override
  public void validateResources(Ambiance ambiance, GroupStepParametersV1 stepParameters) {
    // do nothing
  }

  @Override
  public ChildExecutableResponse obtainChildAfterRbac(
      Ambiance ambiance, GroupStepParametersV1 stepParameters, StepInputPackage inputPackage) {
    log.info("Starting group for Pipeline Step [{}]", stepParameters);
    final String stepNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepNodeId).build();
  }

  @Override
  public StepResponse handleChildResponseInternal(
      Ambiance ambiance, GroupStepParametersV1 stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed group Step =[{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<GroupStepParametersV1> getStepParametersClass() {
    return GroupStepParametersV1.class;
  }
}
