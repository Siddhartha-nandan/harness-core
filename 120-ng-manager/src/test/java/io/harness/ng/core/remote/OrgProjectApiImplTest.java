/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.DIFFERENT_IDENTIFIER_IN_PAYLOAD_AND_PARAM;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;
import static io.harness.utils.PageTestUtils.getPage;

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.accesscontrol.acl.api.AccessControlDTO;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.services.ScopeInfoService;
import io.harness.rule.Owner;
import io.harness.spec.server.ng.v1.model.CreateProjectRequest;
import io.harness.spec.server.ng.v1.model.ProjectResponse;
import io.harness.spec.server.ng.v1.model.UpdateProjectRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class OrgProjectApiImplTest extends CategoryTest {
  private ProjectService projectService;
  private OrganizationService organizationService;
  private AccessControlClient accessControlClient;
  private OrgProjectApiImpl orgProjectApi;
  private Validator validator;
  private ProjectApiUtils projectApiUtils;
  private ScopeInfoService scopeResolverService;

  String account = randomAlphabetic(10);
  String org = randomAlphabetic(10);
  String orgUniqueId = randomAlphabetic(10);
  String identifier = randomAlphabetic(10);
  String name = randomAlphabetic(10);
  int page = 0;
  int limit = 1;

  @Before
  public void setup() {
    projectService = mock(ProjectService.class);
    scopeResolverService = mock(ScopeInfoService.class);
    organizationService = mock(OrganizationService.class);
    accessControlClient = mock(AccessControlClient.class);

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
    projectApiUtils = new ProjectApiUtils(validator);

    orgProjectApi = new OrgProjectApiImpl(projectService, projectApiUtils, scopeResolverService);
  }

  private ProjectDTO getProjectDTO(String orgIdentifier, String identifier, String name) {
    return ProjectDTO.builder().orgIdentifier(orgIdentifier).identifier(identifier).name(name).build();
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testOrgScopedProjectCreate() {
    final String parentUniqueId = randomAlphabetic(10);
    CreateProjectRequest request = new CreateProjectRequest();
    io.harness.spec.server.ng.v1.model.ProjectRequest proj = new io.harness.spec.server.ng.v1.model.ProjectRequest();
    proj.setIdentifier(identifier);
    proj.setName(name);
    request.setProject(proj);

    ProjectDTO projectDTO = projectApiUtils.getProjectDto(request);
    Project project = toProject(projectDTO);
    project.setOrgIdentifier(org);
    project.setVersion(0L);
    project.setUniqueId(randomAlphabetic(10));
    project.setParentUniqueId(parentUniqueId);

    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueId)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    when(projectService.create(eq(account), any(), eq(projectDTO))).thenReturn(project);

    Response response = orgProjectApi.createOrgScopedProject(request, org, account);

    ArgumentCaptor<ScopeInfo> captor = ArgumentCaptor.forClass(ScopeInfo.class);
    verify(projectService, times(1)).create(eq(account), captor.capture(), eq(projectDTO));
    ScopeInfo actualScopeInfo = captor.getValue();
    assertEquals(scopeInfo.getScopeType(), actualScopeInfo.getScopeType());
    assertEquals(scopeInfo.getAccountIdentifier(), actualScopeInfo.getAccountIdentifier());
    assertEquals(scopeInfo.getOrgIdentifier(), actualScopeInfo.getOrgIdentifier());
    assertEquals(scopeInfo.getUniqueId(), actualScopeInfo.getUniqueId());

    assertEquals(201, response.getStatus());

    assertEquals(project.getVersion().toString(), response.getEntityTag().getValue());
    ProjectResponse entity = (ProjectResponse) response.getEntity();

    assertEquals(identifier, entity.getProject().getIdentifier());
    assertEquals(org, entity.getProject().getOrg());
  }

  @Test(expected = NotFoundException.class)
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetOrgScopedProjectNotFoundException() {
    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueId)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    orgProjectApi.getOrgScopedProject(org, identifier, account);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetOrgScopedProject() {
    Project project = Project.builder().identifier(identifier).name(name).orgIdentifier(org).version(0L).build();
    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueId)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    when(projectService.get(account, scopeInfo, identifier)).thenReturn(Optional.of(project));

    Response response = orgProjectApi.getOrgScopedProject(org, identifier, account);

    ProjectResponse entity = (ProjectResponse) response.getEntity();

    assertEquals(identifier, entity.getProject().getIdentifier());
    assertEquals(org, entity.getProject().getOrg());
    assertEquals(project.getVersion().toString(), response.getEntityTag().getValue());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetOrgScopedProjectList() {
    String searchTerm = randomAlphabetic(10);
    ProjectDTO projectDTO = getProjectDTO(org, identifier, name);
    Project project = toProject(projectDTO);
    project.setVersion((long) 0);
    ArgumentCaptor<ProjectFilterDTO> argumentCaptor = ArgumentCaptor.forClass(ProjectFilterDTO.class);

    when(projectService.listPermittedProjects(eq(account), any(), any(), any()))
        .thenReturn(getPage(Collections.singletonList(project), 1));

    when(accessControlClient.checkForAccess(anyList()))
        .thenReturn(
            AccessCheckResponseDTO.builder()
                .accessControlList(Collections.singletonList(AccessControlDTO.builder()
                                                                 .resourceIdentifier(null)
                                                                 .resourceScope(ResourceScope.of(account, org, null))
                                                                 .permitted(true)
                                                                 .build()))
                .build());

    Response response = orgProjectApi.getOrgScopedProjects(org, Collections.singletonList(identifier), true,
        io.harness.spec.server.ng.v1.model.ModuleType.CD.name(), Boolean.FALSE, searchTerm, page, limit, account, null,
        null);

    verify(projectService, times(1)).listPermittedProjects(eq(account), any(), argumentCaptor.capture(), any());
    ProjectFilterDTO projectFilterDTO = argumentCaptor.getValue();

    List<ProjectResponse> entity = (List<ProjectResponse>) response.getEntity();

    assertEquals(searchTerm, projectFilterDTO.getSearchTerm());
    assertEquals(ModuleType.CD, projectFilterDTO.getModuleType());
    assertEquals(3, response.getHeaders().size());
    assertEquals(1, entity.size());
    assertEquals(org, entity.get(0).getProject().getOrg());
    assertEquals(identifier, entity.get(0).getProject().getIdentifier());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateOrgScopedProject() {
    UpdateProjectRequest request = new UpdateProjectRequest();
    io.harness.spec.server.ng.v1.model.ProjectRequest proj = new io.harness.spec.server.ng.v1.model.ProjectRequest();
    proj.setIdentifier(identifier);
    proj.setName("updated_name");
    request.setProject(proj);

    ProjectDTO projectDTO = projectApiUtils.getProjectDto(request);
    Project project = toProject(projectDTO);
    project.setVersion(0L);
    project.setOrgIdentifier(org);

    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueId)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    when(projectService.update(eq(account), any(), eq(identifier), eq(projectDTO))).thenReturn(project);

    Response response = orgProjectApi.updateOrgScopedProject(request, org, identifier, account);

    ArgumentCaptor<ScopeInfo> captor = ArgumentCaptor.forClass(ScopeInfo.class);
    verify(projectService, times(1)).update(eq(account), captor.capture(), eq(identifier), eq(projectDTO));
    ScopeInfo actualScopeInfo = captor.getValue();
    assertEquals(scopeInfo.getScopeType(), actualScopeInfo.getScopeType());
    assertEquals(scopeInfo.getAccountIdentifier(), actualScopeInfo.getAccountIdentifier());
    assertEquals(scopeInfo.getOrgIdentifier(), actualScopeInfo.getOrgIdentifier());
    assertEquals(scopeInfo.getUniqueId(), actualScopeInfo.getUniqueId());

    ProjectResponse entity = (ProjectResponse) response.getEntity();

    assertEquals(project.getVersion().toString(), response.getEntityTag().getValue());
    assertEquals(org, entity.getProject().getOrg());
    assertEquals(identifier, entity.getProject().getIdentifier());
    assertEquals("updated_name", entity.getProject().getName());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testUpdateOrgScopedProjectIdentifierMisMatch() {
    UpdateProjectRequest request = new UpdateProjectRequest();
    io.harness.spec.server.ng.v1.model.ProjectRequest proj = new io.harness.spec.server.ng.v1.model.ProjectRequest();
    proj.setIdentifier(identifier);
    proj.setName("updated_name");
    request.setProject(proj);

    Throwable thrown =
        catchThrowableOfType(()
                                 -> orgProjectApi.updateOrgScopedProject(request, org, "different-identifier", account),
            InvalidRequestException.class);

    assertThat(thrown).hasMessage(DIFFERENT_IDENTIFIER_IN_PAYLOAD_AND_PARAM);
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testOrgScopedProjectDelete() {
    Project project = Project.builder().identifier(identifier).name(name).build();

    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueId)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    when(projectService.delete(eq(account), any(ScopeInfo.class), eq(identifier), isNull())).thenReturn(true);
    when(projectService.get(account, scopeInfo, identifier)).thenReturn(Optional.of(project));

    Response response = orgProjectApi.deleteOrgScopedProject(org, identifier, account);

    ArgumentCaptor<ScopeInfo> captor = ArgumentCaptor.forClass(ScopeInfo.class);
    verify(projectService, times(1)).delete(eq(account), captor.capture(), eq(identifier), isNull());
    ScopeInfo actualScopeInfo = captor.getValue();
    assertEquals(scopeInfo.getScopeType(), actualScopeInfo.getScopeType());
    assertEquals(scopeInfo.getAccountIdentifier(), actualScopeInfo.getAccountIdentifier());
    assertEquals(scopeInfo.getOrgIdentifier(), actualScopeInfo.getOrgIdentifier());
    assertEquals(scopeInfo.getUniqueId(), actualScopeInfo.getUniqueId());

    ProjectResponse entity = (ProjectResponse) response.getEntity();

    assertEquals(identifier, entity.getProject().getIdentifier());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testOrgScopedProjectNotDeleted() {
    String orgUniqueIdentifier = randomAlphabetic(10);
    Project project = Project.builder().identifier(identifier).name(name).build();
    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueIdentifier)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    when(projectService.delete(account, scopeInfo, identifier, null)).thenReturn(false);
    when(projectService.get(account, scopeInfo, identifier)).thenReturn(Optional.of(project));

    Throwable thrown = catchThrowableOfType(
        () -> orgProjectApi.deleteOrgScopedProject(org, identifier, account), NotFoundException.class);

    assertThat(thrown).hasMessage(format("Project with identifier [%s] could not be deleted", identifier));
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testOrgScopedProjectDeleteNotFoundException() {
    String orgUniqueIdentifier = randomAlphabetic(10);
    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier(account)
                              .scopeType(ScopeLevel.ORGANIZATION)
                              .orgIdentifier(org)
                              .uniqueId(orgUniqueIdentifier)
                              .build();
    when(scopeResolverService.getScopeInfo(account, org, null)).thenReturn(Optional.of(scopeInfo));
    Throwable thrown = catchThrowableOfType(
        () -> orgProjectApi.deleteOrgScopedProject(org, identifier, account), NotFoundException.class);

    assertThat(thrown).hasMessage(format("Project with org [%s] and identifier [%s] not found", org, identifier));
  }
}
