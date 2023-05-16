/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.plugin;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.pms.contracts.plan.ErrorResponse;
import io.harness.pms.contracts.plan.PluginCreationBatchRequest;
import io.harness.pms.contracts.plan.PluginCreationBatchResponse;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponse;
import io.harness.pms.contracts.plan.PluginCreationResponseList;
import io.harness.pms.contracts.plan.PluginInfoProviderServiceGrpc.PluginInfoProviderServiceImplBase;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class PluginInfoProviderService extends PluginInfoProviderServiceImplBase {
  @Inject PluginInfoProviderHelper pluginInfoProvider;

  @Override
  public void getPluginInfos(PluginCreationRequest request, StreamObserver<PluginCreationResponse> responseObserver) {
    PluginCreationResponse pluginCreationResponse = PluginCreationResponse.newBuilder().build();
    responseObserver.onNext(pluginCreationResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void getPluginInfosList(
      PluginCreationBatchRequest batchRequest, StreamObserver<PluginCreationBatchResponse> responseObserver) {
    Map<String, PluginCreationResponseList> requestIdToPluginCreationResponse = new HashMap<>();
    for (PluginCreationRequest request : batchRequest.getPluginCreationRequestList()) {
      PluginCreationResponseList pluginCreationResponse;

      try {
        pluginCreationResponse = pluginInfoProvider.getPluginInfo(request);
      } catch (Exception ex) {
        log.error("Got error in getting plugin info", ex);
        pluginCreationResponse = PluginCreationResponseList.newBuilder().build();
      }
      requestIdToPluginCreationResponse.put(request.getRequestId(), pluginCreationResponse);
    }
    responseObserver.onNext(
        PluginCreationBatchResponse.newBuilder().putAllRequestIdToResponse(requestIdToPluginCreationResponse).build());
    responseObserver.onCompleted();
  }
}
