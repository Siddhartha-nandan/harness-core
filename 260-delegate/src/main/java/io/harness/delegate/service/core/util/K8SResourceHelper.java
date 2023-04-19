/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class K8SResourceHelper {
  private static final Pattern RESOURCE_NAME_NORMALIZER = Pattern.compile("_");
  private static final String DEFAULT_RUNNER_NAMESPACE = "harness-delegate-ng";

  @NonNull
  public static String getPodName(final String taskGroupId) {
    return normalizeResourceName("harness-" + taskGroupId + "-job");
  }

  // K8S resource name needs to contain only lowercase alphanumerics . and -, but must start and end with alphanumerics
  // Regex used by K8S for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*'
  @NonNull
  public static String normalizeResourceName(final String resourceName) {
    return RESOURCE_NAME_NORMALIZER.matcher(resourceName.trim().toLowerCase(Locale.ROOT)).replaceAll(".");
  }

  @NonNull
  public static String getRunnerNamespace() {
    return DEFAULT_RUNNER_NAMESPACE;
  }
}
