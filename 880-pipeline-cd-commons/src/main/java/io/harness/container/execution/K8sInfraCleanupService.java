/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.execution;

import io.harness.container.output.KubernetesInfraOutput;
import io.harness.container.output.KubernetesInfraOutputService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sInfraCleanupService {
  @Inject private BijouDelegateTaskExecutor bijouDelegateTaskExecutor;
  @Inject private KubernetesInfraOutputService kubernetesInfraOutputService;

  public void cleanupInfra(Ambiance ambiance) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    try {
      OptionalSweepingOutput optionalCleanupSweepingOutput = kubernetesInfraOutputService.getK8sInfraOutput(ambiance);
      if (!optionalCleanupSweepingOutput.isFound()) {
        log.error(
            "Unable to delete pod, not found K8s infraRefId in sweeping output stored in init step, accountId: {}, planExecutionId: {}",
            accountId, ambiance.getPlanExecutionId());
        return;
      }
      KubernetesInfraOutput kubernetesInfraOutput = (KubernetesInfraOutput) optionalCleanupSweepingOutput.getOutput();
      String infraRefId = kubernetesInfraOutput.getInfraRefId();

      log.info("Sent request for deleting K8s infra, infraRefId: {}", infraRefId);
      String queueCleanupTaskId = bijouDelegateTaskExecutor.queueCleanupTask(accountId, infraRefId);

    } catch (Exception ex) {
      log.error("Failed to delete pod for planExecutionId: {}", ambiance.getPlanExecutionId(), ex);
    }
  }
}
