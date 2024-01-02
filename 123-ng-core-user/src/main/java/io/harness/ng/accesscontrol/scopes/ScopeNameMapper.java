/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.accesscontrol.scopes;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.services.ScopeInfoService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
@Singleton
public class ScopeNameMapper {
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final ScopeInfoService scopeResolverService;

  @Inject
  public ScopeNameMapper(
      OrganizationService organizationService, ProjectService projectService, ScopeInfoService scopeResolverService) {
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.scopeResolverService = scopeResolverService;
  }

  public ScopeNameDTO toScopeNameDTO(@NotNull ScopeDTO scopeDTO) {
    String orgName = null;
    String projectName = null;
    if (!isBlank(scopeDTO.getOrgIdentifier())) {
      orgName = organizationService
                    .get(scopeDTO.getAccountIdentifier(),
                        ScopeInfo.builder()
                            .accountIdentifier(scopeDTO.getAccountIdentifier())
                            .scopeType(ScopeLevel.ACCOUNT)
                            .uniqueId(scopeDTO.getAccountIdentifier())
                            .build(),
                        scopeDTO.getOrgIdentifier())
                    .<InvalidRequestException>orElseThrow(() -> {
                      throw new InvalidRequestException(String.format(
                          "Organization details not found for org Identifier: [%s]", scopeDTO.getOrgIdentifier()));
                    })
                    .getName();
    }
    if (!isBlank(scopeDTO.getProjectIdentifier())) {
      Optional<ScopeInfo> scopeInfo =
          scopeResolverService.getScopeInfo(scopeDTO.getAccountIdentifier(), scopeDTO.getOrgIdentifier(), null);
      projectName =
          projectService.get(scopeDTO.getAccountIdentifier(), scopeInfo.orElseThrow(), scopeDTO.getProjectIdentifier())
              .<InvalidRequestException>orElseThrow(() -> {
                throw new InvalidRequestException(String.format(
                    "Project details not found for project Identifier: [%s]", scopeDTO.getProjectIdentifier()));
              })
              .getName();
    }
    return ScopeNameDTO.builder()
        .accountIdentifier(scopeDTO.getAccountIdentifier())
        .orgIdentifier(scopeDTO.getOrgIdentifier())
        .projectIdentifier(scopeDTO.getProjectIdentifier())
        .orgName(orgName)
        .projectName(projectName)
        .build();
  }

  public static ScopeDTO fromScopeNameDTO(ScopeNameDTO scopeNameDTO) {
    return ScopeDTO.builder()
        .accountIdentifier(scopeNameDTO.getAccountIdentifier())
        .orgIdentifier(scopeNameDTO.getOrgIdentifier())
        .projectIdentifier(scopeNameDTO.getProjectIdentifier())
        .build();
  }
}
