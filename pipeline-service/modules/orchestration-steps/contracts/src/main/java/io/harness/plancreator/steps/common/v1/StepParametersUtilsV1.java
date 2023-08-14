/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps.common.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.v1.StepElementParametersV1.StepElementParametersV1Builder;
import io.harness.plancreator.steps.internal.v1.PmsAbstractStepNodeV1;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class StepParametersUtilsV1 {
  public StepElementParametersV1Builder getStepParameters(PmsAbstractStepNodeV1 stepElementConfig) {
    StepElementParametersV1Builder stepBuilder = StepElementParametersV1.builder();
    stepBuilder.name(stepElementConfig.getName());
    stepBuilder.identifier(stepElementConfig.getId());
    stepBuilder.delegateSelectors(stepElementConfig.getDelegateSelectors());
    stepBuilder.description(stepElementConfig.getDescription());
    stepBuilder.failureStrategies(
        stepElementConfig.getFailureStrategies() != null ? stepElementConfig.getFailureStrategies().getValue() : null);
    stepBuilder.timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepElementConfig.getTimeout())));
    // TODO: when needs to be converted to ParameterField<String> for V1
    stepBuilder.when(
        stepElementConfig.getWhen() != null ? (String) stepElementConfig.getWhen().fetchFinalValue() : null);
    stepBuilder.uuid(stepElementConfig.getUuid());
    stepBuilder.enforce(stepElementConfig.getEnforce());

    return stepBuilder;
  }
}
