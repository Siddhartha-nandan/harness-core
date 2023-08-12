/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles.api;

import static io.harness.accesscontrol.AccessControlPermissions.DELETE_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.EDIT_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.VIEW_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlResourceTypes.ROLE;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.roles.RoleDTOMapper.fromDTO;
import static io.harness.accesscontrol.roles.api.RoleDTO.ScopeLevel.fromString;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleDTOMapper;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeMapper;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.spec.server.accesscontrol.v1.ProjectRolesApi;
import io.harness.spec.server.accesscontrol.v1.model.CreateRoleRequest;
import io.harness.spec.server.accesscontrol.v1.model.RolesResponse;
import io.harness.utils.ApiUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class ProjectRolesApiImpl implements ProjectRolesApi {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;
  private final AccessControlClient accessControlClient;
  private final RolesApiUtils rolesApiUtils;

  @Inject
  public ProjectRolesApiImpl(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper,
      AccessControlClient accessControlClient, RolesApiUtils rolesApiUtils) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
    this.accessControlClient = accessControlClient;
    this.rolesApiUtils = rolesApiUtils;
  }

  @Override
  @FeatureRestrictionCheck(FeatureRestrictionName.CUSTOM_ROLES)
  public Response createRoleProject(
      CreateRoleRequest body, String org, String project, @AccountIdentifier String account) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(ROLE, null), EDIT_ROLE_PERMISSION);
    HarnessScopeParams harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(account).orgIdentifier(org).projectIdentifier(project).build();
    Scope scope = scopeService.getOrCreate(ScopeMapper.fromParams(harnessScopeParams));
    RoleDTO roleDTO = rolesApiUtils.getRoleProjectDTO(body);
    roleDTO.setAllowedScopeLevels(Sets.newHashSet(fromString(scope.getLevel().toString())));
    RoleResponseDTO responseDTO = roleDTOMapper.toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO)));
    RolesResponse response = RolesApiUtils.getRolesResponse(responseDTO);
    return Response.status(201).entity(response).build();
  }

  @Override
  public Response deleteRoleProject(String org, String project, String role, String account) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(ROLE, role), DELETE_ROLE_PERMISSION);
    HarnessScopeParams harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(account).orgIdentifier(org).projectIdentifier(project).build();
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleResponseDTO responseDTO = roleDTOMapper.toResponseDTO(roleService.delete(role, scopeIdentifier));
    RolesResponse response = RolesApiUtils.getRolesResponse(responseDTO);
    return Response.ok().entity(response).build();
  }

  @Override
  public Response getRoleProject(String org, String project, String role, String account) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(ROLE, role), VIEW_ROLE_PERMISSION);
    HarnessScopeParams harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(account).orgIdentifier(org).projectIdentifier(project).build();
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RolesResponse response = RolesApiUtils.getRolesResponse(roleDTOMapper.toResponseDTO(
        roleService.get(role, scopeIdentifier, NO_FILTER).<InvalidRequestException>orElseThrow(() -> {
          throw new InvalidRequestException("Role not found in Project scope for given identifier.");
        })));
    return Response.ok().entity(response).build();
  }

  @Override
  public Response listRolesProject(String org, String project, Integer page, Integer limit, String searchTerm,
      String account, String sort, String order) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    HarnessScopeParams harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(account).orgIdentifier(org).projectIdentifier(project).build();
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleFilter roleFilter =
        RoleFilter.builder().searchTerm(searchTerm).scopeIdentifier(scopeIdentifier).managedFilter(NO_FILTER).build();
    PageRequest pageRequest = ApiUtils.getPageRequest(page, limit, sort, order);
    PageResponse<Role> pageResponse = roleService.list(pageRequest, roleFilter, true);
    ResponseBuilder responseBuilder = Response.ok();
    ResponseBuilder responseBuilderWithLinks =
        ApiUtils.addLinksHeader(responseBuilder, pageResponse.getTotalItems(), page, limit);
    return responseBuilderWithLinks
        .entity(pageResponse.getContent()
                    .stream()
                    .map(role -> RolesApiUtils.getRolesResponse(roleDTOMapper.toResponseDTO(role)))
                    .collect(Collectors.toList()))
        .build();
  }

  @Override
  public Response updateRoleProject(CreateRoleRequest body, String org, String project, String role, String account) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(ROLE, role), EDIT_ROLE_PERMISSION);
    HarnessScopeParams harnessScopeParams =
        HarnessScopeParams.builder().accountIdentifier(account).orgIdentifier(org).projectIdentifier(project).build();
    if (!role.equals(body.getIdentifier())) {
      throw new InvalidRequestException("Role identifier in the request body and the URL do not match.");
    }
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    Role roleUpdated = roleService.update(fromDTO(scopeIdentifier, rolesApiUtils.getRoleProjectDTO(body)));
    RoleResponseDTO responseDTO = roleDTOMapper.toResponseDTO(roleUpdated);
    RolesResponse response = RolesApiUtils.getRolesResponse(responseDTO);
    return Response.ok().entity(response).build();
  }
}
