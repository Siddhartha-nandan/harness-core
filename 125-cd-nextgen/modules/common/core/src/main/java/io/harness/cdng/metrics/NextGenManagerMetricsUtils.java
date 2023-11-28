/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.metrics;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CDP)
public class NextGenManagerMetricsUtils {
  @Inject MetricService metricService;

  public void publishArtifactCounterMetrics(String accountId, String status) {
    try (CdNgMetricsContext metricsContext = new CdNgMetricsContext(accountId, status)) {
      metricService.incCounter("artifacts_counter");
    }
  }
}
