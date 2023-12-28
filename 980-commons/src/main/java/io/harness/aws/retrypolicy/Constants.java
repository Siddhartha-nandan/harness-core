/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

public class Constants {
  public static final String FIXED_DELAY_BACKOFF_STRATEGY = "FixedDelayBackoffStrategy";
  public static final String EQUAL_JITTER_BACKOFF_STRATEGY = "EqualJitterBackoffStrategy";
  public static final String FULL_JITTER_BACKOFF_STRATEGY = "FullJitterBackoffStrategy";
  public static final int DEFAULT_BACKOFF_MAX_ERROR_RETRIES = 5;
  public static final int BASE_DELAY_IN_MS = 100;
  public static final int THROTTLED_BASE_DELAY_IN_MS = 500;
  public static final int MAX_BACKOFF_IN_MS = 20000;
}
