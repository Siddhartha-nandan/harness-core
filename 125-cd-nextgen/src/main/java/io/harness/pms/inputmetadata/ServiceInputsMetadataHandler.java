/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.inputmetadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.encryption.Scope;
import io.harness.ng.core.beans.ServiceV2YamlMetadata;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestMetadata;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestProto;
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataHandler;
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataResponse;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@OwnedBy(CDC)
@Singleton
public class ServiceInputsMetadataHandler implements InputsMetadataHandler {
  @Inject ServiceEntityService serviceEntityService;

  @Override
  public InputsMetadataResponse generateInputsMetadata(
      InputsMetadataRequestProto request, InputsMetadataRequestMetadata metadata) {
    String accountId = metadata.getAccountId();
    String orgId = metadata.getOrgId();
    String projectId = metadata.getProjectId();
    String scopedServiceId = request.getEntityId();

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(scopedServiceId, accountId, orgId, projectId);
    Scope scope = identifierRef.getScope();
    Optional<ServiceEntity> optService;
    switch (scope) {
      case ACCOUNT:
        optService = getService(accountId, null, null, identifierRef.getIdentifier());
        break;
      case ORG:
        optService = getService(accountId, orgId, null, identifierRef.getIdentifier());
        break;
      case PROJECT:
        optService = getService(accountId, orgId, projectId, identifierRef.getIdentifier());
        break;
      default:
        return sendErrorResponseForNotFoundService(scopedServiceId);
    }

    if (optService.isEmpty()) {
      return sendErrorResponseForNotFoundService(scopedServiceId);
    }
    ServiceEntity service = optService.get();
    ServiceV2YamlMetadata serviceMetadata = serviceEntityService.createServiceV2YamlMetadata(service);
    return InputsMetadataResponse.builder().success(true).result(serviceMetadata.getFqnToInputsMetadataMap()).build();
  }

  Optional<ServiceEntity> getService(String accountId, String orgId, String projectId, String serviceId) {
    return serviceEntityService.get(accountId, orgId, projectId, serviceId, false);
  }

  InputsMetadataResponse sendErrorResponseForNotFoundService(String service) {
    return InputsMetadataResponse.builder().success(false).errorMessage("Could not find service: " + service).build();
  }
}
