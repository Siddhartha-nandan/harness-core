/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.user.service.impl;

import static io.harness.NGConstants.ACCOUNT_ADMIN_ROLE;
import static io.harness.NGConstants.PROJECT_ADMIN_ROLE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.invites.mapper.RoleBindingMapper.createRoleAssignmentDTOs;
import static io.harness.ng.core.user.UserMembershipUpdateSource.USER;
import static io.harness.ng.core.user.service.impl.NgUserServiceImpl.DEFAULT_PAGE_SIZE;
import static io.harness.rule.OwnerRule.JIMIT_GANDHI;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.NISHANT;
import static io.harness.rule.OwnerRule.REETIKA;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployVariant;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.AccountOrgProjectHelper;
import io.harness.ng.core.api.DefaultUserGroupService;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.invites.dto.RoleBinding;
import io.harness.ng.core.user.AddUserResponse;
import io.harness.ng.core.user.AddUsersDTO;
import io.harness.ng.core.user.AddUsersResponse;
import io.harness.ng.core.user.NGRemoveUserFilter;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.entities.UserMetadata.UserMetadataKeys;
import io.harness.ng.core.user.exception.InvalidUserRemoveRequestException;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.LastAdminCheckService;
import io.harness.notification.Team;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.user.spring.UserMembershipRepository;
import io.harness.repositories.user.spring.UserMetadataRepository;
import io.harness.rule.Owner;
import io.harness.user.remote.UserClient;
import io.harness.utils.NGFeatureFlagHelperService;
import io.harness.utils.PageTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PL)
public class NgUserServiceImplTest extends CategoryTest {
  @Mock private UserClient userClient;
  @Mock private UserMembershipRepository userMembershipRepository;
  @Mock private AccessControlAdminClient accessControlAdminClient;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private OutboxService outboxService;
  @Mock private UserGroupService userGroupService;
  @Mock private UserMetadataRepository userMetadataRepository;
  @Mock private NotificationClient notificationClient;
  @Mock private AccountOrgProjectHelper accountOrgProjectHelper;
  @Mock private LicenseService licenseService;
  @Mock private LastAdminCheckService lastAdminCheckService;
  @Mock private NGFeatureFlagHelperService ngFeatureFlagHelperService;
  @Mock private DefaultUserGroupService defaultUserGroupService;
  @Mock private AccessControlClient accessControlClient;
  @Spy @Inject @InjectMocks private NgUserServiceImpl ngUserService;
  @Mock Call call;
  private String accountIdentifier;
  private String orgIdentifier;
  private static final String ACCOUNT_VIEWER = "_account_viewer";
  private static final String ORGANIZATION_VIEWER = "_organization_viewer";
  private static final String PROJECT_VIEWER = "_project_viewer";

  @Before
  public void setup() throws NoSuchFieldException {
    initMocks(this);
    accountIdentifier = randomAlphabetic(10);
    orgIdentifier = randomAlphabetic(10);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void listUsers() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).build();
    String userId = randomAlphabetic(10);
    List<String> userIds = singletonList(userId);
    List<UserMetadata> userMetadata = singletonList(UserMetadata.builder().userId(userId).build());

