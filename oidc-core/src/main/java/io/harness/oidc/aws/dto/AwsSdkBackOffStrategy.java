/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.dto;

import io.harness.oidc.aws.constants.AwsOidcConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsFixedDelayBackOffStrategy.class, name = AwsOidcConstants.FIXED_DELAY_BACKOFF_STRATEGY)
  ,
      @JsonSubTypes.Type(
          value = AwsEqualJitterBackOffStrategy.class, name = AwsOidcConstants.EQUAL_JITTER_BACKOFF_STRATEGY),
      @JsonSubTypes.Type(
          value = AwsFullJitterBackOffStrategy.class, name = AwsOidcConstants.FULL_JITTER_BACKOFF_STRATEGY)
})
public interface AwsSdkBackOffStrategy {}
