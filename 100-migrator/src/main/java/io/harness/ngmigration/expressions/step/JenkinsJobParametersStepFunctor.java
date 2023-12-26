/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.expressions.step;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngmigration.beans.StepOutput;

import org.apache.commons.lang3.StringUtils;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class JenkinsJobParametersStepFunctor extends JenkinsStepFunctor {
  public JenkinsJobParametersStepFunctor(StepOutput stepOutput, String currentStageIdentifier) {
    super(stepOutput);
    this.setCurrentStageIdentifier(currentStageIdentifier);
    this.stepOutput = stepOutput;
  }

  @Override
  public synchronized Object get(Object key) {
    return getFQN(key);
  }

  private String getFQN(Object key) {
    if (StringUtils.equals(stepOutput.getStageIdentifier(), getCurrentStageIdentifier())) {
      return String.format("<+execution.steps.%s.steps.%s.spec.fields.%s>", stepOutput.getStepGroupIdentifier(),
          stepOutput.getStepIdentifier(), key);
    }

    return String.format("<+pipeline.stages.%s.spec.execution.steps.%s.steps.%s.spec.fields.%s>",
        stepOutput.getStageIdentifier(), stepOutput.getStepGroupIdentifier(), stepOutput.getStepIdentifier(), key);
  }
}
