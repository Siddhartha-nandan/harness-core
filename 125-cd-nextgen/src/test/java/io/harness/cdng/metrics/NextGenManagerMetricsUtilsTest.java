/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.metrics;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.metrics.service.api.MetricService;
import io.harness.rule.Owner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class NextGenManagerMetricsUtilsTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private MetricService metricService;

  @InjectMocks NextGenManagerMetricsUtils nextGenManagerMetricsUtils;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void test() {
    nextGenManagerMetricsUtils.publishArtifactCounterMetrics("accountId", "status");

    verify(metricService).incCounter(eq("artifacts_counter"));
  }
}
