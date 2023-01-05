/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps;

import static io.harness.annotations.dev.HarnessTeam.IACM;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.IACMTerraformPlanInfo;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import java.time.Duration;
import java.util.List;

@ApiModel(subTypes = {IACMTerraformPlanInfo.class})
@OwnedBy(IACM)
public interface IACMStepInfo extends StepSpecType, WithStepElementParameters, SpecParameters {
  int MIN_RETRY = 0;
  int MAX_RETRY = 5;
  long DEFAULT_TIMEOUT = Duration.ofHours(2).toMillis();

  @JsonIgnore TypeInfo getNonYamlInfo();
  @JsonIgnore int getRetry();
  @JsonIgnore String getName();
  @JsonIgnore String getIdentifier();
  @JsonIgnore
  default long getDefaultTimeout() {
    return DEFAULT_TIMEOUT;
  }

  // TODO: implement this when we support graph section in yaml
  @JsonIgnore
  default List<String> getDependencies() {
    return null;
  }

  @Override
  default SpecParameters getSpecParameters() {
    return this;
  }

  default StepParameters getStepParameters(
      CIAbstractStepNode stepElementConfig, OnFailRollbackParameters failRollbackParameters) {
    StepElementParametersBuilder stepParametersBuilder =
        CiStepParametersUtils.getStepParameters(stepElementConfig, failRollbackParameters);
    stepParametersBuilder.spec(getSpecParameters());
    return stepParametersBuilder.build();
  }
}
