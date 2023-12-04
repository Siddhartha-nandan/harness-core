/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.exception.AccessDeniedException;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditPermissionValidator {
  @Inject private final AccessControlClient accessControlClient;
  private static final String ACCOUNT_RESOURCE_TYPE = "ACCOUNT";
  private static final String ORGANIZATION_RESOURCE_TYPE = "ORGANIZATION";
  private static final String PROJECT_RESOURCE_TYPE = "PROJECT";
  private static final String AUDIT_VIEW_PERMISSION = "core_audit_view";

  public void validate(String accountIdentifier, ResourceScopeDTO resourceScopeDTO) {
    checkPermissions(accountIdentifier, resourceScopeDTO);
  }

  private void checkPermissions(String accountIdentifier, ResourceScopeDTO resourceScopeDTO) {
    boolean hasAccess = hasAccountLevelPermission(accountIdentifier)
        || hasOrganizationLevelPermission(accountIdentifier, resourceScopeDTO.getOrgIdentifier());

    if (!hasAccess) {
      throw new AccessDeniedException(getAccessDeniedExceptionMessage(accountIdentifier,
                                          resourceScopeDTO.getOrgIdentifier(), resourceScopeDTO.getProjectIdentifier()),
          USER);
    }
  }

  private boolean hasAccountLevelPermission(String accountIdentifier) {
    if (isEmpty(accountIdentifier)) {
      return false;
    }
    return accessControlClient.hasAccess(null, Resource.of("AUDIT", null), AUDIT_VIEW_PERMISSION);
  }

  private boolean hasOrganizationLevelPermission(String accountIdentifier, String orgIdentifier) {
    if (isEmpty(accountIdentifier) || isEmpty(orgIdentifier)) {
      return false;
    }
    return accessControlClient.hasAccess(
        ResourceScope.of(accountIdentifier, null, null), Resource.of("AUDIT", null), AUDIT_VIEW_PERMISSION);
  }

  private String getAccessDeniedExceptionMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    String resourceType = "AUDIT";
    String resourceIdentifier = accountIdentifier;
    if (isNotEmpty(projectIdentifier)) {
      resourceIdentifier = projectIdentifier;
    } else if (isNotEmpty(orgIdentifier)) {
      resourceIdentifier = orgIdentifier;
    }
    return String.format("You need %s permission on %s with identifier: %s to perform this action",
        AUDIT_VIEW_PERMISSION, resourceType, resourceIdentifier);
  }
}
