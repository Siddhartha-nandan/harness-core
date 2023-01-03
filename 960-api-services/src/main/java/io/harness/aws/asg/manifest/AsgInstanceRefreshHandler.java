/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.asg.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.asg.AsgSdkManager;
import io.harness.aws.asg.manifest.request.AsgInstanceRefreshManifestRequest;
import io.harness.manifest.request.ManifestRequest;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.StartInstanceRefreshResult;

@OwnedBy(CDP)
public class AsgInstanceRefreshHandler extends AsgManifestHandler<PutScalingPolicyRequest> {
  public AsgInstanceRefreshHandler(AsgSdkManager asgSdkManager, ManifestRequest manifestRequest) {
    super(asgSdkManager, manifestRequest);
  }

  @Override
  public Class<PutScalingPolicyRequest> getManifestContentUnmarshallClass() {
    return PutScalingPolicyRequest.class;
  }

  @Override
  public AsgManifestHandlerChainState upsert(AsgManifestHandlerChainState chainState, ManifestRequest manifestRequest) {
    AsgInstanceRefreshManifestRequest asgInstanceRefreshManifestRequest =
        (AsgInstanceRefreshManifestRequest) manifestRequest;
    String asgName = chainState.getAsgName();
    AutoScalingGroup autoScalingGroup = chainState.getAutoScalingGroup();
    if (autoScalingGroup == null) {
      asgSdkManager.info("Asg with name [%s] does not exist. Skipping instance refresh operation", asgName);
    } else {
      String operationName = format("Instance Refresh Asg %s", asgName);
      asgSdkManager.info("Operation `%s` has started", operationName);
      StartInstanceRefreshResult startInstanceRefreshResult = asgSdkManager.startInstanceRefresh(asgName,
          asgInstanceRefreshManifestRequest.isSkipMatching(), asgInstanceRefreshManifestRequest.getInstanceWarmup(),
          asgInstanceRefreshManifestRequest.getMinimumHealthyPercentage());
      String instanceRefreshId = startInstanceRefreshResult.getInstanceRefreshId();
      asgSdkManager.waitInstanceRefreshSteadyState(asgName, instanceRefreshId, operationName);
      asgSdkManager.infoBold("Operation `%s` ended successfully", operationName);
    }

    return chainState;
  }

  @Override
  public AsgManifestHandlerChainState delete(AsgManifestHandlerChainState chainState, ManifestRequest manifestRequest) {
    return chainState;
  }
}
