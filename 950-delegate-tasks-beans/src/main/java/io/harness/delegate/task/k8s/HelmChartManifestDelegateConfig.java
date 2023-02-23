/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.k8s;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.expression.Expression;
import io.harness.k8s.model.HelmVersion;
import io.harness.reflection.ExpressionReflectionUtils.NestedAnnotationResolver;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelmChartManifestDelegateConfig implements ManifestDelegateConfig, NestedAnnotationResolver {
  StoreDelegateConfig storeDelegateConfig;
  String chartName;
  String chartVersion;
  HelmVersion helmVersion;
  @Expression(ALLOW_SECRETS) HelmCommandFlag helmCommandFlag;
  private boolean checkIncorrectChartVersion;
  private boolean useCache;
  private boolean useRepoFlags;
  private boolean deleteRepoCacheDir;
  private boolean skipApplyHelmDefaultValues;
  String subChartName;

  @Override
  public ManifestType getManifestType() {
    return ManifestType.HELM_CHART;
  }
}
