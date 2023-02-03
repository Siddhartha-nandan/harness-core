/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.beans;

import io.harness.cvng.core.entities.CVConfig;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

@Value
@Builder
public class ResolvedCVConfigInfo {
  @Nullable String monitoredServiceIdentifier;
  @Nullable String monitoredServiceTemplateIdentifier;
  @Nullable String monitoredServiceTemplateVersionLabel;
  @Nullable List<CVConfig> cvConfigs;
  List<HealthSourceInfo> healthSources;

  @Value
  @Builder
  public static class HealthSourceInfo {
    String connectorRef;
    String identifier;
    boolean demoEnabledForAnyCVConfig;
  }

  public List<String> getCvConfigIds() {
    return CollectionUtils.emptyIfNull(cvConfigs).stream().map(CVConfig::getUuid).collect(Collectors.toList());
  }
}
