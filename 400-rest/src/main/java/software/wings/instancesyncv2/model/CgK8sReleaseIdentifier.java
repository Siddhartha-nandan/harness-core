/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.instancesyncv2.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
public class CgK8sReleaseIdentifier implements CgReleaseIdentifiers {
  private String namespace;
  private String releaseName;
  private String clusterName;
  private String containerServiceName;
  private boolean isHelmDeployment;
  @EqualsAndHashCode.Exclude private Set<DeploymentIdentifier> deploymentIdentifiers;
  @EqualsAndHashCode.Exclude private long deleteAfter;
}
