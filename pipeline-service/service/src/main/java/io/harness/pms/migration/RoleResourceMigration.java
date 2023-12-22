/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.migration;

import static io.harness.authorization.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.beans.FeatureName.PIE_INPUT_SET_MIGRATION;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.account.utils.AccountUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeLevel;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ff.FeatureFlagService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.services.ProjectService;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.project.remote.ProjectClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.v1.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.v2.model.ResourceFilter;
import io.harness.resourcegroup.v2.model.ResourceSelector;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDC)
public class RoleResourceMigration implements Runnable {
  private final FeatureFlagService featureFlagService;
  private final AccountUtils accountUtils;
  private final PersistentLocker persistentLocker;
  private final String DEBUG_MESSAGE = "ProjectOrgBasicRoleCreationJob: ";
  private static final String LOCK_NAME = "ProjectOrgBasicRoleCreationJobLock";

  private static final String ORGANIZATION_VIEWER = "_organization_viewer";
  private static final String PROJECT_VIEWER = "_project_viewer";

  private static final String ORGANIZATION_BASIC = "_organization_basic";
  private static final String PROJECT_BASIC = "_project_basic";
  private final ProjectService projectService;
  private final AccessControlClient accessControlClient;
  private final PMSInputSetService pmsInputSetService;
  private final ResourceGroupClient resourceGroupClient;
  private final ProjectClient projectClient;

  @Inject
  public RoleResourceMigration(FeatureFlagService featureFlagService, AccountUtils accountUtils,
      PersistentLocker persistentLocker, ProjectService projectService, AccessControlClient accessControlClient,
      PMSInputSetService pmsInputSetService, ResourceGroupClient resourceGroupClient, ProjectClient projectClient) {
    this.featureFlagService = featureFlagService;
    this.accountUtils = accountUtils;
    this.persistentLocker = persistentLocker;
    this.projectService = projectService;
    this.accessControlClient = accessControlClient;
    this.pmsInputSetService = pmsInputSetService;
    this.resourceGroupClient = resourceGroupClient;
    this.projectClient = projectClient;
  }

