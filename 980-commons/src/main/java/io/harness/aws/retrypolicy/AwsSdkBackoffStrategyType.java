/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AwsSdkBackoffStrategyType {
  @JsonProperty(Constants.FIXED_DELAY_BACKOFF_STRATEGY)
  FIXED_DELAY_BACKOFF_STRATEGY(Constants.FIXED_DELAY_BACKOFF_STRATEGY),
  @JsonProperty(Constants.EQUAL_JITTER_BACKOFF_STRATEGY)
  EQUAL_JITTER_BACKOFF_STRATEGY(Constants.EQUAL_JITTER_BACKOFF_STRATEGY),
  @JsonProperty(Constants.FULL_JITTER_BACKOFF_STRATEGY)
  FULL_JITTER_BACKOFF_STRATEGY(Constants.FULL_JITTER_BACKOFF_STRATEGY);

  private final String displayName;

  AwsSdkBackoffStrategyType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  public static AwsSdkBackoffStrategyType fromString(String typeEnum) {
    for (AwsSdkBackoffStrategyType enumValue : AwsSdkBackoffStrategyType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}
