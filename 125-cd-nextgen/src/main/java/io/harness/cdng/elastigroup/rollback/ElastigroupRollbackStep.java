/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.elastigroup.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.executables.CdTaskExecutable;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.spot.elastigroup.rollback.ElastigroupRollbackTaskParameters;
import io.harness.delegate.task.spot.elastigroup.rollback.ElastigroupRollbackTaskResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.SkipRollbackException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.telemetry.helpers.StepExecutionTelemetryEventDTO;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class ElastigroupRollbackStep extends CdTaskExecutable<ElastigroupRollbackTaskResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.ELASTIGROUP_ROLLBACK.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject private ElastigroupRollbackStepHelper stepHelper;

  @Override
  public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {
    // Noop
  }

  @Override
  public Class<StepBaseParameters> getStepParametersClass() {
    return StepBaseParameters.class;
  }

  @Override
  protected StepExecutionTelemetryEventDTO getStepExecutionTelemetryEventDTO(
      Ambiance ambiance, StepBaseParameters stepParameters) {
    return StepExecutionTelemetryEventDTO.builder().stepType(STEP_TYPE.getType()).build();
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepBaseParameters stepParameters, StepInputPackage inputPackage) {
    try {
      ElastigroupRollbackStepParameters elastigroupRollbackStepParameters =
          (ElastigroupRollbackStepParameters) stepParameters.getSpec();

      ElastigroupRollbackTaskParameters taskParameters =
          stepHelper.getElastigroupRollbackTaskParameters(elastigroupRollbackStepParameters, ambiance, stepParameters);

      TaskData taskData =
          TaskData.builder()
              .async(true)
              .taskType(TaskType.ELASTIGROUP_ROLLBACK.name())
              .parameters(new Object[] {taskParameters})
              .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), StepUtils.DEFAULT_STEP_TIMEOUT))
              .build();

      return stepHelper.prepareTaskRequest(ambiance, taskData, stepHelper.getExecutionUnits(taskParameters),
          TaskType.ELASTIGROUP_ROLLBACK.getDisplayName(),
          TaskSelectorYaml.toTaskSelector(
              emptyIfNull(getParameterFieldValue(elastigroupRollbackStepParameters.getDelegateSelectors()))));
    } catch (SkipRollbackException e) {
      log.info("Skipping rollback: {}", e.getMessage());
      log.debug("Error: ", e);
      return TaskRequest.newBuilder()
          .setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContextAndNodeInfo(Ambiance ambiance,
      StepBaseParameters stepParameters, ThrowingSupplier<ElastigroupRollbackTaskResponse> responseDataSupplier)
      throws Exception {
    ElastigroupRollbackTaskResponse taskResponse;
    try {
      taskResponse = responseDataSupplier.get();
    } catch (Exception ex) {
      return stepHelper.handleTaskFailure(ambiance, stepParameters, ex);
    }

    if (taskResponse == null) {
      return stepHelper.handleTaskFailure(ambiance, stepParameters,
          new InvalidArgumentsException("Failed to process elastigroup rollback task response"));
    }

    return stepHelper.handleTaskResult(ambiance, stepParameters, taskResponse);
  }
}
