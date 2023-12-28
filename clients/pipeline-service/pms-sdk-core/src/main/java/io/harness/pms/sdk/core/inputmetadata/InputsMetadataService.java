/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.inputmetadata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputsMetadata;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.inputmetadata.InputsMetadataProto;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestBatch;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestProto;
import io.harness.pms.contracts.inputmetadata.InputsMetadataResponseBatch;
import io.harness.pms.contracts.inputmetadata.InputsMetadataResponseProto;
import io.harness.pms.contracts.inputmetadata.InputsMetadataServiceGrpc.InputsMetadataServiceImplBase;
import io.harness.pms.sdk.core.registries.InputsMetadataHandlerRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@Slf4j
public class InputsMetadataService extends InputsMetadataServiceImplBase {
  @Inject InputsMetadataHandlerRegistry inputsMetadataHandlerRegistry;
  @Inject ExceptionManager exceptionManager;

  @Override
  public void generateInputsMetadata(
      InputsMetadataRequestBatch requestsBatch, StreamObserver<InputsMetadataResponseBatch> responseObserver) {
    InputsMetadataResponseBatch.Builder inputsMetadataResponseBatchBuilder = InputsMetadataResponseBatch.newBuilder();
    Map<String, InputsMetadataResponse> jsonNodeMap = new HashMap<>();
    List<InputsMetadataRequestProto> inputsMetadataRequests = requestsBatch.getInputsMetadataRequestProtoList();
    log.info("Initiating inputs metadata request for the following FQNs: "
        + inputsMetadataRequests.stream().map(InputsMetadataRequestProto::getFqn).collect(Collectors.toSet()));
    for (InputsMetadataRequestProto request : inputsMetadataRequests) {
      String fqn = request.getFqn();
      try {
        String entityType = request.getEntityType();
        InputsMetadataHandler inputsMetadataHandler = inputsMetadataHandlerRegistry.obtain(entityType);
        String entityRef = request.getEntityId();
        String cacheKey = fqn + ":" + entityType + ":" + entityRef;
        if (jsonNodeMap.containsKey(cacheKey)) {
          inputsMetadataResponseBatchBuilder.addInputsMetadataResponseProto(
              convertToResponseProto(jsonNodeMap.get(cacheKey), fqn));
          continue;
        }
        InputsMetadataResponse inputsMetadataResponse =
            inputsMetadataHandler.generateInputsMetadata(request, requestsBatch.getInputsMetadataRequestMetadata());
        jsonNodeMap.put(cacheKey, inputsMetadataResponse);
        inputsMetadataResponseBatchBuilder.addInputsMetadataResponseProto(
            convertToResponseProto(jsonNodeMap.get(cacheKey), fqn));
      } catch (Exception ex) {
        WingsException processedException = exceptionManager.processException(ex);
        inputsMetadataResponseBatchBuilder.addInputsMetadataResponseProto(
            InputsMetadataResponseProto.newBuilder()
                .setSuccess(false)
                .setErrorMessage(ExceptionUtils.getMessage(processedException))
                .setFqn(fqn)
                .build());
      }
    }
    responseObserver.onNext(inputsMetadataResponseBatchBuilder.build());
    responseObserver.onCompleted();
  }

  InputsMetadataResponseProto convertToResponseProto(InputsMetadataResponse response, String fqn) {
    if (response.isSuccess()) {
      return InputsMetadataResponseProto.newBuilder()
          .setSuccess(response.isSuccess())
          .setFqn(fqn)
          .putAllResult(response.getResult().entrySet().stream().collect(
              Collectors.toMap(Map.Entry::getKey, e -> convertToInputsMetadataProto(e.getValue()))))
          .build();
    } else {
      return InputsMetadataResponseProto.newBuilder()
          .setSuccess(response.isSuccess())
          .setErrorMessage(response.getErrorMessage())
          .setFqn(fqn)
          .build();
    }
  }

  InputsMetadataProto convertToInputsMetadataProto(InputsMetadata inputsMetadata) {
    Boolean required = inputsMetadata.getRequired();
    String description = inputsMetadata.getDescription();
    return InputsMetadataProto.newBuilder()
        .setRequired(required != null && required)
        .setDescription(description == null ? "" : description)
        .build();
  }
}
