/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.ecs.request;

import static io.harness.expression.common.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.task.ecs.EcsCommandTypeNG;
import io.harness.delegate.task.ecs.EcsInfraConfig;
import io.harness.expression.common.Expression;
import io.harness.reflection.util.ExpressionReflectionUtils.NestedAnnotationResolver;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class EcsCanaryDeployRequest implements EcsCommandRequest, NestedAnnotationResolver {
  String accountId;
  EcsCommandTypeNG ecsCommandType;
  String commandName;
  CommandUnitsProgress commandUnitsProgress;
  @NonFinal @Expression(ALLOW_SECRETS) EcsInfraConfig ecsInfraConfig;
  @NonFinal @Expression(ALLOW_SECRETS) Integer timeoutIntervalInMin;
  @Expression(ALLOW_SECRETS) String ecsTaskDefinitionManifestContent;
  @Expression(ALLOW_SECRETS) String ecsServiceDefinitionManifestContent;
  @Expression(ALLOW_SECRETS) List<String> ecsScalableTargetManifestContentList;
  @Expression(ALLOW_SECRETS) List<String> ecsScalingPolicyManifestContentList;
  Long desiredCountOverride;
  String ecsServiceNameSuffix;
}
