/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.worker;

import static io.harness.NGConstants.ACCOUNT_VIEWER_ROLE;
import static io.harness.NGConstants.DEFAULT_ACCOUNT_LEVEL_USER_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.ORGANIZATION_VIEWER_ROLE;
import static io.harness.NGConstants.PROJECT_VIEWER_ROLE;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.ALL_RESOURCES_INCLUDING_CHILD_SCOPES_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.authorization.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Sets;
import com.google.inject.name.Named;
import io.harness.NGConstants;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.HarnessScopeLevel;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.account.utils.AccountUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.harness.utils.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class UserRoleAssignmentRemovalJob implements Runnable {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final AccountUtils accountUtils;
  private final ScopeService scopeService;
  private final PersistentLocker persistentLocker;
  private final String DEBUG_MESSAGE = "UserRoleAssignmentRemovalJob: ";
  private static final String LOCK_NAME = "UserRoleAssignmentRemovalJobLock";
  private final boolean skipManagedUserViewerRoleAssignmentsDeletion;
  private final RoleAssignmentService roleAssignmentService;

  @Inject
  public UserRoleAssignmentRemovalJob(RoleAssignmentRepository roleAssignmentRepository, AccountUtils accountUtils, ScopeService scopeService,
                                      PersistentLocker persistentLocker, RoleAssignmentService roleAssignmentService,
                                      @Named("skipManagedUserViewerRoleAssignmentsDeletion") boolean skipManagedUserViewerRoleAssignmentsDeletion) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.accountUtils = accountUtils;
    this.scopeService = scopeService;
    this.persistentLocker = persistentLocker;
    this.roleAssignmentService = roleAssignmentService;
    this.skipManagedUserViewerRoleAssignmentsDeletion = skipManagedUserViewerRoleAssignmentsDeletion;
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
        log.error(DEBUG_MESSAGE + " unexpected error occurred", ex);
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
    if (skipManagedUserViewerRoleAssignmentsDeletion) {
      return;
    }
    List<String> targetAccounts = accountUtils.getAllAccountIds();

    if (isEmpty(targetAccounts)) {
      return;
    }
    try {
      HashSet<String> filteredAccountIds =
          new HashSet<>(filterAccountsWhereDefaultUserGroupHaveViewerRoleAssignment(targetAccounts));
      HashSet<String> accounts =  Sets.newHashSet(targetAccounts);
      Set<String> accountsToAddViewerRoleAssignment = Sets.difference(accounts, filteredAccountIds);
      for (String accountId : targetAccounts) {
        boolean processDeletion = true;
        if (accountsToAddViewerRoleAssignment.contains(accountId)) {
          processDeletion = assignAccountViewerRoleAssignment(accountId);
        }
        if (processDeletion) {
          deleteAccountScopeRoleAssignments(accountId);
        }
        deleteOrganizationScopeRoleAssignments(accountId);
        deleteProjectScopeRoleAssignments(accountId);
        Thread.sleep(1000);
      }
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + "Failed to filter accounts for ff", ex);
    }
  }

  private void deleteAccountScopeRoleAssignments(String accountId) {
    try {
      String scopeIdentifier = "/ACCOUNT/" + accountId;
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier)
                              .is(scopeIdentifier)
                              .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                              .is(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                              .and(RoleAssignmentDBOKeys.roleIdentifier)
                              .in(NGConstants.ACCOUNT_BASIC_ROLE, ACCOUNT_VIEWER_ROLE)
                              .and(RoleAssignmentDBOKeys.principalType)
                              .is(USER)
                              .and(RoleAssignmentDBOKeys.scopeLevel)
                              .is(HarnessScopeLevel.ACCOUNT.getName())
                              .and(RoleAssignmentDBOKeys.managed)
                              .is(true);

      long count = roleAssignmentRepository.deleteMulti(criteria);
      log.info(DEBUG_MESSAGE
          + String.format("removed Account scope %s Role Assignments for accounts: %s", count, accountId));
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + String.format("Failed to delete Role assignments for accounts %s", accountId));
    }
  }

  private void deleteOrganizationScopeRoleAssignments(String accountId) {
    try {
      Pattern startsWithScope = Pattern.compile("^".concat("/ACCOUNT/" + accountId));
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier)
                              .regex(startsWithScope)
                              .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                              .is(DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                              .and(RoleAssignmentDBOKeys.roleIdentifier)
                              .is(ORGANIZATION_VIEWER_ROLE)
                              .and(RoleAssignmentDBOKeys.principalType)
                              .is(USER)
                              .and(RoleAssignmentDBOKeys.scopeLevel)
                              .is(HarnessScopeLevel.ORGANIZATION.getName())
                              .and(RoleAssignmentDBOKeys.managed)
                              .is(true);

      long count = roleAssignmentRepository.deleteMulti(criteria);
      log.info(DEBUG_MESSAGE
          + String.format("removed Organization scope %s Role Assignment in account %s", count, accountId));
    } catch (Exception ex) {
      log.error(
          DEBUG_MESSAGE + String.format("Failed to delete role assignments for organization of account %s", accountId));
    }
  }

  private void deleteProjectScopeRoleAssignments(String accountId) {
    try {
      Pattern startsWithScope = Pattern.compile("^".concat("/ACCOUNT/" + accountId));
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier)
                              .regex(startsWithScope)
                              .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                              .is(DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                              .and(RoleAssignmentDBOKeys.roleIdentifier)
                              .is(PROJECT_VIEWER_ROLE)
                              .and(RoleAssignmentDBOKeys.principalType)
                              .is(USER)
                              .and(RoleAssignmentDBOKeys.scopeLevel)
                              .is(HarnessScopeLevel.PROJECT.getName())
                              .and(RoleAssignmentDBOKeys.managed)
                              .is(true);

      long count = roleAssignmentRepository.deleteMulti(criteria);
      log.info(
          DEBUG_MESSAGE + String.format("Removed Project scope %s role assignments in account %s", count, accountId));
    } catch (Exception ex) {
      log.error(
          DEBUG_MESSAGE + String.format("Failed to delete role assignments for project of account %s", accountId));
    }
  }

  private List<String> filterAccountsWhereDefaultUserGroupHaveViewerRoleAssignment(List<String> accountIds) {
    return Streams.stream(Iterables.partition(accountIds, 100))
        .flatMap(list -> filterAccountsPaginated(list).stream())
        .collect(Collectors.toList());
  }

  private List<String> filterAccountsPaginated(List<String> accountIds) {
    List<String> filteredAccounts = new ArrayList<>();
    List<String> scopeIdentifiers = new ArrayList<>();
    for (String accountId : accountIds) {
      String scopeIdentifier = "/ACCOUNT/" + accountId;
      scopeIdentifiers.add(scopeIdentifier);
    }
    try {
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.principalIdentifier)
                              .is(DEFAULT_ACCOUNT_LEVEL_USER_GROUP_IDENTIFIER)
                              .and(RoleAssignmentDBOKeys.principalType)
                              .is(USER_GROUP)
                              .and(RoleAssignmentDBOKeys.scopeIdentifier)
                              .in(scopeIdentifiers)
                              .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                              .in(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER,
                                  ALL_RESOURCES_INCLUDING_CHILD_SCOPES_RESOURCE_GROUP_IDENTIFIER)
                              .and(RoleAssignmentDBOKeys.roleIdentifier)
                              .is(ACCOUNT_VIEWER_ROLE)
                              .and(RoleAssignmentDBOKeys.principalScopeLevel)
                              .is(HarnessScopeLevel.ACCOUNT.getName())
                              .and(RoleAssignmentDBOKeys.scopeLevel)
                              .is(HarnessScopeLevel.ACCOUNT.getName());

      Pageable pageable = Pageable.unpaged();
      List<RoleAssignmentDBO> roleAssignmentDBOList = roleAssignmentRepository.findAll(criteria, pageable).getContent();

      for (RoleAssignmentDBO roleAssignmentDBO : roleAssignmentDBOList) {
        Scope accountScope = scopeService.buildScopeFromScopeIdentifier(roleAssignmentDBO.getScopeIdentifier());
        filteredAccounts.add(accountScope.getInstanceId());
      }
      log.info(DEBUG_MESSAGE + String.format("Account Ids for which to remove role assignments %s", filteredAccounts));
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE
          + String.format("Failed to query role assignments of default user group for accounts %s", accountIds));
    }
    return filteredAccounts;
  }

  private boolean assignAccountViewerRoleAssignment(String accountId) {
    // If role assignment doesn't exist on Default User Group at account then we need create.
    try {
      String scopeIdentifier = "/ACCOUNT/" + accountId;
      RoleAssignment roleAssignment = buildRolAssignment(scopeIdentifier);
      roleAssignmentService.create(roleAssignment);
      return true;
    }
    catch (Exception ex) {
      log.error(DEBUG_MESSAGE + String.format("Failed to create role assignments for account %s", accountId));
    }
    return false;
  }

  private RoleAssignment buildRolAssignment(String scopeIdentifier) {
    return RoleAssignment.builder()
            .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
            .principalIdentifier(DEFAULT_ACCOUNT_LEVEL_USER_GROUP_IDENTIFIER)
            .principalType(USER_GROUP)
            .roleIdentifier(ACCOUNT_VIEWER_ROLE)
            .resourceGroupIdentifier(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
            .scopeIdentifier(scopeIdentifier)
            .scopeLevel(HarnessScopeLevel.ACCOUNT.toString())
            .principalScopeLevel(HarnessScopeLevel.ACCOUNT.toString())
            .managed(false)
            .internal(false)
            .disabled(false)
            .build();
  }
}