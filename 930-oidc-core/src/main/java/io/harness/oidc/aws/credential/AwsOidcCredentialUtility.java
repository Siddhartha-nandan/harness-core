/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.credential;

import io.harness.oidc.aws.constants.AwsOidcConstants;
import io.harness.oidc.aws.dto.AwsEqualJitterBackOffStrategy;
import io.harness.oidc.aws.dto.AwsFullJitterBackOffStrategy;
import io.harness.oidc.aws.dto.AwsOidcCredentialRequestDto;
import io.harness.oidc.aws.dto.AwsOidcCredentialResponseDto;
import io.harness.oidc.aws.dto.AwsSdkCallRetryPolicy;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AwsOidcCredentialUtility {
  /**
   * Utility function to exchange for the OIDC AWS IAM Role Credential.
   *
   * @param oidcToken The OIDC Token for aws connector.
   * @param requestDto The Token exchange request body.
   * @return IAM Role Credential
   */
  public AwsOidcCredentialResponseDto getOidcIamRoleCredential(
      String oidcToken, AwsOidcCredentialRequestDto requestDto) {
    WebIdentityFederationSessionCredentialsProvider credentialsProvider =
        (WebIdentityFederationSessionCredentialsProvider) getOidcIamRoleCredentialProvider(oidcToken, requestDto);
    return AwsOidcCredentialResponseDto.builder()
        .accessKey(credentialsProvider.getCredentials().getAWSAccessKeyId())
        .secretAccessKey(credentialsProvider.getCredentials().getAWSSecretKey())
        .sessionToken(credentialsProvider.getCredentials().getSessionToken())
        .build();
  }

  /**
   * Utility function to exchange for the OIDC AWS IAM Role Credential Provider.
   *
   * @param oidcToken The OIDC Token for aws connector.
   * @param requestDto The Token exchange request body.
   * @return AWS IAM Role Credential Provider
   */
  public AWSCredentialsProvider getOidcIamRoleCredentialProvider(
      String oidcToken, AwsOidcCredentialRequestDto requestDto) {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setRetryPolicy(getRetryPolicy(requestDto.getRetryPolicy()));
    return new WebIdentityFederationSessionCredentialsProvider(
        oidcToken, null, requestDto.getIamRoleArn(), clientConfiguration);
  }

  /**
   * Utility function to exchange for the OIDC AWS IAM Role Credential.
   *
   * @param oidcToken The OIDC Token for aws connector.
   * @param requestDto The Token exchange request body.
   * @return AWS IAM Role Credential Provider
   */
  private RetryPolicy getRetryPolicy(AwsSdkCallRetryPolicy retryPolicy) {
    if (retryPolicy == null) {
      return getDefaultRetryPolicy();
    }
    String backOffStrategyType = retryPolicy.getBackOffStrategyType();
    if (AwsOidcConstants.EQUAL_JITTER_BACKOFF_STRATEGY == backOffStrategyType) {
      AwsEqualJitterBackOffStrategy equalJitterBackOffStrategy =
          (AwsEqualJitterBackOffStrategy) retryPolicy.getBackOffStrategy();
      return getRetryPolicy(
          new PredefinedBackoffStrategies.EqualJitterBackoffStrategy(
              (int) equalJitterBackOffStrategy.getBaseDelay(), (int) equalJitterBackOffStrategy.getMaxBackoffTime()),
          equalJitterBackOffStrategy.getRetryCount());
    }

    else if (AwsOidcConstants.FULL_JITTER_BACKOFF_STRATEGY == backOffStrategyType) {
      AwsFullJitterBackOffStrategy fullJitterBackOffStrategy =
          (AwsFullJitterBackOffStrategy) retryPolicy.getBackOffStrategy();
      return getRetryPolicy(
          new PredefinedBackoffStrategies.FullJitterBackoffStrategy(
              (int) fullJitterBackOffStrategy.getBaseDelay(), (int) fullJitterBackOffStrategy.getMaxBackoffTime()),
          fullJitterBackOffStrategy.getRetryCount());
    }

    // AWS SDK v1 does not contain a fixed delay backoff strategy compatible with v1 RetryPolicy
    return getDefaultRetryPolicy();
  }

  private RetryPolicy getDefaultRetryPolicy() {
    return getRetryPolicy(new PredefinedBackoffStrategies.SDKDefaultBackoffStrategy(AwsOidcConstants.BASE_DELAY_IN_MS,
                              AwsOidcConstants.THROTTLED_BASE_DELAY_IN_MS, AwsOidcConstants.MAX_BACKOFF_IN_MS),
        AwsOidcConstants.DEFAULT_BACKOFF_MAX_ERROR_RETRIES);
  }

  private RetryPolicy getRetryPolicy(RetryPolicy.BackoffStrategy backoffStrategy, int retryCount) {
    return new RetryPolicy(new PredefinedRetryPolicies.SDKDefaultRetryCondition(), backoffStrategy, retryCount, false);
  }
}
