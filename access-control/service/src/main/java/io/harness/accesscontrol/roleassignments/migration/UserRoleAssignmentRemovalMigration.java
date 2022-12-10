package io.harness.accesscontrol.roleassignments.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.NGConstants;
import io.harness.accesscontrol.commons.helpers.FeatureFlagHelperService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.remote.client.CGRestUtils;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.NGConstants.DEFAULT_ACCOUNT_LEVEL_USER_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_ORGANIZATION_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_PROJECT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.ORGANIZATION_VIEWER_ROLE;
import static io.harness.NGConstants.PROJECT_VIEWER_ROLE;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupConstants.DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.beans.FeatureName.ACCOUNT_BASIC_ROLE;
import static io.harness.beans.FeatureName.ACCOUNT_BASIC_ROLE_ONLY;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class UserRoleAssignmentRemovalMigration implements NGMigration {
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final FeatureFlagHelperService featureFlagHelperService;
  private final AccountClient accountClient;
  private static final String DEBUG_MESSAGE = "UserRoleAssignmentRemovalMigration: ";

  @Inject
  public UserRoleAssignmentRemovalMigration(RoleAssignmentRepository roleAssignmentRepository,
                                            FeatureFlagHelperService featureFlagHelperService, AccountClient accountClient) {
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.featureFlagHelperService = featureFlagHelperService;
    this.accountClient = accountClient;
  }

  @Override
  public void migrate() {
    log.info(DEBUG_MESSAGE + "started...");

    try {
      SecurityContextBuilder.setContext(new ServicePrincipal(ACCESS_CONTROL_SERVICE.getServiceId()));
      doMigration();
      log.info(DEBUG_MESSAGE + "Setting SecurityContext completed.");
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + " unexpected error occurred while Setting SecurityContext", ex);
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
      log.info(DEBUG_MESSAGE + "Unsetting SecurityContext completed.");
    }
    log.info(DEBUG_MESSAGE + " completed...");
  }

  private void doMigration() {
    List<AccountDTO> accountDTOS = new ArrayList<>();
    try {
      accountDTOS = CGRestUtils.getResponse(accountClient.getAllAccounts());
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + "Failed to fetch all accounts");
    }
    List<AccountDTO> ngEnabledAccounts =
            accountDTOS.stream().filter(AccountDTO::isNextGenEnabled).collect(Collectors.toList());
    log.info(DEBUG_MESSAGE + String.format("%s accounts fetch", ngEnabledAccounts.size()));
    HashSet<String> targetAccounts = new HashSet<>();
    HashSet<String> targetAccountsWithOrganizationAndProject = new HashSet<>();
    for (AccountDTO accountDTO : ngEnabledAccounts) {
      boolean isAccountBasicRoleEnabled = featureFlagHelperService.isEnabled(ACCOUNT_BASIC_ROLE, accountDTO.getIdentifier());
      boolean isAccountBasicRoleOnlyEnabled = featureFlagHelperService.isEnabled(ACCOUNT_BASIC_ROLE_ONLY, accountDTO.getIdentifier());
      if (!(isAccountBasicRoleEnabled || isAccountBasicRoleOnlyEnabled)) {
        targetAccounts.add(accountDTO.getIdentifier());
      }
      targetAccountsWithOrganizationAndProject.add(accountDTO.getIdentifier());
    }
    filterAccounts(targetAccounts);
    if (isNotEmpty(targetAccounts)) {
      deleteAccountScopeRoleAssignments(targetAccounts);
    }
    if (isNotEmpty(targetAccountsWithOrganizationAndProject)) {
      deleteOrganizationScopeRoleAssignments(targetAccountsWithOrganizationAndProject);
      deleteProjectScopeRoleAssignments(targetAccountsWithOrganizationAndProject);
    }
  }

  private void deleteAccountScopeRoleAssignments(HashSet<String> accountIds) {
    try {
      List<String> scopeIdentifiers =
              accountIds.stream().map(accId -> "/ACCOUNT/" + accId).collect(Collectors.toList());
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier)
              .in(scopeIdentifiers)
              .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
              .is(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
              .and(RoleAssignmentDBOKeys.roleIdentifier)
              .in(NGConstants.ACCOUNT_BASIC_ROLE, NGConstants.ACCOUNT_VIEWER_ROLE)
              .and(RoleAssignmentDBOKeys.scopeLevel)
              .is(HarnessScopeLevel.ACCOUNT.getName())
              .and(RoleAssignmentDBOKeys.principalType)
              .is(USER);

      long count = roleAssignmentRepository.deleteMulti(criteria);
      log.info(DEBUG_MESSAGE + String.format("removed Account scope %s Role Assignments", count));
    } catch (Exception ex) {
      log.error(DEBUG_MESSAGE + "Failed to delete Role assignments for accounts");
    }
  }

  private void deleteOrganizationScopeRoleAssignments(HashSet<String> accountIds) {
    for (String accountId : accountIds) {
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
        log.error(DEBUG_MESSAGE
                + String.format("Failed to delete role assignments for organization of account %s", accountId));
      }
    }
  }

  private void deleteProjectScopeRoleAssignments(HashSet<String> accountIds) {
    for (String accountId : accountIds) {
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
  }

  private void filterAccounts(HashSet<String> accountIds) {
    for (String accountId : accountIds) {
      try {
        String scopeIdentifier = "/ACCOUNT/" + accountId;
        Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier)
                .is(scopeIdentifier)
                .and(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                .is(DEFAULT_ACCOUNT_LEVEL_RESOURCE_GROUP_IDENTIFIER)
                .and(RoleAssignmentDBOKeys.roleIdentifier)
                .in(NGConstants.ACCOUNT_VIEWER_ROLE)
                .and(RoleAssignmentDBOKeys.principalIdentifier)
                .is(DEFAULT_ACCOUNT_LEVEL_USER_GROUP_IDENTIFIER)
                .and(RoleAssignmentDBOKeys.principalScopeLevel)
                .is(HarnessScopeLevel.ACCOUNT.getName())
                .and(RoleAssignmentDBOKeys.principalType)
                .is(USER_GROUP)
                .and(RoleAssignmentDBOKeys.scopeLevel)
                .is(HarnessScopeLevel.ACCOUNT.getName());

        Pageable pageable = Pageable.unpaged();
        Page<RoleAssignmentDBO> roleAssignmentDBOPage = roleAssignmentRepository.findAll(criteria, pageable);
        //If role assignment doesn't exist on Default User Group at account then skip removing User assigned role assignment.
        if (roleAssignmentDBOPage == null || roleAssignmentDBOPage.isEmpty()) {
          accountIds.remove(accountId);
        }
      }
      catch (Exception ex)
      {
        log.error(DEBUG_MESSAGE + String.format("Failed to query role assignments of default user group for account %s", accountId));
      }
    }
  }
}


