/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.steps.executables.PipelineAsyncExecutable;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class BarrierStep extends PipelineAsyncExecutable {
  public static final StepType STEP_TYPE = StepSpecTypeConstants.BARRIER_STEP_TYPE;

  private static final String BARRIER = "barrier";

  @Inject private BarrierService barrierService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsyncAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();
    Optional<Level> strategyLevel = AmbianceUtils.getStrategyLevelFromAmbiance(ambiance);
    String strategyExecutionId = strategyLevel.isPresent() ? strategyLevel.get().getRuntimeId() : null;
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByIdentifierAndPlanExecutionIdAndStrategyExecutionId(
        barrierSpecParameters.getBarrierRef(), ambiance.getPlanExecutionId(), strategyExecutionId);

    log.info("Barrier Step getting executed. RuntimeId: [{}], barrierUuid [{}], barrierIdentifier [{}]",
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), barrierExecutionInstance.getUuid(),
        barrierSpecParameters.getBarrierRef());

    return AsyncExecutableResponse.newBuilder().addCallbackIds(barrierExecutionInstance.getUuid()).build();
  }

  @Override
  public StepResponse handleAsyncResponseInternal(
      Ambiance ambiance, StepElementParameters stepElementParameters, Map<String, ResponseData> responseDataMap) {
    BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();

    // if barrier is still in STANDING => update barrier state
    BarrierExecutionInstance barrierExecutionInstance =
        updateBarrierExecutionInstance(barrierSpecParameters.getBarrierRef(), ambiance);

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    BarrierResponseData responseData = (BarrierResponseData) responseDataMap.get(barrierExecutionInstance.getUuid());
    if (responseData.isFailed()) {
      BarrierResponseData.BarrierError barrierError = responseData.getBarrierError();
      if (barrierError.isTimedOut()) {
        stepResponseBuilder.status(Status.EXPIRED);
      } else {
        stepResponseBuilder.status(Status.FAILED);
      }
      stepResponseBuilder.failureInfo(FailureInfo.newBuilder().setErrorMessage(barrierError.getErrorMessage()).build());
    } else {
      stepResponseBuilder.status(Status.SUCCEEDED);
    }

    return stepResponseBuilder
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(BARRIER)
                         .outcome(BarrierOutcome.builder().barrierRef(barrierExecutionInstance.getIdentifier()).build())
                         .build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepElementParameters stepElementParameters, AsyncExecutableResponse executableResponse) {
    BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();

    updateBarrierExecutionInstance(barrierSpecParameters.getBarrierRef(), ambiance);
  }

  private BarrierExecutionInstance updateBarrierExecutionInstance(String identifier, Ambiance ambiance) {
    Optional<Level> strategyLevel = AmbianceUtils.getStrategyLevelFromAmbiance(ambiance);
    String strategyExecutionId = strategyLevel.isPresent() ? strategyLevel.get().getRuntimeId() : null;
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByIdentifierAndPlanExecutionIdAndStrategyExecutionId(
            identifier, ambiance.getPlanExecutionId(), strategyExecutionId);
    return barrierService.update(barrierExecutionInstance);
  }
}