  @Override
  public void run() {
    log.info(DEBUG_MESSAGE + "started...");
    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(LOCK_NAME, Duration.ofSeconds(5))) {
      if (lock == null) {
        log.info(DEBUG_MESSAGE + "failed to acquire lock");
        return;
      }
      try {
        SecurityContextBuilder.setContext(new ServicePrincipal(ACCESS_CONTROL_SERVICE.getServiceId()));
        log.info(DEBUG_MESSAGE + "Setting SecurityContext completed.");
        execute();
      } catch (Exception ex) {
        log.error(DEBUG_MESSAGE + " unexpected error occurred while Setting SecurityContext", ex);
      } finally {
        SecurityContextBuilder.unsetCompleteContext();
        log.info(DEBUG_MESSAGE + "Unsetting SecurityContext completed.");
      }
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + " failed to acquire lock", ex);
    }
    log.info(DEBUG_MESSAGE + " completed...");
  }

  @VisibleForTesting
  void execute() {
    List<String> targetAccounts = getAccountsForFFEnabled();
    for (String accountId : targetAccounts) {
      List<ResourceGroupResponse> resourceGroupResponses = getResourceGroupResource(accountId);
      if (EmptyPredicate.isNotEmpty(resourceGroupResponses)) {
        resourceGroupResponses = getResourceGroupResource(accountId);
        for (int i = 0; i < resourceGroupResponses.size(); i++) {
          ResourceGroupResponse response = resourceGroupResponses.get(i);
          if (null == response.getResourceGroup().getResourceFilter().getResources()) {
            break;
          }
          List<ResourceSelector> resourceSelectorList = response.getResourceGroup().getResourceFilter().getResources();
          for (int j = 0; j < resourceSelectorList.size(); j++) {
            validateAndUpdateInputSetPermission(response, resourceSelectorList.get(j));
          }
        }
      }
    }
  }

  private void validateAndUpdateInputSetPermission(ResourceGroupResponse response, ResourceSelector resourceSelector) {
    if (resourceSelector.getResourceType().equals("PIPELINE")) {
      ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder = createResourceGroupDTOBuilder(response);
      List<ResourceSelector> resourceSelectorList = response.getResourceGroup().getResourceFilter().getResources();
      if (resourceSelector.getIdentifiers() != null) {
        createSpecificInputSetPermission(response, resourceSelector, resourceSelectorList, resourceGroupDTOBuilder);
      } else {
        createAllInputSetPermission(response, resourceSelectorList, resourceGroupDTOBuilder);
      }
    }
  }

  private void createSpecificInputSetPermission(ResourceGroupResponse response, ResourceSelector resourceSelector,
      List<ResourceSelector> resourceSelectorList, ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    for (String identifier : resourceSelector.getIdentifiers()) {
      Criteria criteria = createCriteria(response);
      criteria.and(InputSetEntityKeys.pipelineIdentifier).is(identifier);
      List<InputSetEntity> inputSetEntities = pmsInputSetService.list(criteria);
      List<String> inputSetIdentifiers = new ArrayList<>();
      if (!inputSetEntities.isEmpty()) {
        inputSetEntities.forEach(
            inputSetEntity -> inputSetIdentifiers.add(identifier + "-" + inputSetEntity.getIdentifier()));
        ResourceSelector resourceSelectorNew =
            ResourceSelector.builder().resourceType("INPUT_SET").identifiers(inputSetIdentifiers).build();
        if (!resourceSelectorList.contains(resourceSelectorNew)) {
          resourceSelectorList.add(
              ResourceSelector.builder().resourceType("INPUT_SET").identifiers(inputSetIdentifiers).build());
          resourceGroupDTOBuilder.resourceFilter(ResourceFilter.builder().resources(resourceSelectorList).build());
          updateResourceGroup(response, resourceGroupDTOBuilder);
        }
      }
    }
  }

  private void createAllInputSetPermission(ResourceGroupResponse response, List<ResourceSelector> resourceSelectorList,
      ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    ResourceSelector resourceSelector = ResourceSelector.builder().resourceType("INPUT_SET").build();
    if (!resourceSelectorList.contains(resourceSelector)) {
      resourceSelectorList.add(resourceSelector);
      resourceGroupDTOBuilder.resourceFilter(ResourceFilter.builder().resources(resourceSelectorList).build());
      updateResourceGroup(response, resourceGroupDTOBuilder);
    }
  }

  private void updateResourceGroup(
      ResourceGroupResponse response, ResourceGroupDTO.ResourceGroupDTOBuilder resourceGroupDTOBuilder) {
    Optional<ResourceGroupResponse> resourceGroupResponse =
        Optional.ofNullable(NGRestUtils.getResponse(resourceGroupClient.updateResourceGroup(
            response.getResourceGroup().getIdentifier(), response.getResourceGroup().getAccountIdentifier(),
            response.getResourceGroup().getOrgIdentifier(), response.getResourceGroup().getProjectIdentifier(),
            ResourceGroupRequest.builder().resourceGroup(resourceGroupDTOBuilder.build()).build())));
    resourceGroupResponse.get();
  }
  private Criteria createCriteria(ResourceGroupResponse response) {
    Criteria criteria = new Criteria();
    criteria.and(InputSetEntityKeys.accountId).is(response.getResourceGroup().getAccountIdentifier());
    criteria.and(InputSetEntityKeys.orgIdentifier).is(response.getResourceGroup().getOrgIdentifier());
    criteria.and(InputSetEntityKeys.projectIdentifier).is(response.getResourceGroup().getProjectIdentifier());
    criteria.and(InputSetEntityKeys.deleted).is(false);
    return criteria;
  }

  private ResourceGroupDTO.ResourceGroupDTOBuilder createResourceGroupDTOBuilder(ResourceGroupResponse response) {
    return ResourceGroupDTO.builder()
        .identifier(response.getResourceGroup().getIdentifier())
        .name(response.getResourceGroup().getName())
        .projectIdentifier(response.getResourceGroup().getProjectIdentifier())
        .orgIdentifier(response.getResourceGroup().getOrgIdentifier())
        .accountIdentifier(response.getResourceGroup().getAccountIdentifier())
        .includedScopes(response.getResourceGroup().getIncludedScopes())
        .tags(response.getResourceGroup().getTags())
        .color(response.getResourceGroup().getColor())
        .description(response.getResourceGroup().getDescription())
        .allowedScopeLevels(Sets.newHashSet(
            ScopeLevel
                .of(response.getResourceGroup().getAccountIdentifier(), response.getResourceGroup().getOrgIdentifier(),
                    response.getResourceGroup().getProjectIdentifier())
                .toString()
                .toLowerCase()));
  }

  private List<String> getAccountsForFFEnabled() {
    List<String> accountIds = accountUtils.getAllAccountIds();
    List<String> targetAccounts = new ArrayList<>();
    List<ResourceGroupResponse> resourceGroupResponses = new ArrayList<>();
    try {
      for (String accountId : accountIds) {
        boolean isMigrateInputSetPermissionEnabled = featureFlagService.isEnabled(PIE_INPUT_SET_MIGRATION, accountId);
        if (isMigrateInputSetPermissionEnabled) {
          targetAccounts.add(accountId);
        }
      }
    } catch (Exception ex) {
      log.error("Failed to filter accounts for FF PL_REMOVE_USER_VIEWER_ROLE_ASSIGNMENTS");
    }
    return targetAccounts;
  }

  private List<ResourceGroupResponse> getResourceGroupResource(String accountId) {
    List<ResourceGroupResponse> resourceGroupResponses = new ArrayList<>();
    List<ProjectResponse> projects =
        getResponse(projectClient.listProject(accountId, null, false, null, null, 0, 1, null)).getContent();
    for (ProjectResponse projectResponse : projects) {
      ResourceGroupFilterDTO resourceGroupFilterDTO =
          ResourceGroupFilterDTO.builder()
              .accountIdentifier(accountId)
              .orgIdentifier(projectResponse.getProject().getOrgIdentifier())
              .projectIdentifier(projectResponse.getProject().getIdentifier())
              .build();
      resourceGroupResponses =
          getResponse(resourceGroupClient.getFilteredResourceGroups(resourceGroupFilterDTO, accountId, 0, 10))
              .getContent();
    }
    return resourceGroupResponses;
  }
}
