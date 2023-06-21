/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class HelmChartManifest {
  private String apiVersion;
  private String name;
  private String version;
  @Nullable private String kubeVersion;
  @Nullable private String description;
  @Nullable private String type;
  @Nullable private String appVersion;
  @Nullable private Map<String, String> annotations;
  private Map<String, String> metadata;
}
