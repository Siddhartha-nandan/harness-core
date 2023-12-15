/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.approval.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.plancreator.steps.common.v1.StageElementParametersV1;
import io.harness.plancreator.steps.common.v1.StepParametersUtilsV1;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.stage.v1.ApprovalStageNodeV1;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_PIPELINE})
public class StepParameterUtils {
  public StageElementParametersV1.StageElementParametersV1Builder getStageParameters(ApprovalStageNodeV1 stageNode) {
    StageElementParametersV1.StageElementParametersV1Builder stageBuilder =
        StepParametersUtilsV1.getCommonStageParameters(stageNode);
    stageBuilder.failure(ParameterField.isNotNull(stageNode.getFailure()) ? stageNode.getFailure().getValue() : null);
    return stageBuilder;
  }
}
