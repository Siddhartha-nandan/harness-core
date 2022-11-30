/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.dto;

import java.util.List;
import lombok.*;

@Value
@Builder
public class InstanceGroupedByServiceList {
  List<InstanceGroupedByService> instanceGroupedByServiceList;

  @Value
  @Builder
  public static class InstanceGroupedByService {
    String serviceId;
    String serviceName;
    List<InstanceGroupedByArtifactV2> instanceGroupedByArtifactList;
  }

  @Value
  @Builder
  public static class InstanceGroupedByArtifactV2 {
    String artifactVersion;
    String artifactPath;
    boolean isLatest;
    List<InstanceGroupedByEnvironmentV2> instanceGroupedByEnvironmentList;
  }

  @Value
  @Builder
  public static class InstanceGroupedByEnvironmentV2 {
    String envId;
    String envName;
    List<InstanceGroupedByInfrastructureV2> instanceGroupedByInfraList;
    List<InstanceGroupedByInfrastructureV2> instanceGroupedByClusterList;
  }

  @Value
  @Builder
  public static class InstanceGroupedByInfrastructureV2 {
    String infraIdentifier;
    String infraName;
    String clusterIdentifier;
    String agentIdentifier;
    List<InstanceGroupedByPipelineExecution> instanceGroupedByPipelineExecutionList;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class InstanceGroupedByPipelineExecution {
    Integer count;
    String lastPipelineExecutionId;
    String lastPipelineExecutionName;
    Long lastDeployedAt;
  }
}