    final ArgumentCaptor<Criteria> userMembershipCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    final ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
    when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));

    ngUserService.listUsers(scope, pageRequest, null);

    verify(userMembershipRepository, times(1)).findAllUserIds(userMembershipCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAll(userMetadataCriteriaArgumentCaptor.capture(), any());

    Criteria userMembershipCriteria = userMembershipCriteriaArgumentCaptor.getValue();
    assertNotNull(userMembershipCriteria);
    String userMembershipCriteriaAccount =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY);
    String userMembershipCriteriaOrg =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ORG_IDENTIFIER_KEY);
    String userMembershipCriteriaProject =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY);
    assertEquals(accountIdentifier, userMembershipCriteriaAccount);
    assertNull(userMembershipCriteriaOrg);
    assertNull(userMembershipCriteriaProject);
    assertEquals(3, userMembershipCriteria.getCriteriaObject().size());

    assertUserMetadataCriteria(userMetadataCriteriaArgumentCaptor, userId);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void listUserWithEmailFilter() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).build();
    List<String> userIds = Arrays.asList("ug1", "ug2", "ug3");
    List<UserMetadata> userMetadata = List.of(UserMetadata.builder().userId("ug1").build(),
        UserMetadata.builder().userId("ug2").build(), UserMetadata.builder().userId("ug3").build());

    final ArgumentCaptor<Criteria> userMembershipCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    final ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
    when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));
    when(userMetadataRepository.findAllIds(any())).thenReturn(List.of("ug3"));

    UserFilter userFilter = UserFilter.builder()
                                .emails(Sets.newHashSet("ug3@harness.io"))
                                .identifiers(Sets.newHashSet("ug1", "ug2"))
                                .build();
    ngUserService.listUsers(scope, pageRequest, userFilter);

    verify(userMembershipRepository, times(1)).findAllUserIds(userMembershipCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAll(userMetadataCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAllIds(userMetadataCriteriaArgumentCaptor.capture());

    Criteria userMembershipCriteria = userMembershipCriteriaArgumentCaptor.getValue();
    assertNotNull(userMembershipCriteria);
    String userMembershipCriteriaAccount =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY);
    String userMembershipCriteriaOrg =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.ORG_IDENTIFIER_KEY);
    String userMembershipCriteriaProject =
        (String) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY);
    assertEquals(accountIdentifier, userMembershipCriteriaAccount);
    assertNull(userMembershipCriteriaOrg);
    assertNull(userMembershipCriteriaProject);
    assertEquals(4, userMembershipCriteria.getCriteriaObject().size());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void listUsersStrictlyParentScopes() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).build();
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    String userId = randomAlphabetic(10);
    List<String> userIds = singletonList(userId);
    List<UserMetadata> userMetadata = singletonList(UserMetadata.builder().userId(userId).build());

    final ArgumentCaptor<Criteria> userMembershipCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    final ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
    when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));
    doReturn(Collections.emptyList()).when(ngUserService).listUserIds(scope);

    ngUserService.listUsers(
        scope, pageRequest, UserFilter.builder().parentFilter(UserFilter.ParentFilter.STRICTLY_PARENT_SCOPES).build());

    verify(userMembershipRepository, times(1)).findAllUserIds(userMembershipCriteriaArgumentCaptor.capture(), any());
    verify(userMetadataRepository, times(1)).findAll(userMetadataCriteriaArgumentCaptor.capture(), any());

    Criteria userMembershipCriteria = userMembershipCriteriaArgumentCaptor.getValue();
    assertNotNull(userMembershipCriteria);
    assertEquals(2, userMembershipCriteria.getCriteriaObject().size());

    BasicDBList orList = (BasicDBList) userMembershipCriteria.getCriteriaObject().get("$or");
    assertEquals(2, orList.size());
    assertEquals(accountIdentifier, (String) ((Document) orList.get(0)).get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(0)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(0)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));
    assertEquals(accountIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ACCOUNT_IDENTIFIER_KEY));
    assertEquals(orgIdentifier, (String) ((Document) orList.get(1)).get(UserMembershipKeys.ORG_IDENTIFIER_KEY));
    assertNull(((Document) orList.get(1)).get(UserMembershipKeys.PROJECT_IDENTIFIER_KEY));

    Document userIdMembershipDocument =
        (Document) userMembershipCriteria.getCriteriaObject().get(UserMembershipKeys.userId);
    assertNotNull(userIdMembershipDocument);
    assertEquals(1, userIdMembershipDocument.size());
    List<?> negativeList = (List<?>) userIdMembershipDocument.get("$nin");
    assertEquals(0, negativeList.size());

    assertUserMetadataCriteria(userMetadataCriteriaArgumentCaptor, userId);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRemoveUserFromAccountScope() {
    String accountIdentifier = randomAlphabetic(10);
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).build();
    String userId = randomAlphabetic(10);
    UserMembership userMembership = UserMembership.builder().scope(scope).userId(userId).build();

    preLastAdminFailure(userId, scope, userMembership);
    try {
      ngUserService.removeUserFromScope(userId, scope, USER, NGRemoveUserFilter.ACCOUNT_LAST_ADMIN_CHECK);
      fail();
    } catch (InvalidUserRemoveRequestException exception) {
      // ignore
    }

    try {
      ngUserService.removeUserFromScope(userId, scope, USER, null);
      fail();
    } catch (InvalidUserRemoveRequestException exception) {
      // ignore
    }

    assertSuccessfulRemoveUserFromScope(userId, scope, userMembership);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testRemoveUserFromOrgScope() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    Scope scope = Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    String userId = randomAlphabetic(10);
    UserMembership userMembership = UserMembership.builder().scope(scope).userId(userId).build();

    preLastAdminFailure(userId, scope, userMembership);

    assertSuccessfulRemoveUserFromScope(userId, scope, userMembership);
  }

  private void preLastAdminFailure(String userId, Scope scope, UserMembership userMembership) {
    when(userMembershipRepository.findOne(any())).thenReturn(userMembership);
    when(userMembershipRepository.stream(any(Criteria.class)))
        .thenReturn(createStream(List.of(userMembership).iterator()));
    when(lastAdminCheckService.doesAdminExistAfterRemoval(any(), any())).thenReturn(false);
  }

  private void assertSuccessfulRemoveUserFromScope(String userId, Scope scope, UserMembership userMembership) {
    when(userMembershipRepository.findOne(any())).thenReturn(userMembership);
    when(userMembershipRepository.stream(any(Criteria.class)))
        .thenReturn(createStream(List.of(userMembership).iterator()));
    when(lastAdminCheckService.doesAdminExistAfterRemoval(any(), any())).thenReturn(true);
    when(userMetadataRepository.findDistinctByUserId(userId))
        .thenReturn(Optional.of(UserMetadata.builder().userId(userId).build()));
    when(transactionTemplate.execute(any())).thenReturn(null);
    ngUserService.removeUserFromScope(userId, scope, USER, null);
    verify(transactionTemplate, times(1)).execute(any());
  }

  private void assertUserMetadataCriteria(ArgumentCaptor<Criteria> userMetadataCriteriaArgumentCaptor, String userId) {
    Criteria userMetadataCriteria = userMetadataCriteriaArgumentCaptor.getValue();
    assertNotNull(userMetadataCriteria);
    Document userMetadataCriteriaUserId =
        (Document) userMetadataCriteria.getCriteriaObject().get(UserMetadataKeys.userId);
    assertEquals(1, userMetadataCriteriaUserId.size());
    List<?> userMetadataCriteriaUserIds = (List<?>) userMetadataCriteriaUserId.get("$in");
    assertEquals(1, userMetadataCriteriaUserIds.size());
    assertEquals(userId, userMetadataCriteriaUserIds.get(0));
  }

  private AddUsersDTO setupAddUsersDTO(List<String> emails) {
    String userGroupIdentifier = randomAlphabetic(10);
    String roleIdentifier = randomAlphabetic(5) + '_' + randomAlphabetic(5) + '_' + randomAlphabetic(5);
    String resourceGroupIdentifier = randomAlphabetic(10);
    RoleBinding roleBinding =
        RoleBinding.builder().roleIdentifier(roleIdentifier).resourceGroupIdentifier(resourceGroupIdentifier).build();

    return AddUsersDTO.builder()
        .emails(emails)
        .userGroups(singletonList(userGroupIdentifier))
        .roleBindings(singletonList(roleBinding))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersAddUserAlreadyPartOfAccount() {
    String orgName = randomAlphabetic(10);
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);

    String emailAlreadyPartOfAccount = randomAlphabetic(10) + '@' + randomAlphabetic(10);
    String userIdAlreadyPartOfAccount = randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(emailAlreadyPartOfAccount);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    UserMetadata userMetadataAlreadyPartOfAccount =
        UserMetadata.builder().userId(userIdAlreadyPartOfAccount).email(emailAlreadyPartOfAccount).build();
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(userMetadataAlreadyPartOfAccount), 1));

    doReturn(Sets.newHashSet(userIdAlreadyPartOfAccount))
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(userIdAlreadyPartOfAccount), Scope.of(accountIdentifier, null, null));

    preDirectlyAddUsers(scope, orgName, Lists.newArrayList(userIdAlreadyPartOfAccount), addUsersDTO);

    ArgumentCaptor<NotificationChannel> notificationChannelArgumentCaptor =
        ArgumentCaptor.forClass(NotificationChannel.class);
    when(notificationClient.sendNotificationAsync(any())).thenReturn(null);

    AddUsersResponse response = ngUserService.addUsers(scope, addUsersDTO);
    assertEquals(1, response.getAddUserResponseMap().size());
    assertEquals(
        AddUserResponse.USER_ADDED_SUCCESSFULLY, response.getAddUserResponseMap().get(emailAlreadyPartOfAccount));

    verify(userGroupService, times(1)).list(any(UserGroupFilterDTO.class));
    verify(userMetadataRepository, times(1)).findAll(any(), any());
    verify(ngUserService, times(1)).getUsersAtScope(any(), any());

    assertDirectlyAddUsers(scope, Lists.newArrayList(emailAlreadyPartOfAccount), addUsersDTO.getUserGroups());
    assertDirectlyAddedNotification(
        notificationChannelArgumentCaptor, Lists.newArrayList(emailAlreadyPartOfAccount), "organizationname", orgName);
  }

  private void preDirectlyAddUsers(
      Scope scope, String resourceName, List<String> toBeDirectlyAddedUserIds, AddUsersDTO addUsersDTO) {
    if (isEmpty(toBeDirectlyAddedUserIds)) {
      return;
    }
    when(accountOrgProjectHelper.getResourceScopeName(scope)).thenReturn(resourceName);
    when(accountOrgProjectHelper.getBaseUrl(accountIdentifier)).thenReturn("qa.harness.io");

    toBeDirectlyAddedUserIds.forEach(userIdAlreadyPartOfAccount -> {
      doReturn(false).when(ngUserService).isUserAtScope(userIdAlreadyPartOfAccount, scope);
      preAddUserToScope(userIdAlreadyPartOfAccount, scope, addUsersDTO.getRoleBindings(), addUsersDTO.getUserGroups());
    });
  }

  private void preAddUserToScope(String userId, Scope scope, List<RoleBinding> roleBindings, List<String> userGroups) {
    List<Scope> parentScopes = new ArrayList<>();
    if (isNotEmpty(scope.getProjectIdentifier())) {
      parentScopes.add(Scope.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), null));
    }
    if (isNotEmpty(scope.getOrgIdentifier())) {
      parentScopes.add(Scope.of(scope.getAccountIdentifier(), null, null));
    }
    when(userMetadataRepository.findDistinctByUserId(userId))
        .thenReturn(Optional.of(UserMetadata.builder().userId(userId).build()));
    boolean isAccountBasicRoleFeatureFlag = false;
    when(ngFeatureFlagHelperService.isEnabled(
             scope.getAccountIdentifier(), io.harness.beans.FeatureName.ACCOUNT_BASIC_ROLE))
        .thenReturn(isAccountBasicRoleFeatureFlag);
    doNothing()
        .when(ngUserService)
        .addUserToScopeInternal(userId, UserMembershipUpdateSource.USER, scope, getDefaultRoleIdentifier(scope),
            isAccountBasicRoleFeatureFlag);

    parentScopes.forEach(parentScope
        -> doNothing()
               .when(ngUserService)
               .addUserToScopeInternal(userId, UserMembershipUpdateSource.USER, parentScope,
                   getDefaultRoleIdentifier(parentScope), isAccountBasicRoleFeatureFlag));
    doNothing()
        .when(ngUserService)
        .createRoleAssignments(
            userId, scope, createRoleAssignmentDTOs(roleBindings, userId, scope), isAccountBasicRoleFeatureFlag);

    UserGroupFilterDTO userGroupFilterDTO =
        UserGroupFilterDTO.builder()
            .accountIdentifier(scope.getAccountIdentifier())
            .orgIdentifier(scope.getOrgIdentifier())
            .projectIdentifier(scope.getProjectIdentifier())
            .identifierFilter(new HashSet<>(isEmpty(userGroups) ? new ArrayList<>() : userGroups))
            .build();

    List<UserGroup> userGroupsResult = isEmpty(userGroups)
        ? new ArrayList<>()
        : userGroups.stream()
              .map(userGroupIdentifier -> UserGroup.builder().identifier(userGroupIdentifier).build())
              .collect(toList());
    when(userGroupService.list(userGroupFilterDTO)).thenReturn(userGroupsResult);

    doNothing().when(userGroupService).addUserToUserGroups(scope, userId, userGroups);
  }

  private String getDefaultRoleIdentifier(Scope scope) {
    if (isNotEmpty(scope.getProjectIdentifier())) {
      return PROJECT_VIEWER;
    } else if (isNotEmpty(scope.getOrgIdentifier())) {
      return ORGANIZATION_VIEWER;
    }
    return ACCOUNT_VIEWER;
  }

  private int getRank(Scope scope) {
    if (isNotEmpty(scope.getProjectIdentifier())) {
      return 3;
    } else if (isNotEmpty(scope.getOrgIdentifier())) {
      return 2;
    }
    return 1;
  }

  private void assertDirectlyAddUsers(Scope scope, List<String> toBeDirectlyAddedUserIds, List<String> userGroups) {
    verify(accountOrgProjectHelper, times(1)).getResourceScopeName(any());
    verify(accountOrgProjectHelper, times(1)).getBaseUrl(any());
    verify(ngUserService, times(toBeDirectlyAddedUserIds.size())).isUserAtScope(any(), any());
    assertAddUserToScope(scope, toBeDirectlyAddedUserIds, userGroups);
  }

  private void assertAddUserToScope(Scope scope, List<String> userIds, List<String> userGroups) {
    verify(userMetadataRepository, times(userIds.size())).findDistinctByUserId(any());
    verify(ngUserService, times(userIds.size() * getRank(scope)))
        .addUserToScopeInternal(any(), any(), any(), any(), anyBoolean());
    verify(ngUserService, times(userIds.size() * 2)).createRoleAssignments(any(), any(), any(), anyBoolean());
    verify(userGroupService, times(isEmpty(userGroups) ? 0 : userIds.size())).list(any(UserGroupFilterDTO.class));
    verify(userGroupService, times(userIds.size())).addUserToUserGroups(any(Scope.class), any(), any());
  }

  private void assertDirectlyAddedNotification(ArgumentCaptor<NotificationChannel> notificationChannelArgumentCaptor,
      List<String> emails, String resourceKey, String resourceName) {
    verify(notificationClient, times(1)).sendNotificationAsync(notificationChannelArgumentCaptor.capture());
    NotificationChannel notificationChannel = notificationChannelArgumentCaptor.getValue();
    assertNotNull(notificationChannel);
    assertTrue(notificationChannel instanceof EmailChannel);
    assertEquals(emails, ((EmailChannel) notificationChannel).getRecipients());
    assertEquals(accountIdentifier, notificationChannel.getAccountId());
    assertEquals("email_notify", notificationChannel.getTemplateId());
    assertEquals(Team.PL, notificationChannel.getTeam());
    assertTrue(notificationChannel.getTemplateData().containsKey(resourceKey));
    assertEquals(resourceName, notificationChannel.getTemplateData().get(resourceKey));
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersInviteHarnessUser() {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String emailAlreadyPartOfHarness = randomAlphabetic(10) + '@' + randomAlphabetic(10);
    String userIdAlreadyPartOfHarness = randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(emailAlreadyPartOfHarness);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    UserMetadata userMetadataAlreadyPartOfHarness =
        UserMetadata.builder().userId(userIdAlreadyPartOfHarness).email(emailAlreadyPartOfHarness).build();
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(userMetadataAlreadyPartOfHarness), 1));

    doReturn(Sets.newHashSet())
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(userIdAlreadyPartOfHarness), Scope.of(accountIdentifier, null, null));

    assertInviteUser(scope, singletonList(emailAlreadyPartOfHarness), addUsersDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersInviteNewEmail() {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String newEmail = randomAlphabetic(10) + '@' + randomAlphabetic(10);

    List<String> emails = Lists.newArrayList(newEmail);
    AddUsersDTO addUsersDTO = setupAddUsersDTO(emails);

    Criteria criteria = Criteria.where(UserMetadataKeys.email).in(addUsersDTO.getEmails());
    when(userMetadataRepository.findAll(criteria, Pageable.unpaged()))
        .thenReturn(PageTestUtils.getPage(Lists.newArrayList(), 0));

    doReturn(Sets.newHashSet())
        .when(ngUserService)
        .getUsersAtScope(Sets.newHashSet(), Scope.of(accountIdentifier, null, null));

    assertInviteUser(scope, singletonList(newEmail), addUsersDTO);
  }

  @Test
  @Owner(developers = JIMIT_GANDHI)
  @Category(UnitTests.class)
  public void testWaitForRbacSetup() {
    doReturn(true).when(accessControlClient).hasAccess(any(), any(), any(), anyString());
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String email = randomAlphabetic(10) + '@' + randomAlphabetic(10);
    String userId = randomAlphabetic(10);
    ngUserService.waitForRbacSetup(scope, userId, email);
    verify(accessControlClient, times(1)).hasAccess(any(), any(), any(), anyString());
  }

  @Test
  @Owner(developers = JIMIT_GANDHI)
  @Category(UnitTests.class)
  public void busyPollUntilAccountRBACSetupCompletes_VerifyMaxRetries() {
    for (int i = 0; i < 3; i++) {
      doReturn(false).when(accessControlClient).hasAccess(any(), any(), any(), anyString());
    }
    Scope scope = Scope.of(accountIdentifier, null, null);
    String email = "testEmail";
    String userId = "testUserId";
    boolean result = ngUserService.busyPollUntilAccountRBACSetupCompletes(scope, userId, 3, 100);
    assertFalse(result);
    verify(accessControlClient, times(3)).hasAccess(any(), any(), any(), anyString());
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testListUsersHavingRole_community() {
    String userId = randomAlphabetic(10);
    List<String> userIds = List.of(userId);
    List<UserMetadata> userMetadata = singletonList(UserMetadata.builder().userId(userId).build());

    try (MockedStatic<DeployVariant> mockedStatic = Mockito.mockStatic(DeployVariant.class)) {
      mockedStatic.when(() -> DeployVariant.isCommunity(any())).thenReturn(true);
      when(userMembershipRepository.findAllUserIds(any(), any())).thenReturn(PageTestUtils.getPage(userIds, 1));
      when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(userMetadata, 1));
      List<UserMetadataDTO> result =
          ngUserService.listUsersHavingRole(Scope.of(accountIdentifier, orgIdentifier, null), PROJECT_ADMIN_ROLE);
      assertThat(result).isNotNull();
      verify(userMembershipRepository, times(1)).findAllUserIds(any(), any());
      verify(userMetadataRepository, times(1)).findAll(any(), any());
      verify(accessControlAdminClient, times(0))
          .getFilteredRoleAssignments(anyString(), any(), any(), anyInt(), anyInt(), any());
    }
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testListUsersHavingRole_non_community() throws IOException {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    try (MockedStatic<DeployVariant> mockedStatic = Mockito.mockStatic(DeployVariant.class)) {
      mockedStatic.when(() -> DeployVariant.isCommunity(any())).thenReturn(false);
      when(call.execute())
          .thenReturn(Response.success(
              ResponseDTO.newResponse(PageResponse.getEmptyPageResponse(PageRequest.builder().build()))));
      when(accessControlAdminClient.getFilteredRoleAssignments(anyString(), any(), any(), anyInt(), anyInt(), any()))
          .thenReturn(call);
      when(userMetadataRepository.findAll(any(), any())).thenReturn(PageTestUtils.getPage(Lists.newArrayList(), 0));
      List<UserMetadataDTO> result = ngUserService.listUsersHavingRole(scope, PROJECT_ADMIN_ROLE);
      assertThat(result).isNotNull().isEmpty();
      ArgumentCaptor<RoleAssignmentFilterDTO> argumentCaptor = ArgumentCaptor.forClass(RoleAssignmentFilterDTO.class);
      verify(accessControlAdminClient, times(1))
          .getFilteredRoleAssignments(eq(scope.getAccountIdentifier()), eq(scope.getOrgIdentifier()),
              eq(scope.getProjectIdentifier()), eq(0), eq(DEFAULT_PAGE_SIZE), argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getRoleFilter()).hasSize(1).containsExactly(PROJECT_ADMIN_ROLE);
    }
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testIsAccountAdmin_community() {
    String userId = randomAlphabetic(10);
    UserMembership userMembership = UserMembership.builder().userId(userId).build();
    try (MockedStatic<DeployVariant> mockedStatic = Mockito.mockStatic(DeployVariant.class)) {
      mockedStatic.when(() -> DeployVariant.isCommunity(any())).thenReturn(true);
      when(userMembershipRepository.findOne(any())).thenReturn(userMembership);
      boolean result = ngUserService.isAccountAdmin(userId, accountIdentifier);
      assertThat(result).isTrue();
      verify(accessControlAdminClient, times(0))
          .getFilteredRoleAssignments(anyString(), any(), any(), anyInt(), anyInt(), any());
    }
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testIsAccountAdmin_non_community() throws IOException {
    String userId = randomAlphabetic(10);
    try (MockedStatic<DeployVariant> mockedStatic = Mockito.mockStatic(DeployVariant.class)) {
      mockedStatic.when(() -> DeployVariant.isCommunity(any())).thenReturn(false);
      when(call.execute())
          .thenReturn(Response.success(
              ResponseDTO.newResponse(PageResponse.getEmptyPageResponse(PageRequest.builder().build()))));
      when(accessControlAdminClient.getFilteredRoleAssignments(anyString(), any(), any(), anyInt(), anyInt(), any()))
          .thenReturn(call);
      boolean result = ngUserService.isAccountAdmin(userId, accountIdentifier);
      assertThat(result).isFalse();
      ArgumentCaptor<RoleAssignmentFilterDTO> argumentCaptor = ArgumentCaptor.forClass(RoleAssignmentFilterDTO.class);
      verify(accessControlAdminClient, times(1))
          .getFilteredRoleAssignments(
              eq(accountIdentifier), eq(null), eq(null), eq(0), eq(DEFAULT_PAGE_SIZE), argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getRoleFilter()).hasSize(1).containsExactly(ACCOUNT_ADMIN_ROLE);
    }
  }

  private void assertInviteUser(Scope scope, List<String> emailsExpectedToBeInvited, AddUsersDTO addUsersDTO) {
    Map<String, AddUserResponse> inviteResponse = new HashMap<>();
    emailsExpectedToBeInvited.forEach(email -> inviteResponse.put(email, AddUserResponse.USER_INVITED_SUCCESSFULLY));
    ArgumentCaptor<List> toBeInvitedEmailsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    doReturn(inviteResponse)
        .when(ngUserService)
        .inviteUsers(eq(scope), eq(addUsersDTO.getRoleBindings()), eq(addUsersDTO.getUserGroups()), any());

    AddUsersResponse response = ngUserService.addUsers(scope, addUsersDTO);

    assertEquals(emailsExpectedToBeInvited.size(), response.getAddUserResponseMap().size());
    emailsExpectedToBeInvited.forEach(
        email -> assertEquals(AddUserResponse.USER_INVITED_SUCCESSFULLY, response.getAddUserResponseMap().get(email)));

    verify(userMetadataRepository, times(1)).findAll(any(), any());
    verify(ngUserService, times(1)).getUsersAtScope(any(), any());

    verify(ngUserService, times(1)).inviteUsers(any(), any(), any(), toBeInvitedEmailsArgumentCaptor.capture());
    List<String> toBeInvitedEmails = toBeInvitedEmailsArgumentCaptor.getValue();
    assertEquals(emailsExpectedToBeInvited.size(), toBeInvitedEmails.size());
    Collections.sort(toBeInvitedEmails);
    Collections.sort(emailsExpectedToBeInvited);
    assertEquals(emailsExpectedToBeInvited, toBeInvitedEmails);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testAddUsersNullRoleBindingsAndUserGroups() {
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, null);
    String userId = randomAlphabetic(10);
    preAddUserToScope(userId, scope, new ArrayList<>(), new ArrayList<>());
    ngUserService.addUserToScope(userId, scope, null, null, UserMembershipUpdateSource.USER);
    assertAddUserToScope(scope, singletonList(userId), null);
  }

  private static <T> Stream<T> createStream(Iterator<T> iterator) {
    return new Stream<T>() {
      @NotNull
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }

      @NotNull
      @Override
      public Spliterator<T> spliterator() {
        return null;
      }

      @Override
      public boolean isParallel() {
        return false;
      }

      @NotNull
      @Override
      public Stream<T> sequential() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> parallel() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> unordered() {
        return null;
      }

      @NotNull
      @Override
      public Stream<T> onClose(Runnable closeHandler) {
        return null;
      }

      @Override
      public void close() {}

      @Override
      public Stream<T> filter(Predicate<? super T> predicate) {
        return null;
      }

      @Override
      public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return null;
      }

      @Override
      public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return null;
      }

      @Override
      public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return null;
      }

      @Override
      public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return null;
      }

      @Override
      public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return null;
      }

      @Override
      public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return null;
      }

      @Override
      public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return null;
      }

      @Override
      public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return null;
      }

      @Override
      public Stream<T> distinct() {
        return null;
      }

      @Override
      public Stream<T> sorted() {
        return null;
      }

      @Override
      public Stream<T> sorted(Comparator<? super T> comparator) {
        return null;
      }

      @Override
      public Stream<T> peek(Consumer<? super T> action) {
        return null;
      }

      @Override
      public Stream<T> limit(long maxSize) {
        return null;
      }

      @Override
      public Stream<T> skip(long n) {
        return null;
      }

      @Override
      public void forEach(Consumer<? super T> action) {}

      @Override
      public void forEachOrdered(Consumer<? super T> action) {}

      @NotNull
      @Override
      public Object[] toArray() {
        return new Object[0];
      }

      @NotNull
      @Override
      public <A> A[] toArray(IntFunction<A[]> generator) {
        return null;
      }

      @Override
      public T reduce(T identity, BinaryOperator<T> accumulator) {
        return null;
      }

      @NotNull
      @Override
      public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return Optional.empty();
      }

      @Override
      public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return null;
      }

      @Override
      public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return null;
      }

      @Override
      public <R, A> R collect(Collector<? super T, A, R> collector) {
        return null;
      }

      @NotNull
      @Override
      public Optional<T> min(Comparator<? super T> comparator) {
        return Optional.empty();
      }

      @NotNull
      @Override
      public Optional<T> max(Comparator<? super T> comparator) {
        return Optional.empty();
      }

      @Override
      public long count() {
        return 0;
      }

      @Override
      public boolean anyMatch(Predicate<? super T> predicate) {
        return false;
      }

      @Override
      public boolean allMatch(Predicate<? super T> predicate) {
        return false;
      }

      @Override
      public boolean noneMatch(Predicate<? super T> predicate) {
        return false;
      }

      @NotNull
      @Override
      public Optional<T> findFirst() {
        return Optional.empty();
      }

      @NotNull
      @Override
      public Optional<T> findAny() {
        return Optional.empty();
      }
    };
  }
}
