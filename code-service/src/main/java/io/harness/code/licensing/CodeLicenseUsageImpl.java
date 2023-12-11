/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.code.licensing;

import static io.harness.audit.ResourceTypeConstants.CODE_REPOSITORY;

import io.harness.ModuleType;
import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.accesscontrol.acl.api.AccessControlDTO;
import io.harness.accesscontrol.acl.api.PermissionCheckDTO;
import io.harness.accesscontrol.acl.api.Principal;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.licensing.usage.params.DefaultPageableUsageRequestParams;
import io.harness.licensing.usage.params.PageableUsageRequestParams;
import io.harness.licensing.usage.params.UsageRequestParams;
import io.harness.utils.PageUtils;

import software.wings.beans.User;
import software.wings.service.impl.UserServiceImpl;

import com.google.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class CodeLicenseUsageImpl implements LicenseUsageInterface<CodeLicenseUsageDTO, UsageRequestParams> {
  @Inject protected FeatureFlagService featureFlagService;
  @Inject UserServiceImpl userServiceImpl;
  @Inject AccessControlClient accessControlClient;

  @Override
  public CodeLicenseUsageDTO getLicenseUsage(
      String accountIdentifier, ModuleType module, long timestamp, UsageRequestParams usageRequest) {
    if (!ModuleType.CODE.equals(module) || !featureFlagService.isEnabled(FeatureName.CODE_ENABLED, accountIdentifier)) {
      return null;
    }

    int activeDevelopersCount = getActiveDevelopersByAccountID(accountIdentifier).size();

    return CodeLicenseUsageDTO.builder()
        .accountIdentifier(accountIdentifier)
        .module(module.getDisplayName())
        .timestamp(System.currentTimeMillis())
        .activeDevelopers(UsageDataDTO.builder()
                              .displayName("Total users with access to CODE service")
                              .count(activeDevelopersCount)
                              .build())
        .build();
  }

  @Override
  public Page<CodeActiveDeveloperDTO> listLicenseUsage(
      String accountIdentifier, ModuleType module, long currentTS, PageableUsageRequestParams usageRequest) {
    if (!ModuleType.CODE.equals(module) || !featureFlagService.isEnabled(FeatureName.CODE_ENABLED, accountIdentifier)) {
      return null;
    }

    List<CodeActiveDeveloperDTO> activeDeveloperDTOS = getActiveDevelopersByAccountID(accountIdentifier);
    if (activeDeveloperDTOS.isEmpty()) {
      return null;
    }

    DefaultPageableUsageRequestParams defaultUsageRequestParams = (DefaultPageableUsageRequestParams) usageRequest;
    Pageable pageRequest = defaultUsageRequestParams.getPageRequest();

    return PageUtils.getPage(activeDeveloperDTOS, pageRequest.getPageNumber(), pageRequest.getPageSize());
  }

  @Override
  public File getLicenseUsageCSVReport(String accountIdentifier, ModuleType moduleType, long currentTsInMs) {
    return null;
  }

  private List<CodeActiveDeveloperDTO> getActiveDevelopersByAccountID(String accountIdentifier) {
    List<CodeActiveDeveloperDTO> codeActiveDeveloperDTOList = new ArrayList<>();
    List<User> users = userServiceImpl.getUsersOfAccount(accountIdentifier);
    if (users == null || users.isEmpty()) {
      return codeActiveDeveloperDTOList;
    }

    List<PermissionCheckDTO> permissionCheckDTOList = new ArrayList<>();
    permissionCheckDTOList.add(
        PermissionCheckDTO.builder().resourceType(CODE_REPOSITORY).permission("code_repo_create").build());
    permissionCheckDTOList.add(
        PermissionCheckDTO.builder().resourceType(CODE_REPOSITORY).permission("code_repo_view").build());
    permissionCheckDTOList.add(
        PermissionCheckDTO.builder().resourceType(CODE_REPOSITORY).permission("code_repo_edit").build());
    permissionCheckDTOList.add(
        PermissionCheckDTO.builder().resourceType(CODE_REPOSITORY).permission("code_repo_delete").build());

    for (User user : users) {
      AccessCheckResponseDTO accessCheckResponseDTO = accessControlClient.checkForAccess(
          Principal.builder().principalIdentifier(user.getUuid()).principalType(PrincipalType.USER).build(),
          permissionCheckDTOList);

      long accessPermittedCount =
          accessCheckResponseDTO.getAccessControlList().stream().filter(AccessControlDTO::isPermitted).count();
      if (accessPermittedCount == 0) {
        continue;
      }

      codeActiveDeveloperDTOList.add(CodeActiveDeveloperDTO.builder()
                                         .identifier(user.getUuid())
                                         .email(user.getEmail())
                                         .name(user.getName())
                                         .build());
    }

    return codeActiveDeveloperDTOList;
  }
}
