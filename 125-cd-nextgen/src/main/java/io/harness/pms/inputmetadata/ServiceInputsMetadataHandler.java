/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.inputmetadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestMetadata;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestProto;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataHandler;
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Optional;

@OwnedBy(CDC)
@Singleton
public class ServiceInputsMetadataHandler implements InputsMetadataHandler {
  @Inject PmsGitSyncHelper gitSyncHelper;
  @Inject ServiceEntityService serviceEntityService;

  @Override
  public InputsMetadataResponse generateInputsMetadata(
      InputsMetadataRequestProto request, InputsMetadataRequestMetadata metadata) {
    String accountId = metadata.getAccountId();
    String orgId = metadata.getOrgId();
    String projectId = metadata.getProjectId();
    String scopedServiceRef = request.getEntityId();
    String runtimeInputYaml = request.getInputFormYaml().toStringUtf8();
    ByteString gitSyncBranchContext = metadata.getGitSyncBranchContext();
    Optional<ServiceEntity> optService;
    try (PmsGitSyncBranchContextGuard ignore =
             gitSyncHelper.createGitSyncBranchContextGuardFromBytes(gitSyncBranchContext, true)) {
      optService = serviceEntityService.get(accountId, orgId, projectId, scopedServiceRef, false);
    }
    if (optService.isEmpty()) {
      return sendErrorResponseForNotFoundService(scopedServiceRef);
    }
    ServiceEntity service = optService.get();
    return InputsMetadataResponse.builder()
        .success(true)
        .result(serviceEntityService.getServiceInputsMetadata(runtimeInputYaml, service))
        .build();
  }

  InputsMetadataResponse sendErrorResponseForNotFoundService(String service) {
    return InputsMetadataResponse.builder().success(false).errorMessage("Could not find service: " + service).build();
  }
}
