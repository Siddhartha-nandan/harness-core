/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.common;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMANG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.engine.execution.ExecutionInputService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.execution.NodeExecution;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.rule.Owner;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;
import io.harness.steps.wait.WaitStepService;
import io.harness.timeout.TimeoutEngine;
import io.harness.waiter.persistence.SpringPersistenceWrapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeExecutionMetadataDeleteObserverTest extends CategoryTest {
  @Mock private SpringPersistenceWrapper springPersistenceWrapper;
  @Mock private TimeoutEngine timeoutEngine;
  @Mock private ResourceRestraintInstanceService resourceRestraintInstanceService;
  @Mock private PlanService planService;
  @Mock private PmsGraphStepDetailsService pmsGraphStepDetailsService;
  @Mock private WaitStepService waitStepService;
  @Mock private ExecutionInputService executionInputService;
  @Mock private ApprovalInstanceService approvalInstanceService;
  @InjectMocks NodeExecutionMetadataDeleteObserver nodeExecutionMetadataDeleteObserver;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestNodesTTLUpdate() {
    Date ttlExpiry = Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(30)).toInstant());
    when(approvalInstanceService.isNodeExecutionOfApprovalStepType(any())).thenReturn(false);
    nodeExecutionMetadataDeleteObserver.onNodesTTLUpdate(Collections.emptyList(), ttlExpiry);
    verify(springPersistenceWrapper, times(0)).updateTTLAndRemoveForWaitInstancesAndMetadata(any(), any());
    verify(timeoutEngine, times(0)).deleteTimeouts(any());
    verify(resourceRestraintInstanceService, times(0)).updateTTLForGivenReleaseType(any(), any(), any());
    verify(planService, times(0)).updateTTLForNodesForGivenIds(any(), any());
    verify(pmsGraphStepDetailsService, times(0)).updateTTLForNodesForGivenIds(any(), any());
    verify(waitStepService, times(0)).deleteWaitStepInstancesForGivenNodeExecutionIds(any());
    verify(executionInputService, times(0)).updateTTLForNodesForGivenIds(any(), any());

    List<NodeExecution> nodeExecutionList = new LinkedList<>();
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionMetadataDeleteObserver.onNodesTTLUpdate(nodeExecutionList, ttlExpiry);
    verify(springPersistenceWrapper, times(1)).updateTTLAndRemoveForWaitInstancesAndMetadata(any(), any());
    verify(timeoutEngine, times(1)).deleteTimeouts(any());
    verify(resourceRestraintInstanceService, times(1)).updateTTLForGivenReleaseType(any(), any(), any());
    verify(planService, times(1)).updateTTLForNodesForGivenIds(any(), any());
    verify(pmsGraphStepDetailsService, times(1)).updateTTLForNodesForGivenIds(any(), any());
    verify(waitStepService, times(1)).deleteWaitStepInstancesForGivenNodeExecutionIds(any());
    verify(executionInputService, times(1)).updateTTLForNodesForGivenIds(any(), any());
    verify(approvalInstanceService, times(1)).deleteByNodeExecutionIds(any());
  }

  @Test
  @Owner(developers = NAMANG)
  @Category(UnitTests.class)
  public void shouldTestNodesDeleteOfApprovalStep() {
    Date ttlExpiry = Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(30)).toInstant());
    when(approvalInstanceService.isNodeExecutionOfApprovalStepType(any())).thenReturn(true);
    List<NodeExecution> nodeExecutionList = new LinkedList<>();
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());
    nodeExecutionList.add(NodeExecution.builder().uuid(UUIDGenerator.generateUuid()).build());

    nodeExecutionMetadataDeleteObserver.onNodesTTLUpdate(nodeExecutionList, ttlExpiry);
    verify(springPersistenceWrapper, times(1)).updateTTLAndRemoveForWaitInstancesAndMetadata(any(), any());
    verify(timeoutEngine, times(1)).deleteTimeouts(any());
    verify(resourceRestraintInstanceService, times(1)).updateTTLForGivenReleaseType(any(), any(), any());
    ArgumentCaptor<Set<String>> nodeExecutionIdsArgumentCaptor = ArgumentCaptor.forClass(Set.class);
    verify(approvalInstanceService, times(1)).deleteByNodeExecutionIds(nodeExecutionIdsArgumentCaptor.capture());
    assertThat(nodeExecutionIdsArgumentCaptor.getValue().size()).isEqualTo(4);
  }
}
