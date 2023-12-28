/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AwsSdkRetryPolicyUtil {
  private @Inject AwsSdkDefaultBackOffStrategyConfiguration awsSdkDefaultBackOffStrategyConfiguration;
  public RetryPolicy getRetryPolicy(AwsSdkRetryPolicySpec retryPolicy) {
    if (retryPolicy == null) {
      return getDefaultRetryPolicy();
    }
    String backOffStrategyType = retryPolicy.getBackOffStrategyType();
    if (Constants.EQUAL_JITTER_BACKOFF_STRATEGY == backOffStrategyType) {
      AwsEqualJitterBackoffStrategySpec equalJitterBackOffStrategy =
          (AwsEqualJitterBackoffStrategySpec) retryPolicy.getBackOffStrategy();
      return getRetryPolicy(
          new PredefinedBackoffStrategies.EqualJitterBackoffStrategy(
              (int) equalJitterBackOffStrategy.getBaseDelay(), (int) equalJitterBackOffStrategy.getMaxBackoffTime()),
          equalJitterBackOffStrategy.getRetryCount());
    }

    else if (Constants.FULL_JITTER_BACKOFF_STRATEGY == backOffStrategyType) {
      AwsFullJitterBackoffStrategySpec fullJitterBackOffStrategy =
          (AwsFullJitterBackoffStrategySpec) retryPolicy.getBackOffStrategy();
      return getRetryPolicy(
          new PredefinedBackoffStrategies.FullJitterBackoffStrategy(
              (int) fullJitterBackOffStrategy.getBaseDelay(), (int) fullJitterBackOffStrategy.getMaxBackoffTime()),
          fullJitterBackOffStrategy.getRetryCount());
    }

    // AWS SDK v1 does not contain a fixed delay backoff strategy compatible with v1 RetryPolicy
    return getDefaultRetryPolicy();
  }

  private RetryPolicy getDefaultRetryPolicy() {
    return getRetryPolicy(new PredefinedBackoffStrategies.SDKDefaultBackoffStrategy(
                              awsSdkDefaultBackOffStrategyConfiguration.getBaseDelayInMs(),
                              awsSdkDefaultBackOffStrategyConfiguration.getThrottledBaseDelayInMs(),
                              awsSdkDefaultBackOffStrategyConfiguration.getMaxBackoffInMs()),
        awsSdkDefaultBackOffStrategyConfiguration.getBackoffMaxErrorRetries());
  }

  private RetryPolicy getRetryPolicy(RetryPolicy.BackoffStrategy backoffStrategy, int retryCount) {
    return new RetryPolicy(new PredefinedRetryPolicies.SDKDefaultRetryCondition(), backoffStrategy, retryCount, false);
  }
}
