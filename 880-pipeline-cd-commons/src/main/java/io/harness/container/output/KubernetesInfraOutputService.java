/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.output;

import static io.harness.container.output.KubernetesInfraOutput.KUBERNETES_INFRA_OUTPUT;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_COMMON_STEPS})
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class KubernetesInfraOutputService {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  public String getK8sInfraRefIdOrThrow(Ambiance ambiance) {
    OptionalSweepingOutput optionalCleanupSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(KUBERNETES_INFRA_OUTPUT));
    if (!optionalCleanupSweepingOutput.isFound()) {
      throw new InvalidRequestException("Not found k8sInfra infraRefId");
    }
    KubernetesInfraOutput k8sInfra = (KubernetesInfraOutput) optionalCleanupSweepingOutput.getOutput();
    return k8sInfra.getInfraRefId();
  }

  public void saveK8sInfra(Ambiance ambiance, String infraRefId) {
    KubernetesInfraOutput kubernetesInfraOutput = KubernetesInfraOutput.builder().infraRefId(infraRefId).build();
    executionSweepingOutputService.consume(
        ambiance, KUBERNETES_INFRA_OUTPUT, kubernetesInfraOutput, StepOutcomeGroup.STAGE.name());
  }

  public OptionalSweepingOutput getK8sInfraOutput(Ambiance ambiance) {
    return executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(KUBERNETES_INFRA_OUTPUT));
  }
}
