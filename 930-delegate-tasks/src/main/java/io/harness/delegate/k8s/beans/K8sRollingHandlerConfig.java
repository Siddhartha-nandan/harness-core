/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.k8s.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.KubernetesResource;

import io.kubernetes.client.openapi.models.V1Secret;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class K8sRollingHandlerConfig extends K8sHandlerConfig {
  private List<KubernetesResource> managedWorkloads;
  private List<KubernetesResource> customWorkloads;
  private List<V1Secret> releaseHistory;
  private V1Secret release;
  private int currentReleaseNumber;
}
