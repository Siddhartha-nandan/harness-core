/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import lombok.experimental.UtilityClass;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InstanceSyncPerpetualTaskInfoMapper {
  public InstanceSyncPerpetualTaskInfoDTO toDTO(InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo) {
    return InstanceSyncPerpetualTaskInfoDTO.builder()
        .id(instanceSyncPerpetualTaskInfo.getId())
        .accountIdentifier(instanceSyncPerpetualTaskInfo.getAccountIdentifier())
        .infrastructureMappingId(instanceSyncPerpetualTaskInfo.getInfrastructureMappingId())
        .deploymentInfoDetailsDTOList(DeploymentInfoDetailsMapper.toDeploymentInfoDetailsDTOList(
            instanceSyncPerpetualTaskInfo.getDeploymentInfoDetailsList()))
        .perpetualTaskId(instanceSyncPerpetualTaskInfo.getPerpetualTaskId())
        .perpetualTaskIdV2(instanceSyncPerpetualTaskInfo.getPerpetualTaskIdV2())
        .createdAt(instanceSyncPerpetualTaskInfo.getCreatedAt())
        .lastUpdatedAt(instanceSyncPerpetualTaskInfo.getLastUpdatedAt())
        .connectorIdentifier(instanceSyncPerpetualTaskInfo.getConnectorIdentifier())
        .lastSuccessfulRun(instanceSyncPerpetualTaskInfo.getLastSuccessfulRun())
        .build();
  }

  public InstanceSyncPerpetualTaskInfo toEntity(InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO) {
    return InstanceSyncPerpetualTaskInfo.builder()
        .id(instanceSyncPerpetualTaskInfoDTO.getId())
        .accountIdentifier(instanceSyncPerpetualTaskInfoDTO.getAccountIdentifier())
        .infrastructureMappingId(instanceSyncPerpetualTaskInfoDTO.getInfrastructureMappingId())
        .deploymentInfoDetailsList(DeploymentInfoDetailsMapper.toDeploymentInfoDetailsEntityList(
            instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()))
        .perpetualTaskId(instanceSyncPerpetualTaskInfoDTO.getPerpetualTaskId())
        .perpetualTaskIdV2(instanceSyncPerpetualTaskInfoDTO.getPerpetualTaskIdV2())
        .connectorIdentifier(instanceSyncPerpetualTaskInfoDTO.getConnectorIdentifier())
        .lastSuccessfulRun(instanceSyncPerpetualTaskInfoDTO.getLastSuccessfulRun())
        .build();
  }
}
