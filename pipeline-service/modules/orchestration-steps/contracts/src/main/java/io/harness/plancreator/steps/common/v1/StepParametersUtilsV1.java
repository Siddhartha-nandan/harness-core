/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps.common.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.ParameterFieldHelper;
import io.harness.plancreator.stages.stage.v1.AbstractStageNodeV1;
import io.harness.plancreator.steps.common.v1.StepElementParametersV1.StepElementParametersV1Builder;
import io.harness.plancreator.steps.internal.v1.PmsAbstractStepNodeV1;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.SdkCoreStepUtils;
import io.harness.utils.TimeoutUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class StepParametersUtilsV1 {
  public StepElementParametersV1Builder getStepParameters(PmsAbstractStepNodeV1 stepElementConfig) {
    StepElementParametersV1Builder stepBuilder = StepElementParametersV1.builder();
    stepBuilder.name(stepElementConfig.getName());
    stepBuilder.id(stepElementConfig.getId());
    stepBuilder.desc(stepElementConfig.getDesc());
    stepBuilder.failure(stepElementConfig.getFailure() != null ? stepElementConfig.getFailure().getValue() : null);
    stepBuilder.timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepElementConfig.getTimeout())));
    stepBuilder.when(
        stepElementConfig.getWhen() != null ? (String) stepElementConfig.getWhen().fetchFinalValue() : null);
    stepBuilder.uuid(stepElementConfig.getUuid());
    stepBuilder.enforce(stepElementConfig.getEnforce());
    stepBuilder.type(stepElementConfig.getType());
    return stepBuilder;
  }

  public StageElementParametersV1.StageElementParametersV1Builder getCommonStageParameters(
      AbstractStageNodeV1 stageNode) {
    TagUtils.removeUuidFromTags(stageNode.getLabels());
    StageElementParametersV1.StageElementParametersV1Builder stageBuilder = StageElementParametersV1.builder();
    stageBuilder.name(stageNode.getName());
    stageBuilder.id(stageNode.getId());
    stageBuilder.desc(SdkCoreStepUtils.getParameterFieldHandleValueNull(stageNode.getDesc()));
    stageBuilder.when(ParameterField.isNotNull(stageNode.getWhen())
            ? ParameterFieldHelper.getParameterFieldFinalValueString(stageNode.getWhen())
            : null);
    stageBuilder.uuid(stageNode.getUuid());
    stageBuilder.variables(stageNode.getVariables());
    stageBuilder.delegates(stageNode.getDelegates());
    stageBuilder.labels(stageNode.getLabels());
    stageBuilder.type(stageNode.getType());
    stageBuilder.timeout(ParameterField.isNotNull(stageNode.getTimeout()) ? stageNode.getTimeout() : null);
    return stageBuilder;
  }
}
