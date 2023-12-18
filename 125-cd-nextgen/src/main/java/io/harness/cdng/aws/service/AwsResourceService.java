/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.IdentifierRef;
import io.harness.beans.ScopeInfo;

import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.impl.aws.model.AwsEC2Instance;
import software.wings.service.impl.aws.model.AwsVPC;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(CDP)
public interface AwsResourceService {
  /**
   * Get the list of all the Capabilities in cloudformation
   *
   * @return the list of capabilities
   */
  List<String> getCapabilities();

  /**
   * Get the list of all the cloudformation states
   *
   * @return the list of all the cloudformation states
   */
  Set<String> getCFStates();

  /**
   * Get the list of available regions from the aws.yaml resource file
   *
   * @return the list of available regions
   */
  Map<String, String> getRegions();

  /**
   * Get all the rolesARNs associated with the given computeProviderId and deployment type
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   *
   * @return the list of rolesARNs
   */
  Map<String, String> getRolesARNs(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get all parameter keys for a cloudformation template
   *
   * @param type Where the template is stored (GIT, S3, or inline)
   * @param region AWS region
   * @param isBranch For GIT, the fetchType, (branch or commit)
   * @param branch The branch reference for GIT
   * @param repoName the name of the repo
   * @param filePath The file path for the template
   * @param commitId The commit id for GIT
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param data the template data if inline is selected
   * @param connectorDTO the IdentifierRef of the git connector
   * @return the list of Cloudformation param keys
   */
  List<AwsCFTemplateParamsData> getCFparametersKeys(ScopeInfo scopeInfo, String type, String region, boolean isBranch,
      String branch, String repoName, String filePath, String commitId, IdentifierRef awsConnectorRef, String data,
      String connectorDTO);

  /**
   * Get list of AWS instance hosts based on search criteria.
   * Use either autoScalingGroupName or (vpcIds, tags) criteria.
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param winRm true for winRm credentials, default false
   * @param region AWS region
   * @param vpcIds list of AWS vpc-ids
   * @param tags map of tags
   * @param autoScalingGroupName AWS autoScalingGroupName
   * @return list of hosts
   */
  List<AwsEC2Instance> filterHosts(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, boolean winRm, String region,
      List<String> vpcIds, Map<String, String> tags, String autoScalingGroupName);

  /**
   * Get list of AWS VPC ids
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS VPC ids
   */
  List<AwsVPC> getVPCs(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS tags
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS tags
   */
  Map<String, String> getTags(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS instance Load balancers
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS Load balancers
   */
  List<String> getLoadBalancers(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS autoscaling groups
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS autoscaling groups
   */
  List<String> getASGNames(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS clusters
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS clusters
   */
  List<String> getClusterNames(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS elastic load balancers
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS elastic load balancers
   */
  List<String> getElasticLoadBalancerNames(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);

  /**
   * Get list of AWS elastic load balancer listeners arn
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return map of AWS elastic load balancer listeners arn
   */
  Map<String, String> getElasticLoadBalancerListenersArn(
      ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region, String elasticLoadBalancer);

  /**
   * Get list of AWS elastic load balancer listener rules
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @param region AWS region
   * @return list of AWS elastic load balancer listener rules
   */
  List<String> getElasticLoadBalancerListenerRules(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region,
      String elasticLoadBalancer, String listenerArn);

  /**
   * Get list of AWS elastic kubernetes service clusters
   *
   * @param awsConnectorRef the IdentifierRef of the aws connector
   * @return list of AWS elastic kubernetes service clusters
   */
  List<String> getEKSClusterNames(ScopeInfo scopeInfo, IdentifierRef awsConnectorRef, String region);
}
