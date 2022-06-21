package io.harness.delegate.task.ldap;

import static io.harness.rule.OwnerRule.PRATEEK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ldap.LdapGroupSearchTaskParameters;
import io.harness.delegate.beans.ldap.LdapGroupSearchTaskResponse;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.dto.LdapSettings;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.service.intfc.ldap.LdapDelegateService;

import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PL)
public class NGLdapGroupSearchTaskTest extends CategoryTest {
  @Mock EncryptedDataDetail encryptedDataDetail;
  @Mock LdapSettings ldapSettings;
  LdapDelegateService ldapDelegateService = mock(LdapDelegateService.class);

  LdapGroupSearchTaskParameters ldapGroupSearchTaskParameters = LdapGroupSearchTaskParameters.builder()
                                                                    .ldapSettings(ldapSettings)
                                                                    .encryptedDataDetail(encryptedDataDetail)
                                                                    .build();

  DelegateTaskPackage delegateTaskPackage = DelegateTaskPackage.builder()
                                                .delegateId(UUIDGenerator.generateUuid())
                                                .accountId(UUIDGenerator.generateUuid())
                                                .data(TaskData.builder()
                                                          .async(false)
                                                          .parameters(new Object[] {ldapGroupSearchTaskParameters})
                                                          .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT)
                                                          .build())
                                                .build();
  @InjectMocks
  NGLdapGroupSearchTask ngLdapGroupSearchTask =
      new NGLdapGroupSearchTask(delegateTaskPackage, null, delegateTaskResponse -> {}, () -> true);

  @Test
  @Owner(developers = PRATEEK)
  @Category(UnitTests.class)
  public void testRunNgLdapGroupSearchTask() throws IllegalAccessException {
    String groupNameQuery = "grpName";

    LdapGroupResponse response = LdapGroupResponse.builder()
                                     .name(groupNameQuery)
                                     .description("desc")
                                     .dn("uid=ldap_user1,ou=Users,dc=jumpcloud,dc=com")
                                     .totalMembers(4)
                                     .build();
    Collection<LdapGroupResponse> matchedGroups = Collections.singletonList(response);

    FieldUtils.writeField(ngLdapGroupSearchTask, "ldapDelegateService", ldapDelegateService, true);
    when(ldapDelegateService.searchGroupsByName(any(), any(), anyString())).thenReturn(matchedGroups);

    LdapGroupSearchTaskResponse taskResponse =
        (LdapGroupSearchTaskResponse) ngLdapGroupSearchTask.run(ldapGroupSearchTaskParameters);
    assertThat(taskResponse).isNotNull();
  }

  @Test
  @Owner(developers = PRATEEK)
  @Category(UnitTests.class)
  public void testRunNgLdapGroupSearchTaskNoMatchingGroups() throws IllegalAccessException {
    FieldUtils.writeField(ngLdapGroupSearchTask, "ldapDelegateService", ldapDelegateService, true);
    when(ldapDelegateService.searchGroupsByName(any(), any(), anyString())).thenReturn(Collections.EMPTY_LIST);

    LdapGroupSearchTaskResponse taskResponse =
        (LdapGroupSearchTaskResponse) ngLdapGroupSearchTask.run(ldapGroupSearchTaskParameters);
    assertThat(taskResponse).isNotNull();
  }
}
