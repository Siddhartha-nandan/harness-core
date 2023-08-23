/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.utils;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.core.service.entity.ServiceEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.CDC)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class CDGitXServiceImpl implements CDGitXService {
  GitSyncSdkService gitSyncSdkService;

  public boolean isNewGitXEnabledAndIsRemoteEntity(ServiceEntity serviceToSave, GitEntityInfo gitEntityInfo) {
    return isNewGitXEnabled(
               serviceToSave.getAccountId(), serviceToSave.getOrgIdentifier(), serviceToSave.getProjectIdentifier())
        && (isRemoteEntity(gitEntityInfo) || StoreType.REMOTE.equals(serviceToSave.getStoreType()));
  }

  private boolean isRemoteEntity(GitEntityInfo gitEntityInfo) {
    if (gitEntityInfo == null) {
      return false;
    }
    return StoreType.REMOTE.equals(gitEntityInfo.getStoreType());
  }

  @Override
  public boolean isNewGitXEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (projectIdentifier != null) {
      return isGitSimplificationEnabledForAProject(accountIdentifier, orgIdentifier, projectIdentifier);
    } else {
      return true;
    }
  }

  private boolean isGitSimplificationEnabledForAProject(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return gitSyncSdkService.isGitSimplificationEnabled(accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
