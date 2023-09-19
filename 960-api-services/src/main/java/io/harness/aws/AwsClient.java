/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.aws;

import io.harness.aws.beans.AwsInternalConfig;
import io.harness.remote.CEAwsServiceEndpointConfig;
import io.harness.remote.CEProxyConfig;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.codecommit.model.Commit;
import com.amazonaws.services.codecommit.model.RepositoryMetadata;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.organizations.AWSOrganizationsClient;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.ObjectListing;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface AwsClient {
  void validateAwsAccountCredential(AwsConfig awsConfig, @NotNull String region);

  void validateAwsCodeCommitCredential(AwsConfig awsConfig, String region, String repo);

  RepositoryMetadata fetchRepositoryInformation(AwsConfig awsConfig, String region, String repo);

  List<Commit> fetchCommitInformation(AwsConfig awsConfig, String region, String repo, List<String> commitIds);

  void confirmSnsSubscription(String confirmationMessage, String topicArnString);

  String getAmazonEcrAuthToken(AwsConfig awsConfig, String account, String region);

  AWSCredentialsProvider getAssumedCredentialsProvider(AWSCredentialsProvider credentialsProvider,
      String crossAccountRoleArn, @Nullable String externalId, CEAwsServiceEndpointConfig ceAwsServiceEndpointConfig);

  AWSCredentialsProvider getAssumedCredentialsProviderWithRegion(AWSCredentialsProvider credentialsProvider,
      String crossAccountRoleArn, @Nullable String externalId, @NotNull String region,
      CEAwsServiceEndpointConfig ceAwsServiceEndpointConfig);

  Optional<ReportDefinition> getReportDefinition(
      AWSCredentialsProvider credentialsProvider, String curReportName, CEProxyConfig ceProxyConfig);

  AWSCredentialsProvider constructStaticBasicAwsCredentials(@NotNull String accessKey, @NotNull String secretKey);

  List<String> listRolePolicyNames(
      AWSCredentialsProvider awsCredentialsProvider, @NotNull String roleName, CEProxyConfig ceProxyConfig);

  List<EvaluationResult> simulatePrincipalPolicy(AWSCredentialsProvider credentialsProvider,
      @NotNull String policySourceArn, @NotEmpty List<String> actionNames, @Nullable List<String> resourceArns,
      @NotNull String region, CEProxyConfig ceProxyConfig);

  Policy getRolePolicy(
      AWSCredentialsProvider credentialsProvider, String roleName, String policyName, CEProxyConfig ceProxyConfig);

  ObjectListing getBucket(AWSCredentialsProvider credentialsProvider, String s3BucketName, String s3Prefix,
      CEAwsServiceEndpointConfig ceAwsServiceEndpointConfig);

  S3Objects getIterableS3ObjectSummaries(AWSCredentialsProvider credentialsProvider, String s3BucketName,
      String s3Prefix, CEAwsServiceEndpointConfig ceAwsServiceEndpointConfig);

  AWSOrganizationsClient getAWSOrganizationsClient(String crossAccountRoleArn, String externalId, String awsAccessKey,
      String awsSecretKey, CEProxyConfig ceProxyConfig, CEAwsServiceEndpointConfig ceAwsServiceEndpointConfig);

  Map<String, String> listIAMRoles(AwsInternalConfig awsInternalConfig);

  String getEcrImageUrl(AwsInternalConfig awsConfig, String registryId, String region, String imageName);
}
