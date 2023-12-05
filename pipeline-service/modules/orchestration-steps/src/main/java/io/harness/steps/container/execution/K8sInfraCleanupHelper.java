/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.container.execution;

import static io.harness.plancreator.steps.pluginstep.KubernetesInfraOutput.KUBERNETES_INFRA_OUTPUT;

import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.CleanupInfraRequest;
import io.harness.delegate.ScheduleTaskServiceGrpc;
import io.harness.plancreator.steps.pluginstep.KubernetesInfraOutput;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sInfraCleanupHelper {
  @Inject private ScheduleTaskServiceGrpc.ScheduleTaskServiceBlockingStub scheduleTaskServiceBlockingStub;

  @Inject private Supplier<DelegateCallbackToken> tokenSupplier;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  public void cleanupInfra(Ambiance ambiance) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    try {
      OptionalSweepingOutput optionalCleanupSweepingOutput = executionSweepingOutputService.resolveOptional(
          ambiance, RefObjectUtils.getSweepingOutputRefObject(KUBERNETES_INFRA_OUTPUT));
      if (!optionalCleanupSweepingOutput.isFound()) {
        log.error(
            "Unable to delete pod, not found K8s infraRefId in sweeping output stored in init step, accountId: {}, planExecutionId: {}",
            accountId, ambiance.getPlanExecutionId());
        return;
      }
      KubernetesInfraOutput kubernetesInfraOutput = (KubernetesInfraOutput) optionalCleanupSweepingOutput.getOutput();
      String infraRefId = kubernetesInfraOutput.getInfraRefId();

      log.info("Sent request for deleting K8s infra, infraRefId: {}", infraRefId);
      PmsGrpcClientUtils.retryAndProcessException(scheduleTaskServiceBlockingStub::cleanupInfra,
          CleanupInfraRequest.newBuilder().setAccountId(accountId).setInfraRefId(infraRefId).build());
    } catch (Exception ex) {
      log.error("Failed to delete pod for planExecutionId: {}", ambiance.getPlanExecutionId(), ex);
    }
  }
}
