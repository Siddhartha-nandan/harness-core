/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.commons.metrics;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.AutoMetricContext;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(PL)
public class AccessControlMetricsContext extends AutoMetricContext {
  public AccessControlMetricsContext(String namespace, String containerName, String serviceName) {
    put("namespace", namespace);
    put("containerName", containerName);
    put("serviceName", serviceName);
  }

  public AccessControlMetricsContext(String entityIdentifier, String entityType, String action, String serviceId) {
    put("entityIdentifier", entityIdentifier);
    put("entityType", entityType);
    put("action", action);
    put("serviceId", serviceId);
  }
}
