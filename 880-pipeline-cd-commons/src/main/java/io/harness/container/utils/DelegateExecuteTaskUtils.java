/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.Entrypoint;
import io.harness.delegate.Execution;
import io.harness.delegate.K8sExecutionSpec;
import io.harness.yaml.core.variables.OutputNGVariable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_COMMON_STEPS})
@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class DelegateExecuteTaskUtils {
  public static Execution getStepExecution(String stepIdentifier, String logPrefix, Entrypoint entryPoint,
      String infraRefId, List<OutputNGVariable> outputVariables) {
    List<String> outVars = getOutputVariableNames(outputVariables);

    return Execution.newBuilder()
        .setInfraRefId(infraRefId)
        .setStepId(stepIdentifier)
        .setStepLogKey(logPrefix)
        .setK8S(K8sExecutionSpec.newBuilder().addAllEnvVarOutputs(outVars).setEntryPoint(entryPoint).build())
        .build();
  }

  private List<String> getOutputVariableNames(List<OutputNGVariable> outputVariables) {
    return isEmpty(outputVariables)
        ? Collections.emptyList()
        : outputVariables.stream().map(OutputNGVariable::getName).collect(Collectors.toList());
  }
}
