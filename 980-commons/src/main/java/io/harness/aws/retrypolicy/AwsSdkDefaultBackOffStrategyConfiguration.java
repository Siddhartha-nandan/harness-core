/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Singleton
public class AwsSdkDefaultBackOffStrategyConfiguration {
  @JsonProperty(defaultValue = "5") @Builder.Default int backoffMaxErrorRetries = 5;
  @JsonProperty(defaultValue = "100") @Builder.Default int baseDelayInMs = 100;
  @JsonProperty(defaultValue = "500") @Builder.Default int throttledBaseDelayInMs = 500;
  @JsonProperty(defaultValue = "20000") @Builder.Default int maxBackoffInMs = 20000;
}
