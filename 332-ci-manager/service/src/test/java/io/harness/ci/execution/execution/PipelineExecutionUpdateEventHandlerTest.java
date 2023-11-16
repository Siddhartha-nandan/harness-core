/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.execution;

import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.RAGHAV_GUPTA;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.app.beans.entities.StepExecutionParameters;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.ListUtils;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.repositories.StepExecutionParametersRepository;
import io.harness.rule.Owner;

import java.util.Optional;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private GitBuildStatusUtility gitBuildStatusUtility;
  @Mock private StepExecutionParametersRepository stepExecutionParametersRepository;
  @InjectMocks private PipelineExecutionUpdateEventHandler pipelineExecutionUpdateEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEvent() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(ListUtils.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .build();
    when(stepExecutionParametersRepository.findFirstByAccountIdAndRunTimeId(any(), any()))
        .thenReturn(Optional.of(
            StepExecutionParameters.builder().accountId("accountId").stepParameters("stepParameters").build()));
    when(gitBuildStatusUtility.shouldSendStatus(any())).thenReturn(true);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(gitBuildStatusUtility).sendStatusToGit(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = RAGHAV_GUPTA)
  @Category(UnitTests.class)
  public void testSendGitStatusWhenCloneDisabled() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(ListUtils.newArrayList(
                              Level.newBuilder()
                                  .setRuntimeId("node1")
                                  .setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).build())
                                  .build()))
                          .build())
            .build();
    IntegrationStageStepParametersPMS integrationStageStepParametersPMS =
        IntegrationStageStepParametersPMS.builder().enableCloneRepo(ParameterField.createValueField(false)).build();
    StageElementParameters stageElementParameters =
        StageElementParameters.builder().specConfig(integrationStageStepParametersPMS).build();
    when(stepExecutionParametersRepository.findFirstByAccountIdAndRunTimeId(any(), any()))
        .thenReturn(Optional.of(StepExecutionParameters.builder()
                                    .accountId("accountId")
                                    .stepParameters(RecastOrchestrationUtils.toJson(stageElementParameters))
                                    .build()));
    when(gitBuildStatusUtility.shouldSendStatus(any())).thenReturn(true);
    Ambiance ambiance = orchestrationEvent.getAmbiance();
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    pipelineExecutionUpdateEventHandler.sendGitStatus(
        level, ambiance, orchestrationEvent.getStatus(), orchestrationEvent, "accountId");
    verify(gitBuildStatusUtility, times(0)).sendStatusToGit(any(), any(), any(), any());
  }
}
