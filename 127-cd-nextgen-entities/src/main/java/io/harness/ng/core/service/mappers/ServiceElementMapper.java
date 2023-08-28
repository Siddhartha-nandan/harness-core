/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.service.mappers;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.ScmException;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.sdk.CacheResponse;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceBasicInfo;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.yaml.NGServiceConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.ng.core.template.CacheResponseMetadataDTO;

import lombok.experimental.UtilityClass;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@OwnedBy(PIPELINE)
@UtilityClass
public class ServiceElementMapper {
  public static final String BOOLEAN_TRUE_VALUE = "true";
  public ServiceEntity toServiceEntity(String accountId, ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity serviceEntity = ServiceEntity.builder()
                                      .identifier(serviceRequestDTO.getIdentifier())
                                      .accountId(accountId)
                                      .orgIdentifier(serviceRequestDTO.getOrgIdentifier())
                                      .projectIdentifier(serviceRequestDTO.getProjectIdentifier())
                                      .name(serviceRequestDTO.getName())
                                      .description(serviceRequestDTO.getDescription())
                                      .tags(convertToList(serviceRequestDTO.getTags()))
                                      .yaml(serviceRequestDTO.getYaml())
                                      .build();
    // This also validates the service yaml
    final NGServiceConfig ngServiceConfig = NGServiceEntityMapper.toNGServiceConfig(serviceEntity);
    final NGServiceV2InfoConfig ngServiceV2InfoConfig = ngServiceConfig.getNgServiceV2InfoConfig();
    if (isEmpty(serviceEntity.getYaml())) {
      serviceEntity.setYaml(NGServiceEntityMapper.toYaml(ngServiceConfig));
    }
    serviceEntity.setGitOpsEnabled(ngServiceV2InfoConfig.getGitOpsEnabled());
    if (ngServiceV2InfoConfig.getServiceDefinition() != null) {
      serviceEntity.setType(ngServiceV2InfoConfig.getServiceDefinition().getType());
    }
    return serviceEntity;
  }

  public ServiceResponseDTO writeDTO(ServiceEntity serviceEntity) {
    return writeDTO(serviceEntity, false);
  }

  public ServiceResponseDTO writeDTO(ServiceEntity serviceEntity, boolean includeVersionInfo) {
    ServiceResponseDTO serviceResponseDTO = ServiceResponseDTO.builder()
                                                .accountId(serviceEntity.getAccountId())
                                                .orgIdentifier(serviceEntity.getOrgIdentifier())
                                                .projectIdentifier(serviceEntity.getProjectIdentifier())
                                                .identifier(serviceEntity.getIdentifier())
                                                .name(serviceEntity.getName())
                                                .description(serviceEntity.getDescription())
                                                .deleted(serviceEntity.getDeleted())
                                                .tags(convertToMap(serviceEntity.getTags()))
                                                .version(serviceEntity.getVersion())
                                                .yaml(serviceEntity.getYaml())
                                                .entityGitDetails(getEntityGitDetails(serviceEntity))
                                                .storeType(serviceEntity.getStoreType())
                                                .connectorRef(serviceEntity.getConnectorRef())
                                                .cacheResponseMetadataDTO(getCacheResponse(serviceEntity))
                                                .build();

    if (includeVersionInfo && serviceEntity.getType() != null) {
      serviceResponseDTO.setV2Service(true);
    }
    return serviceResponseDTO;
  }

  public ScmException getScmException(Throwable ex) {
    while (ex != null) {
      if (ex instanceof ScmException) {
        return (ScmException) ex;
      }
      ex = ex.getCause();
    }
    return null;
  }

  public EntityGitDetails getEntityGitDetails(ServiceEntity serviceEntity) {
    EntityGitDetails entityGitDetails;
    if (serviceEntity.getStoreType() == null) {
      entityGitDetails = EntityGitDetailsMapper.mapEntityGitDetails(serviceEntity);
    } else if (serviceEntity.getStoreType() == StoreType.REMOTE) {
      entityGitDetails = GitAwareContextHelper.getEntityGitDetailsFromScmGitMetadata();
    } else {
      entityGitDetails = EntityGitDetails.builder().build();
    }

    return entityGitDetails;
  }

  public CacheResponseMetadataDTO getCacheResponse(ServiceEntity serviceEntity) {
    if (serviceEntity.getStoreType() == StoreType.REMOTE) {
      CacheResponse cacheResponse = GitAwareContextHelper.getCacheResponseFromScmGitMetadata();

      if (cacheResponse != null) {
        return createCacheResponseMetadataDTO(cacheResponse);
      }
    }

    return null;
  }

  public boolean parseLoadFromCacheHeaderParam(String loadFromCache) {
    if (isEmpty(loadFromCache)) {
      return false;
    } else {
      return BOOLEAN_TRUE_VALUE.equalsIgnoreCase(loadFromCache);
    }
  }

  private CacheResponseMetadataDTO createCacheResponseMetadataDTO(CacheResponse cacheResponse) {
    return CacheResponseMetadataDTO.builder()
        .cacheState(cacheResponse.getCacheState())
        .ttlLeft(cacheResponse.getTtlLeft())
        .lastUpdatedAt(cacheResponse.getLastUpdatedAt())
        .build();
  }

  public ServiceResponseDTO writeAccessListDTO(ServiceEntity serviceEntity) {
    return ServiceResponseDTO.builder()
        .accountId(serviceEntity.getAccountId())
        .orgIdentifier(serviceEntity.getOrgIdentifier())
        .projectIdentifier(serviceEntity.getProjectIdentifier())
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .description(serviceEntity.getDescription())
        .deleted(serviceEntity.getDeleted())
        .tags(convertToMap(serviceEntity.getTags()))
        .version(serviceEntity.getVersion())
        .build();
  }

  public ServiceResponse toResponseWrapper(ServiceEntity serviceEntity) {
    return ServiceResponse.builder()
        .service(writeDTO(serviceEntity))
        .createdAt(serviceEntity.getCreatedAt())
        .lastModifiedAt(serviceEntity.getLastModifiedAt())
        .build();
  }

  public ServiceResponse toResponseWrapper(ServiceEntity serviceEntity, boolean includeVersionInfo) {
    return ServiceResponse.builder()
        .service(writeDTO(serviceEntity, includeVersionInfo))
        .createdAt(serviceEntity.getCreatedAt())
        .lastModifiedAt(serviceEntity.getLastModifiedAt())
        .build();
  }

  public ServiceResponse toAccessListResponseWrapper(ServiceEntity serviceEntity) {
    return ServiceResponse.builder()
        .service(writeAccessListDTO(serviceEntity))
        .createdAt(serviceEntity.getCreatedAt())
        .lastModifiedAt(serviceEntity.getLastModifiedAt())
        .build();
  }

  public ServiceBasicInfo toBasicInfo(ServiceEntity serviceEntity) {
    return ServiceBasicInfo.builder()
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .description(serviceEntity.getDescription())
        .accountIdentifier(serviceEntity.getAccountId())
        .orgIdentifier(serviceEntity.getOrgIdentifier())
        .projectIdentifier(serviceEntity.getProjectIdentifier())
        .tags(convertToMap(serviceEntity.getTags()))
        .build();
  }
}
