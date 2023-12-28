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
@JsonTypeName(Constants.FIXED_DELAY_BACKOFF_STRATEGY)
@Schema(name = "AwsFixedDelayBackoffStrategy",
    description = "Simple backoff strategy that always uses a fixed delay for the delay before the next retry attempt.")
public class AwsFixedDelayBackoffStrategySpec implements AwsSdkBackoffStrategySpec {
  long fixedBackoff;
  int retryCount;
}
