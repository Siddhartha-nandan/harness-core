package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@OwnedBy(PIPELINE)
@Slf4j
public class AsyncSdkProgressCallback implements ProgressCallback {
  @Inject ExecutableProcessorFactory executableProcessorFactory;
  @Inject SdkNodeExecutionService sdkNodeExecutionService;

  byte[] ambianceBytes;
  byte[] stepParameters;
  ExecutionMode mode;

  @Override
  public void notify(String correlationId, ProgressData progressData) {
    try {
      Ambiance ambiance = Ambiance.parseFrom(ambianceBytes);
      String stepParamsJson = ByteString.copyFrom(stepParameters).toStringUtf8();
      ExecutableProcessor executableProcessor = executableProcessorFactory.obtainProcessor(mode);
      executableProcessor.handleProgress(
          ProgressPackage.builder()
              .ambiance(ambiance)
              .stepParameters(RecastOrchestrationUtils.fromDocumentJson(stepParamsJson, StepParameters.class))
              .progressData(progressData)
              .build());
    } catch (InvalidProtocolBufferException e) {
      log.error("Not able to deserialize Node Execution from bytes. Progress Callback will not be executed");
    }
  }
}
