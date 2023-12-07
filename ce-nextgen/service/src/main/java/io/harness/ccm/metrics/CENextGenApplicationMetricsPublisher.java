/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.metrics;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CENextGenApplicationMetricsPublisher implements MetricsPublisher {
  private final MetricRegistry metricRegistry;
  private final MetricService metricService;
  private static final Double SNAPSHOT_FACTOR = 1.0D / (double) TimeUnit.SECONDS.toNanos(1L);
  private static final Pattern METRIC_NAME_RE = Pattern.compile("[^a-zA-Z0-9:_]");
  private static final String NAMESPACE = System.getenv("NAMESPACE");
  private static final String CONTAINER_NAME = System.getenv("CONTAINER_NAME");
  private static final String SERVICE_NAME = "ce-nextgen-application";
  private static final MetricFilter meterMetricFilter =
      MetricFilter.startsWith("io.dropwizard.jetty.MutableServletContextHandler");
  private static final MetricFilter perspectiveMetricFilter = MetricFilter.contains("graphql");

  @Override
  public void recordMetrics() {
    metricRegistry.getMeters(meterMetricFilter)
        .forEach((key1, value1) -> recordMeter(sanitizeMetricName(key1), value1));
    metricRegistry.getMeters(perspectiveMetricFilter)
        .forEach((key, value) -> recordMeter(sanitizeMetricName(key), value));
    metricRegistry.getGauges().forEach((key, value) -> recordGauge(sanitizeMetricName(key), value));
    metricRegistry.getTimers().forEach((key, value) -> recordTimer(sanitizeMetricName(key), value));
    metricRegistry.getCounters().forEach((key, value) -> recordCounter(sanitizeMetricName(key), value));
  }

  private void recordMeter(String metricName, Meter meter) {
    try (CENextGenApplicationMetricsContext ignore =
             new CENextGenApplicationMetricsContext(NAMESPACE, CONTAINER_NAME, SERVICE_NAME)) {
      recordMetric(metricName + "_count", meter.getCount());
      recordMetric(metricName + "_fifteenMinuteRate", meter.getFifteenMinuteRate());
      recordMetric(metricName + "_fiveMinuteRate", meter.getFiveMinuteRate());
      recordMetric(metricName + "_oneMinuteRate", meter.getOneMinuteRate());
      recordMetric(metricName + "_meanRate", meter.getMeanRate());
    }
  }

  private void recordCounter(String metricName, Counter counter) {
    try (CENextGenApplicationMetricsContext ignore =
             new CENextGenApplicationMetricsContext(NAMESPACE, CONTAINER_NAME, SERVICE_NAME)) {
      recordMetric(metricName, counter.getCount());
    }
  }

  private void recordGauge(String metricName, Gauge gauge) {
    try (CENextGenApplicationMetricsContext ignore =
             new CENextGenApplicationMetricsContext(NAMESPACE, CONTAINER_NAME, SERVICE_NAME)) {
      Object obj = gauge.getValue();
      double value;
      if (obj instanceof Number) {
        value = ((Number) obj).doubleValue();
      } else {
        if (!(obj instanceof Boolean)) {
          log.debug(String.format(
              "Invalid type for Gauge %s: %s", metricName, obj == null ? "null" : obj.getClass().getName()));
          return;
        }
        value = (Boolean) obj ? 1.0D : 0.0D;
      }
      recordMetric(metricName, value);
    }
  }

  private void recordTimer(String metricName, Timer timer) {
    try (CENextGenApplicationMetricsContext ignore =
             new CENextGenApplicationMetricsContext(NAMESPACE, CONTAINER_NAME, SERVICE_NAME)) {
      recordMetric(metricName + "_count", timer.getCount());
      recordMetric(metricName + "_fifteenMinuteRate", timer.getFifteenMinuteRate());
      recordMetric(metricName + "_fiveMinuteRate", timer.getFiveMinuteRate());
      recordMetric(metricName + "_oneMinuteRate", timer.getOneMinuteRate());
      recordMetric(metricName + "_meanRate", timer.getMeanRate());
      recordSnapshot(metricName + "_snapshot", timer.getSnapshot());
    }
  }

  private void recordSnapshot(String metricName, Snapshot snapshot) {
    try (CENextGenApplicationMetricsContext ignore =
             new CENextGenApplicationMetricsContext(NAMESPACE, CONTAINER_NAME, SERVICE_NAME)) {
      recordMetric(metricName + "_mean", snapshot.getMean() * SNAPSHOT_FACTOR);
      recordMetric(metricName + "_95thPercentile", snapshot.get95thPercentile() * SNAPSHOT_FACTOR);
      recordMetric(metricName + "_99thPercentile", snapshot.get99thPercentile() * SNAPSHOT_FACTOR);
      recordMetric(metricName + "_999thPercentile", snapshot.get999thPercentile() * SNAPSHOT_FACTOR);
    }
  }

  private void recordMetric(String name, double value) {
    metricService.recordMetric(name, value);
  }

  private static String sanitizeMetricName(String dropwizardName) {
    String name = METRIC_NAME_RE.matcher(dropwizardName).replaceAll("_");
    if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
      name = "_" + name;
    }
    return name;
  }
}
