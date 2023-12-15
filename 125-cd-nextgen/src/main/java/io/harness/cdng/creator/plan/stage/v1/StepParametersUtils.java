/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.common.ParameterFieldHelper;
import io.harness.plancreator.steps.common.v1.StageElementParametersV1.StageElementParametersV1Builder;
import io.harness.plancreator.steps.common.v1.StepParametersUtilsV1;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_PIPELINE})
public class StepParametersUtils {
  public StageElementParametersV1Builder getStageParameters(CustomStageNodeV1 stageNode) {
    StageElementParametersV1Builder stageBuilder = StepParametersUtilsV1.getCommonStageParameters(stageNode);
    stageBuilder.failure(ParameterField.isNotNull(stageNode.getFailure()) ? stageNode.getFailure().getValue() : null);
    return stageBuilder;
  }

  public StageElementParametersV1Builder getStageParameters(DeploymentStageNodeV1 stageNode) {
    StageElementParametersV1Builder stageBuilder = StepParametersUtilsV1.getCommonStageParameters(stageNode);
    stageBuilder.failure(ParameterField.isNotNull(stageNode.getFailure()) ? stageNode.getFailure().getValue() : null);
    stageBuilder.skipInstances(ParameterField.isNotNull(stageNode.getSkipInstances())
            ? ParameterFieldHelper.getBooleanParameterFieldValue(stageNode.getSkipInstances())
            : null);
    return stageBuilder;
  }
}
