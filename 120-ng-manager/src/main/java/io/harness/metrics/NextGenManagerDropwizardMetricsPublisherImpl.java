/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.metrics;

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
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class NextGenManagerDropwizardMetricsPublisherImpl implements MetricsPublisher {
  private final MetricRegistry metricRegistry;
  private final MetricService metricService;
  private static final Double SNAPSHOT_FACTOR = 1.0D / (double) TimeUnit.SECONDS.toNanos(1L);
  private static final Pattern METRIC_NAME_RE = Pattern.compile("[^a-zA-Z0-9:_]");
  private static final String NAMESPACE = System.getenv("NAMESPACE");
  private static final String SERVICE_NAME = "ng-manager";
  private static final MetricFilter MUTABLE_SERVLET_CONTEXT_HANDLER_FILTER =
      MetricFilter.startsWith("io.dropwizard.jetty.MutableServletContextHandler");

  private static final List<String> RESOURCE_METRIC_NAMES_PREFIXES =
      List.of("io_harness_ng_core_service_resources_ServiceResourceV2_",
          "io_harness_ng_core_environment_resources_EnvironmentResourceV2_",
          "io_harness_ng_core_infrastructure_resource_InfrastructureResource_",
          "io_harness_ng_core_artifacts_resources_docker_DockerArtifactResource_",
          "io_harness_filestore_resource_FileStoreResource_");
  private static final MetricFilter SERVICE_RESOURCE_V2_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.service.resources.ServiceResourceV2");
  private static final MetricFilter ENVIRONMENT_RESOURCE_V2_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.environment.resources.EnvironmentResourceV2");
  private static final MetricFilter INFRASTRUCTURE_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.infrastructure.resource.InfrastructureResource");
  private static final MetricFilter DOCKER_ARTIFACT_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.artifacts.resources.docker.DockerArtifactResource");
  private static final MetricFilter FILESTORE_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.filestore.resource.FileStoreResource");
  private static final MetricFilter DELEGATE_DOWNLOAD_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateDownloadResource");
  private static final MetricFilter DELEGATE_TOKEN_NG_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateTokenNgResource");
  private static final MetricFilter DELEGATE_CONFIG_NG_V2_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateConfigNgV2Resource");
  private static final MetricFilter DELEGATE_GROUP_TAGS_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateGroupTagsResource");
  private static final MetricFilter DELEGATE_PROFILE_NG_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateProfileNgResource");
  private static final MetricFilter DELEGATE_SETUP_NG_RESOURCE_FILTER =
      MetricFilter.startsWith("io.harness.ng.core.delegate.resources.DelegateSetupNgResource");
  private static final String NG_MANAGER_RESOURCE_RESPONSE_COUNT_METRIC_NAME =
      "io_harness_ng_manager_resources_responses_count";

  @Override
  public void recordMetrics() {
    metricRegistry.getMeters(MUTABLE_SERVLET_CONTEXT_HANDLER_FILTER)
        .forEach((key, value) -> recordMutableServletContextMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(SERVICE_RESOURCE_V2_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(ENVIRONMENT_RESOURCE_V2_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(INFRASTRUCTURE_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DOCKER_ARTIFACT_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(FILESTORE_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_DOWNLOAD_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_TOKEN_NG_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_CONFIG_NG_V2_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_GROUP_TAGS_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_PROFILE_NG_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));
    metricRegistry.getMeters(DELEGATE_SETUP_NG_RESOURCE_FILTER)
        .forEach((key, value) -> recordResourceMeter(sanitizeMetricName(key), value));

    Set<Map.Entry<String, Gauge>> gaugeSet = metricRegistry.getGauges().entrySet();
    gaugeSet.forEach(entry -> recordGauge(sanitizeMetricName(entry.getKey()), entry.getValue()));
    Set<Map.Entry<String, Timer>> timerSet = metricRegistry.getTimers().entrySet();
    timerSet.forEach(entry -> recordTimer(sanitizeMetricName(entry.getKey()), entry.getValue()));
    Set<Map.Entry<String, Counter>> counterSet = metricRegistry.getCounters().entrySet();
    counterSet.forEach(entry -> recordCounter(sanitizeMetricName(entry.getKey()), entry.getValue()));
  }

  private void recordMutableServletContextMeter(String metricName, Meter meter) {
    try (NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME)) {
      recordMetric(metricName + "_count", meter.getCount());
    }
  }

  private void recordResourceMeter(String originalMetricName, Meter meter) {
    String[] s = originalMetricName.split("_");
    if (s.length >= 4) {
      String statusCode = s[s.length - 2];
      String method = s[s.length - 3];
      String resource = s[s.length - 4];
      try (NextGenMetricsContext ignore =
               new NextGenMetricsContext(NAMESPACE, SERVICE_NAME, resource, method, statusCode)) {
        recordMetric(NG_MANAGER_RESOURCE_RESPONSE_COUNT_METRIC_NAME, meter.getCount());
      }
    }
  }

  private void recordCounter(String metricName, Counter counter) {
    try (NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME)) {
      recordMetric(metricName, counter.getCount());
    }
  }

  private void recordGauge(String metricName, Gauge gauge) {
    try (NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME)) {
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
    if (isNgManagerResourceMetric(metricName)) {
      addTimerMetricsForResources(metricName, "io_harness_ng_manager_resources_total", timer);
    } else {
      try (NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME)) {
        recordMetric(metricName + "_count", timer.getCount());
        recordSnapshot(metricName + "_snapshot", timer.getSnapshot());
      }
    }
  }

  private void addTimerMetricsForResources(String originalMetricName, String metricName, Timer timer) {
    String[] s = originalMetricName.split("_");
    if (s.length >= 3) {
      String methodName = s[s.length - 2];
      String resourceName = s[s.length - 3];
      try (
          NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME, resourceName, methodName)) {
        recordMetric(metricName + "_count", timer.getCount());
        Snapshot snapshot = timer.getSnapshot();
        recordMetric(metricName + "_snapshot_95thPercentile", snapshot.get95thPercentile() * SNAPSHOT_FACTOR);
        recordMetric(metricName + "_snapshot_99thPercentile", snapshot.get99thPercentile() * SNAPSHOT_FACTOR);
      }
    }
  }

  private boolean isNgManagerResourceMetric(String metricName) {
    for (String resourceName : RESOURCE_METRIC_NAMES_PREFIXES) {
      // Logging only total metrics as we want to find total time spent for each api
      if (metricName.startsWith(resourceName) && metricName.contains("_total")) {
        return true;
      }
    }
    return false;
  }

  private void recordSnapshot(String metricName, Snapshot snapshot) {
    try (NextGenMetricsContext ignore = new NextGenMetricsContext(NAMESPACE, SERVICE_NAME)) {
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
