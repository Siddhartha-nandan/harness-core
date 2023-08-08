/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm.request;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.mixin.HelmChartCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class HelmFetchChartManifestTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  String accountId;
  @Expression(ALLOW_SECRETS) HelmChartManifestDelegateConfig helmChartConfig;
  long timeoutInMillis;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return HelmChartCapabilityGenerator.generateCapabilities(helmChartConfig, maskingEvaluator);
  }
}
