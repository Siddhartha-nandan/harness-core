/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.status.k8s;

import io.harness.spec.server.idp.v1.model.StatusInfo;

import java.util.Optional;

public interface HealthCheck {
  Optional<StatusInfo> getCurrentStatus(String accountId);
}
