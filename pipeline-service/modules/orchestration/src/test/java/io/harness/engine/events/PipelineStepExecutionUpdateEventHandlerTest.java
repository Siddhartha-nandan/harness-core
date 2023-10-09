/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.RISHABH;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.step.StepExecutionEntityService;
import io.harness.execution.step.StepExecutionEntity.StepExecutionEntityKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.rule.Owner;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.utils.PmsFeatureFlagHelper;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class PipelineStepExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private StepExecutionEntityService stepExecutionEntityService;
  @Mock private PmsFeatureFlagHelper pmsFeatureFlagHelper;
  @InjectMocks private PipelineStepExecutionUpdateEventHandler pipelineStepExecutionUpdateEventHandler;
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String PLAN_EXECUTION_ID = "planExecutionId";
  private static final String PIPELINE_ID = "pipelineId";
  private static final String STAGE_EXECUTION_ID = "stageExecutionId";
  private static final String RUNTIME_ID = "runtimeId";
  private static final String SETUP_ID = "setupId";
  private static final String STEP_ID = "stepId";
  private final Ambiance ambiance =
      Ambiance.newBuilder()
          .setPlanExecutionId(PLAN_EXECUTION_ID)
          .putAllSetupAbstractions(ImmutableMap.of("accountId", ACCOUNT_IDENTIFIER, "orgIdentifier", ORG_IDENTIFIER,
              "projectIdentifier", PROJECT_IDENTIFIER))
          .setStageExecutionId(STAGE_EXECUTION_ID)
          .setMetadata(ExecutionMetadata.newBuilder().setPipelineIdentifier(PIPELINE_ID).build())
          .addLevels(Level.newBuilder()
                         .setIdentifier(STEP_ID)
                         .setStepType(StepType.newBuilder()
                                          .setType(StepSpecTypeConstants.HARNESS_APPROVAL_STEP_TYPE.getType())
                                          .setStepCategory(StepCategory.STEP)
                                          .build())
                         .setRuntimeId(RUNTIME_ID)
                         .setStartTs(2)
                         .setSetupId(SETUP_ID)
                         .build())
          .build();
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(pmsFeatureFlagHelper.isEnabled(anyString(), any(FeatureName.class))).thenReturn(false);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testDeploymentStatusUpdateEventWithFFDisabled() {
    when(pmsFeatureFlagHelper.isEnabled(anyString(), any(FeatureName.class))).thenReturn(false);
    pipelineStepExecutionUpdateEventHandler.handleEvent(
        OrchestrationEvent.builder().status(Status.SUCCEEDED).ambiance(ambiance).build());
    verify(stepExecutionEntityService, times(0)).update(any(), anyString(), any());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testDeploymentStatusUpdateEventWithFFEnabled() {
    when(pmsFeatureFlagHelper.isEnabled(anyString(), any(FeatureName.class))).thenReturn(true);
    pipelineStepExecutionUpdateEventHandler.handleEvent(
        OrchestrationEvent.builder().status(Status.SUCCEEDED).ambiance(ambiance).endTs(80L).build());
    Map<String, Object> updates = new HashMap<>();
    updates.put(StepExecutionEntityKeys.status, Status.SUCCEEDED);
    updates.put(StepExecutionEntityKeys.endts, 80L);
    verify(stepExecutionEntityService)
        .update(Scope.builder()
                    .accountIdentifier(ACCOUNT_IDENTIFIER)
                    .orgIdentifier(ORG_IDENTIFIER)
                    .projectIdentifier(PROJECT_IDENTIFIER)
                    .build(),
            RUNTIME_ID, updates);
  }
}
