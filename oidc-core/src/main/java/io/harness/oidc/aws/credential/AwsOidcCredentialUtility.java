/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.credential;

import io.harness.aws.retrypolicy.AwsSdkRetryPolicyUtil;
import io.harness.oidc.aws.dto.AwsOidcCredentialRequestDto;
import io.harness.oidc.aws.dto.AwsOidcCredentialResponseDto;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AwsOidcCredentialUtility {
  @Inject private AwsSdkRetryPolicyUtil awsSdkRetryPolicyUtil;
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
    clientConfiguration.setRetryPolicy(awsSdkRetryPolicyUtil.getRetryPolicy(requestDto.getRetryPolicy()));
    return new WebIdentityFederationSessionCredentialsProvider(
        oidcToken, null, requestDto.getIamRoleArn(), clientConfiguration);
  }
}
