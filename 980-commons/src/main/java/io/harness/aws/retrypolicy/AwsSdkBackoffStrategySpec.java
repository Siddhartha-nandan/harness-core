/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsFixedDelayBackoffStrategySpec.class, name = Constants.FIXED_DELAY_BACKOFF_STRATEGY)
  , @JsonSubTypes.Type(value = AwsEqualJitterBackoffStrategySpec.class, name = Constants.EQUAL_JITTER_BACKOFF_STRATEGY),
      @JsonSubTypes.Type(value = AwsFullJitterBackoffStrategySpec.class, name = Constants.FULL_JITTER_BACKOFF_STRATEGY)
})
@Schema(name = "AwsSdkBackOffStrategySpec", description = "This contains AWS Sdk BackOff strategy spec")
public interface AwsSdkBackoffStrategySpec {}
