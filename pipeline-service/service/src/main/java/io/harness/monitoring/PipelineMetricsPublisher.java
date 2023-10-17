/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.monitoring;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.engine.executions.node.NodeExecutionMonitorService;
import io.harness.engine.executions.plan.PlanExecutionMonitorService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineMetricsPublisher implements MetricsPublisher {
  @Inject PlanExecutionMonitorService planExecutionMonitorService;
  @Inject NodeExecutionMonitorService nodeExecutionMonitorService;
  @Override
  public void recordMetrics() {
    planExecutionMonitorService.registerActiveExecutionMetrics();
    nodeExecutionMonitorService.registerActiveExecutionMetrics();
  }
}
