/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.interrupt;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.events.PmsEventMonitoringConstants;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.supporter.async.TestAsyncStep;
import io.harness.pms.sdk.core.supporter.children.TestChildChainStep;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class InterruptEventHandlerTest extends PmsSdkCoreTestBase {
  @Mock private PMSInterruptService pmsInterruptService;
  @Inject private StepRegistry stepRegistry;

  @Inject @InjectMocks InterruptEventHandler interruptEventHandler;

  @Before
  public void setup() {
    stepRegistry.register(TestChildChainStep.STEP_TYPE, new TestChildChainStep());
    stepRegistry.register(TestAsyncStep.ASYNC_STEP_TYPE, new TestAsyncStep("init"));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractLogProperties() {
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(AmbianceTestUtils.buildAmbiance())
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    Map<String, String> autoLogMap = ImmutableMap.<String, String>builder()
                                         .put("interruptType", event.getType().name())
                                         .put("interruptUuid", event.getInterruptUuid())
                                         .put("notifyId", event.getNotifyId())
                                         .build();
    assertThat(interruptEventHandler.extraLogProperties(event)).isEqualTo(autoLogMap);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractAmbiance() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(ambiance)
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    assertThat(interruptEventHandler.extractAmbiance(event)).isEqualTo(ambiance);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractMetricContext() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(ambiance)
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    assertThat(interruptEventHandler.extractMetricContext(new HashMap<>(), event, "RANDOM_STREAM"))
        .isEqualTo(ImmutableMap.<String, String>builder()
                       .put(PmsEventMonitoringConstants.MODULE, "pms")
                       .put(PmsEventMonitoringConstants.EVENT_TYPE, "interrupt_event")
                       .put(PmsEventMonitoringConstants.STREAM_NAME, "RANDOM_STREAM")
                       .build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractMetricPrefix() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(ambiance)
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    assertThat(interruptEventHandler.getEventType(event)).isEqualTo("interrupt_event");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleAbort() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(AmbianceUtils.cloneForFinish(ambiance,
                                   ambiance.getLevelsList()
                                       .get(ambiance.getLevelsList().size() - 1)
                                       .toBuilder()
                                       .setStepType(TestChildChainStep.STEP_TYPE)
                                       .build()))

                               .setType(InterruptType.ABORT)
                               .setInterruptUuid("interruptUuid")
                               .setNotifyId("notifyId")
                               .build();
    interruptEventHandler.handleEventWithContext(event);
    Mockito.verify(pmsInterruptService).handleAbort("notifyId");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleFailure() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(AmbianceUtils.cloneForFinish(ambiance,
                                   ambiance.getLevelsList()
                                       .get(ambiance.getLevelsList().size() - 1)
                                       .toBuilder()
                                       .setStepType(TestAsyncStep.ASYNC_STEP_TYPE)
                                       .build()))
                               .setType(InterruptType.CUSTOM_FAILURE)
                               .setInterruptUuid("interruptUuid")
                               .setNotifyId("notifyId")
                               .setAsync(AsyncExecutableResponse.newBuilder().addCallbackIds(generateUuid()).build())
                               .build();
    interruptEventHandler.handleEventWithContext(event);
    Mockito.verify(pmsInterruptService).handleFailure("notifyId");
    assertThat(TestAsyncStep.FAIL_COUNTER.get()).isEqualTo(1);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testHandleUserMarkedFailure() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String callbackId = generateUuid();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(AmbianceUtils.cloneForFinish(ambiance,
                                   ambiance.getLevelsList()
                                       .get(ambiance.getLevelsList().size() - 1)
                                       .toBuilder()
                                       .setStepType(TestAsyncStep.ASYNC_STEP_TYPE)
                                       .build()))
                               .setType(InterruptType.USER_MARKED_FAIL_ALL)
                               .setInterruptUuid("interruptUuid")
                               .setNotifyId("notifyId")
                               .setAsync(AsyncExecutableResponse.newBuilder().addCallbackIds(callbackId).build())
                               .build();
    interruptEventHandler.handleEventWithContext(event);
    Mockito.verify(pmsInterruptService).handleAbort("notifyId");
    // TestChildChainStep.isHandleAbortAndUserMarkedFailureCalled will be marked as true once the
    // handleAbortAndUserMarkedFailure is called on step.
    assertThat(TestAsyncStep.ABORT_COUNTER.get()).isEqualTo(1);
  }
}
