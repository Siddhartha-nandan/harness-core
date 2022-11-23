/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.elastigroup.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.elastigroup.response.SpotInstConfig;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.spotinst.model.ElastiGroup;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.NonFinal;

@Data
@Builder
@OwnedBy(CDP)
public class ElastigroupSetupCommandRequest
    implements ElastigroupCommandRequest, ExpressionReflectionUtils.NestedAnnotationResolver {
  String accountId;
  String commandName;
  CommandUnitsProgress commandUnitsProgress;
  String elastigroupJson;
  String elastigroupNamePrefix;
  ElastiGroup elastigroupOriginalConfig;
  Integer maxInstanceCount;
  boolean useCurrentRunningInstanceCount;
  String startupScript;
  String image;
  boolean blueGreen;
  ResizeStrategy resizeStrategy;
  List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs;
  String awsRegion;
  ConnectorInfoDTO connectorInfoDTO;
  @NonFinal @Expression(ALLOW_SECRETS) Integer timeoutIntervalInMin;
  @NonFinal @Expression(ALLOW_SECRETS) SpotInstConfig spotInstConfig;
  List<EncryptedDataDetail> awsEncryptedDetails;
}
