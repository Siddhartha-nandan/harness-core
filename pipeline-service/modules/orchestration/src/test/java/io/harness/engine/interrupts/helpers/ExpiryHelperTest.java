/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts.helpers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.UnitStatus.EXPIRED;
import static io.harness.pms.contracts.interrupts.InterruptType.MARK_EXPIRED;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.SHIVAM;
import static io.harness.rule.OwnerRule.YUVRAJ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.ExpiryInterruptCallback;
import io.harness.engine.interrupts.handlers.publisher.InterruptEventPublisher;
import io.harness.engine.pms.execution.modifier.ambiance.NodeExecutionAmbianceHelper;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionBuilder;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.rule.Owner;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExpiryHelperTest extends OrchestrationTestBase {
  @Mock InterruptHelper interruptHelper;
  @Mock NodeExecutionService nodeExecutionService;
  @Mock NodeExecutionAmbianceHelper nodeExecutionAmbianceHelper;
  @Mock OrchestrationEngine engine;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock InterruptEventPublisher interruptEventPublisher;
  @Inject @InjectMocks ExpiryHelper expiryHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldExpireNodeExecutionInstance() {
    String notifyId = generateUuid();
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();

    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    NodeExecutionBuilder nodeExecutionBuilder =
        NodeExecution.builder()
            .uuid(nodeExecutionId)
            .ambiance(ambiance)
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .startTs(123L);

    Interrupt interrupt =
        Interrupt.builder().uuid(generateUuid()).planExecutionId(planExecutionId).type(MARK_EXPIRED).build();
    when(interruptHelper.discontinueTaskIfRequired(nodeExecutionBuilder.build())).thenReturn(true);
    when(nodeExecutionService.update(eq(nodeExecutionId), any()))
        .thenReturn(nodeExecutionBuilder
                        .interruptHistory(InterruptEffect.builder()
                                              .interruptType(interrupt.getType())
                                              .tookEffectAt(System.currentTimeMillis())
                                              .interruptId(interrupt.getUuid())
                                              .interruptConfig(interrupt.getInterruptConfig())
                                              .build())
                        .build());
    when(interruptEventPublisher.publishEvent(nodeExecutionId, interrupt, MARK_EXPIRED)).thenReturn(notifyId);
    expiryHelper.expireMarkedInstance(nodeExecutionBuilder.build(), interrupt);

    ArgumentCaptor<String> pName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<OldNotifyCallback> callbackCaptor = ArgumentCaptor.forClass(OldNotifyCallback.class);
    ArgumentCaptor<List> correlationIdCaptor = ArgumentCaptor.forClass(List.class);

    verify(waitNotifyEngine, times(1))
        .waitForAllOnInList(
            pName.capture(), callbackCaptor.capture(), correlationIdCaptor.capture(), eq(Duration.ofMinutes(1)));

    assertThat(callbackCaptor.getValue()).isInstanceOf(ExpiryInterruptCallback.class);
    List<String> corrIds = correlationIdCaptor.getValue();
    assertThat(corrIds).hasSize(1);
    assertThat(corrIds.get(0)).isEqualTo(notifyId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestEvaluateUnitProgress() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .unitProgress(UnitProgress.newBuilder()
                              .setUnitName("Fetch Files")
                              .setStatus(UnitStatus.SUCCESS)
                              .setEndTime(System.currentTimeMillis())
                              .build())
            .unitProgress(UnitProgress.newBuilder().setUnitName("Apply").setStatus(UnitStatus.RUNNING).build())
            .startTs(123L)
            .build();

    List<UnitProgress> unitProgressList = InterruptHelper.evaluateUnitProgresses(nodeExecution, EXPIRED);

    assertThat(unitProgressList).hasSize(2);
    assertThat(unitProgressList.stream().map(UnitProgress::getStatus))
        .containsExactly(UnitStatus.SUCCESS, UnitStatus.EXPIRED);
    assertThat(unitProgressList.stream().map(UnitProgress::getEndTime)).doesNotContain((Long) null);
  }

  @Test
  @Owner(developers = YUVRAJ)
  @Category(UnitTests.class)
  public void shouldTestExpireDiscontinuedInstance() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .unitProgress(UnitProgress.newBuilder()
                              .setUnitName("Fetch Files")
                              .setStatus(UnitStatus.SUCCESS)
                              .setEndTime(System.currentTimeMillis())
                              .build())
            .unitProgress(UnitProgress.newBuilder().setUnitName("Apply").setStatus(UnitStatus.RUNNING).build())
            .startTs(123L)
            .build();
    String interruptId = "interruptId";
    InterruptConfig interruptConfig = InterruptConfig.newBuilder().build();
    when(nodeExecutionAmbianceHelper.getExecutionAmbiance(any())).thenReturn(nodeExecution.getAmbiance());
    expiryHelper.expireDiscontinuedInstance(nodeExecution, interruptConfig, interruptId, MARK_EXPIRED);
    verify(engine, times(1)).processStepResponse(any(), any());
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void shouldTestExpireDiscontinuedInstanceAndEndAllNodesExecution() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .unitProgress(UnitProgress.newBuilder()
                              .setUnitName("Fetch Files")
                              .setStatus(UnitStatus.SUCCESS)
                              .setEndTime(System.currentTimeMillis())
                              .build())
            .unitProgress(UnitProgress.newBuilder().setUnitName("Apply").setStatus(UnitStatus.RUNNING).build())
            .startTs(123L)
            .build();
    String interruptId = "interruptId";
    when(nodeExecutionService.updateStatusWithOps(any(), any(), any(), any())).thenReturn(nodeExecution);
    when(nodeExecutionAmbianceHelper.getExecutionAmbiance(any())).thenReturn(nodeExecution.getAmbiance());
    Map<String, String> metadata = new HashMap<>();
    metadata.put("group", "STAGE");
    metadata.put("identifier", "Stage");

    expiryHelper.expireDiscontinuedInstanceAndEndAllNodesExecution(
        nodeExecution, InterruptConfig.newBuilder().build(), interruptId, MARK_EXPIRED, metadata);
    verify(engine, times(1)).endNodeExecution(any());
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void shouldExpireNodeExecutionInstanceAndEndExecution() {
    String notifyId = generateUuid();
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();

    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    NodeExecutionBuilder nodeExecutionBuilder =
        NodeExecution.builder()
            .uuid(nodeExecutionId)
            .ambiance(ambiance)
            .status(Status.RUNNING)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .startTs(123L);

    Interrupt interrupt =
        Interrupt.builder().uuid(generateUuid()).planExecutionId(planExecutionId).type(MARK_EXPIRED).build();
    when(interruptHelper.discontinueTaskIfRequired(any())).thenReturn(true);
    when(nodeExecutionService.update(eq(nodeExecutionId), any()))
        .thenReturn(nodeExecutionBuilder
                        .interruptHistory(InterruptEffect.builder()
                                              .interruptType(interrupt.getType())
                                              .tookEffectAt(System.currentTimeMillis())
                                              .interruptId(interrupt.getUuid())
                                              .interruptConfig(interrupt.getInterruptConfig())
                                              .build())
                        .build());
    when(interruptEventPublisher.publishEvent(nodeExecutionId, interrupt, MARK_EXPIRED)).thenReturn(notifyId);
    expiryHelper.expireMarkedInstance(nodeExecutionBuilder.build(), interrupt, true);

    ArgumentCaptor<String> pName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<OldNotifyCallback> callbackCaptor = ArgumentCaptor.forClass(OldNotifyCallback.class);
    ArgumentCaptor<List> correlationIdCaptor = ArgumentCaptor.forClass(List.class);

    verify(waitNotifyEngine, times(1))
        .waitForAllOnInList(
            pName.capture(), callbackCaptor.capture(), correlationIdCaptor.capture(), eq(Duration.ofMinutes(1)));

    assertThat(callbackCaptor.getValue()).isInstanceOf(ExpiryInterruptCallback.class);
    List<String> corrIds = correlationIdCaptor.getValue();
    assertThat(corrIds).hasSize(1);
    assertThat(corrIds.get(0)).isEqualTo(notifyId);
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void shouldExpireNodeExecutionInstanceAndEndExecutionSyncTask() {
    String notifyId = generateUuid();
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();

    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    NodeExecutionBuilder nodeExecutionBuilder =
        NodeExecution.builder()
            .uuid(nodeExecutionId)
            .ambiance(ambiance)
            .status(Status.RUNNING)
            .mode(ExecutionMode.SYNC)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .startTs(123L);

    Interrupt interrupt =
        Interrupt.builder().uuid(generateUuid()).planExecutionId(planExecutionId).type(MARK_EXPIRED).build();
    when(interruptHelper.discontinueTaskIfRequired(any())).thenReturn(true);
    when(nodeExecutionService.update(eq(nodeExecutionId), any()))
        .thenReturn(nodeExecutionBuilder
                        .interruptHistory(InterruptEffect.builder()
                                              .interruptType(interrupt.getType())
                                              .tookEffectAt(System.currentTimeMillis())
                                              .interruptId(interrupt.getUuid())
                                              .interruptConfig(interrupt.getInterruptConfig())
                                              .build())
                        .build());
    when(nodeExecutionService.updateStatusWithOps(any(), any(), any(), any())).thenReturn(nodeExecutionBuilder.build());
    when(nodeExecutionAmbianceHelper.getExecutionAmbiance(any())).thenReturn(ambiance);
    expiryHelper.expireMarkedInstance(nodeExecutionBuilder.build(), interrupt, true);

    ArgumentCaptor<String> pName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<OldNotifyCallback> callbackCaptor = ArgumentCaptor.forClass(OldNotifyCallback.class);
    ArgumentCaptor<List> correlationIdCaptor = ArgumentCaptor.forClass(List.class);
    verify(engine, times(1)).endNodeExecution(any());
  }
}
