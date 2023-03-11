/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.matrix.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.strategy.v1.StrategyConfigV1;
import io.harness.plancreator.strategy.v1.StrategyTypeV1;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.matrix.StrategyAbstractStepParameters;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class StrategyStepParametersV1 extends StrategyAbstractStepParameters {
  StrategyConfigV1 strategyConfig;
  String childNodeId;
  ParameterField<Integer> maxConcurrency;
  StrategyTypeV1 strategyType;
  Boolean shouldProceedIfFailed;
}