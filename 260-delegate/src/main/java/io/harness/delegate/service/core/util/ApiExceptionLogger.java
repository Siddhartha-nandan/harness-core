/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.util;

import io.kubernetes.client.openapi.ApiException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiExceptionLogger {
  public static String format(final ApiException e) {
    return String.format("K8S ApiException: %s, %s, %s, %s, %s", e.getCode(), e.getResponseBody(), e.getMessage(),
        e.getResponseHeaders(), e.getCause());
  }
}
