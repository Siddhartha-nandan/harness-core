/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.interrupts.InterruptService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.InitiateMode;
import io.harness.pms.contracts.execution.events.InitiateNodeEvent;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.utils.PmsFeatureFlagService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class InitiateNodeHandler extends PmsBaseEventHandler<InitiateNodeEvent> {
  @Inject private OrchestrationEngine engine;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;

  @Inject private InterruptService interruptService;

  @Override
  protected String getEventType(InitiateNodeEvent message) {
    return "trigger_node_event";
  }

  @Override
  protected Map<String, String> extraLogProperties(InitiateNodeEvent event) {
    return ImmutableMap.of();
  }

  @Override
  protected Ambiance extractAmbiance(InitiateNodeEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected void handleEventWithContext(InitiateNodeEvent event) {
    if (event.getInitiateMode() == InitiateMode.START) {
      engine.startNodeExecution(event.getAmbiance());
    } else {
      engine.initiateNode(event.getAmbiance(), event.getNodeId(), event.getRuntimeId(), null,
          event.hasStrategyMetadata() ? event.getStrategyMetadata() : null, event.getInitiateMode());
    }
  }
}
