/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(Constants.FULL_JITTER_BACKOFF_STRATEGY)
@Schema(name = "AwsFullJitterBackoffStrategy",
    description = "Backoff strategy that uses a full jitter strategy for computing the next backoff delay.")
public class AwsFullJitterBackoffStrategySpec implements AwsSdkBackoffStrategySpec {
  long baseDelay;
  long maxBackoffTime;
  int retryCount;
}
