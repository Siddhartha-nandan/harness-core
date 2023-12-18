/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ASHISHSANODIA;
import static io.harness.rule.OwnerRule.BHAVYA;
import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.rule.OwnerRule.NISHANT;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.VITALIE;

import static java.util.List.of;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.NGAccessDeniedException;
import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.accesscontrol.acl.api.AccessControlDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.beans.secrets.WinRmConfigValidationTaskResponse;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;
import io.harness.encryption.SecretRefData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.exception.exceptionmanager.exceptionhandler.DocumentLinksConstants;
import io.harness.ng.core.api.NGSecretActivityService;
import io.harness.ng.core.dto.secrets.SSHCredentialType;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.TGTGenerationMethod;
import io.harness.ng.core.events.SecretDeleteEvent;
import io.harness.ng.core.events.SecretForceDeleteEvent;
import io.harness.ng.core.models.KerberosConfig;
import io.harness.ng.core.models.KerberosWinRmConfig;
import io.harness.ng.core.models.NTLMConfig;
import io.harness.ng.core.models.SSHAuth;
import io.harness.ng.core.models.SSHConfig;
import io.harness.ng.core.models.SSHExecutionCredentialSpec;
import io.harness.ng.core.models.SSHKeyCredential;
import io.harness.ng.core.models.SSHKeyPathCredential;
import io.harness.ng.core.models.SSHPasswordCredential;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.SecretTextSpec;
import io.harness.ng.core.models.TGTKeyTabFilePathSpec;
import io.harness.ng.core.models.TGTPasswordSpec;
import io.harness.ng.core.models.WinRmAuth;
import io.harness.ng.core.models.WinRmCredentialsSpec;
import io.harness.ng.core.remote.SSHKeyValidationMetadata;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.ng.core.remote.WinRmCredentialsValidationMetadata;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.SecretRepository;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.WinRmAuthScheme;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.secretmanagerclient.services.WinRmCredentialsSpecDTOHelper;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.springdata.HTransactionTemplate;
import io.harness.utils.NGFeatureFlagHelperService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class NGSecretServiceV2ImplTest extends CategoryTest {
  private SecretRepository secretRepository;
  private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private NGSecretServiceV2Impl secretServiceV2;
  private NGSecretServiceV2Impl secretServiceV2Spy;
  private NGSecretActivityService ngSecretActivityService;
  private OutboxService outboxService;
  private TaskSetupAbstractionHelper taskSetupAbstractionHelper;
  private TransactionTemplate transactionTemplate;
  private AccessControlClient accessControlClient;
  private NGFeatureFlagHelperService ngFeatureFlagHelperService;
  private ExceptionManager exceptionManager;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  private ArgumentCaptor<SecretForceDeleteEvent> secretForceDeleteEventArgumentCaptor;
  private ArgumentCaptor<SecretDeleteEvent> secretDeleteEventArgumentCaptor;
  private ArgumentCaptor<Exception> exceptionArgumentCaptor;

  private String ORG_ID = randomAlphabetic(10);
  private String PROJECT_ID = randomAlphabetic(10);
  private String ID = randomAlphabetic(10);
  private String ACC_ID = randomAlphabetic(10);
  Secret secret = Secret.builder()
                      .identifier(ID)
                      .accountIdentifier(ACC_ID)
                      .projectIdentifier(PROJECT_ID)
                      .orgIdentifier(ORG_ID)
                      .secretSpec(SecretTextSpec.builder()
                                      .secretManagerIdentifier("secretManager")
                                      .valueType(ValueType.Inline)
                                      .value("value")
                                      .build())

                      .build();
  @Before
  public void setup() {
    secretRepository = mock(SecretRepository.class);
    delegateGrpcClientWrapper = mock(DelegateGrpcClientWrapper.class);
    ngSecretActivityService = mock(NGSecretActivityService.class);
    outboxService = mock(OutboxService.class);
    transactionTemplate = new HTransactionTemplate(new MongoTransactionManager(), false);
    taskSetupAbstractionHelper = new TaskSetupAbstractionHelper();
    accessControlClient = mock(AccessControlClient.class);
    ngFeatureFlagHelperService = mock(NGFeatureFlagHelperService.class);
    exceptionManager = mock(ExceptionManager.class);
    SshKeySpecDTOHelper sshKeySpecDTOHelper = mock(SshKeySpecDTOHelper.class);
    WinRmCredentialsSpecDTOHelper winRmCredentialsSpecDTOHelper = mock(WinRmCredentialsSpecDTOHelper.class);

    secretServiceV2 = new NGSecretServiceV2Impl(secretRepository, delegateGrpcClientWrapper, sshKeySpecDTOHelper,
        ngSecretActivityService, outboxService, transactionTemplate, taskSetupAbstractionHelper,
        winRmCredentialsSpecDTOHelper, accessControlClient, ngFeatureFlagHelperService, exceptionManager);
    secretServiceV2Spy = spy(secretServiceV2);
    secretForceDeleteEventArgumentCaptor = ArgumentCaptor.forClass(SecretForceDeleteEvent.class);
    secretDeleteEventArgumentCaptor = ArgumentCaptor.forClass(SecretDeleteEvent.class);
    exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);
  }

  private SecretDTOV2 getSecretDTO() {
    return SecretDTOV2.builder()
        .name("name")
        .type(SecretType.SecretText)
        .identifier("identifier")
        .tags(Maps.newHashMap(ImmutableMap.of("a", "b")))
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testGet() {
    when(secretRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    Optional<Secret> secretOptional = secretServiceV2.get("account", null, null, "identifier");
    assertThat(secretOptional).isEqualTo(Optional.empty());
    verify(secretRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetWithScopeInfo() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    when(secretRepository.findByAccountIdentifierAndParentUniqueIdAndIdentifier(any(), any(), any()))
        .thenReturn(Optional.empty());
    Optional<Secret> secretOptional = secretServiceV2.get(scopeInfo, "identifier");
    assertThat(secretOptional).isEqualTo(Optional.empty());
    verify(secretRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testGetForIdentifierRef() {
    Secret testsecret = Secret.builder().name("testsecret").build();
    when(secretRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(testsecret));
    Optional<Secret> secretOptional = secretServiceV2.get(IdentifierRef.builder().identifier("test").build());
    assertThat(secretOptional.get()).isEqualTo(testsecret);
    verify(secretRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDelete() {
    ScopeInfo scopeInfo = ScopeInfo.builder()
                              .accountIdentifier("account")
                              .orgIdentifier("org")
                              .projectIdentifier("proj")
                              .uniqueId(randomAlphabetic(10))
                              .scopeType(ScopeLevel.PROJECT)
                              .build();
    Secret secret = Secret.builder().build();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    doNothing().when(secretRepository).delete(any());
    boolean success = secretServiceV2Spy.delete(scopeInfo, "identifier", false);
    assertThat(success).isTrue();
    verify(secretServiceV2Spy).get(eq(scopeInfo), any());
    verify(secretRepository, times(1)).delete(any());
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void deleteInternal_withForceDeleteFalse() {
    doNothing().when(ngSecretActivityService).deleteAllActivities(any(), any());
    doNothing().when(secretRepository).delete(any());
    boolean success = secretServiceV2Spy.deleteInternal("account", "org", "proj", "identifier", secret, false);
    assertThat(success).isTrue();
    verify(outboxService, times(1)).save(secretDeleteEventArgumentCaptor.capture());
    SecretDeleteEvent secretDeleteEvent = secretDeleteEventArgumentCaptor.getValue();
    assertThat(secret.getIdentifier()).isEqualTo(secretDeleteEvent.getSecret().getIdentifier());
    assertThat("SecretDeleted").isEqualTo(secretDeleteEvent.getEventType());
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void deleteInternal_withForceDeleteTrue() {
    doNothing().when(ngSecretActivityService).deleteAllActivities(any(), any());
    doNothing().when(secretRepository).delete(any());
    boolean success = secretServiceV2Spy.deleteInternal("account", "org", "proj", "identifier", secret, true);
    assertThat(success).isTrue();
    verify(outboxService, times(1)).save(secretForceDeleteEventArgumentCaptor.capture());
    SecretForceDeleteEvent secretDeleteEvent = secretForceDeleteEventArgumentCaptor.getValue();
    assertThat(secret.getIdentifier()).isEqualTo(secretDeleteEvent.getSecret().getIdentifier());
    assertThat("SecretForceDeleted").isEqualTo(secretDeleteEvent.getEventType());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreate() {
    SecretDTOV2 secretDTOV2 = getSecretDTO();
    Secret secret = Secret.fromDTO(secretDTOV2);
    secret.setAccountIdentifier(ACC_ID);
    when(secretRepository.save(any())).thenReturn(secret);

    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret savedSecret = secretServiceV2.create("account", scopeInfo, secretDTOV2, false);
    assertThat(secret).isNotNull();
    assertThat(secret).isEqualTo(savedSecret);
    ArgumentCaptor<Secret> captor = ArgumentCaptor.forClass(Secret.class);
    verify(secretRepository, times(1)).save(captor.capture());

    Secret actualSecret = captor.getValue();
    assertThat(actualSecret.getParentUniqueId()).isNotEmpty();
    assertThat(actualSecret.getParentUniqueId()).isEqualTo("account");
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdate() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = Secret.builder().build();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    SecretDTOV2 secretDTOV2 = getSecretDTO();
    when(secretRepository.save(any())).thenReturn(secret);

    Secret success = secretServiceV2Spy.update(scopeInfo, secretDTOV2, false);
    assertThat(success).isNotNull();
    verify(secretServiceV2Spy).get(eq(scopeInfo), any());
    verify(secretRepository, times(1)).save(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidateForNonSSHType() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = Secret.builder().type(SecretType.SecretText).build();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    SecretValidationResultDTO secretValidationResultDTO =
        secretServiceV2Spy.validateSecret(scopeInfo, "identifier", null);
    assertThat(secretValidationResultDTO.isSuccess()).isEqualTo(false);
  }

  private SSHKeyValidationMetadata getMetadata() {
    return SSHKeyValidationMetadata.builder().host("1.2.3.4").build();
  }

  private Secret getSecret() {
    return Secret.builder().type(SecretType.SSHKey).build();
  }

  private WinRmCredentialsValidationMetadata getWinRmMetaData() {
    return WinRmCredentialsValidationMetadata.builder().host("test").build();
  }

  private Secret getWinRmSecret() {
    return Secret.builder().type(SecretType.WinRmCredentials).build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidationForSSHWithPassword() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecretPasswordCredentialType();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO = secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidationForSSHWithKeyReference() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.SSH)
                                       .sshSpec(SSHConfig.builder()
                                                    .credentialType(SSHCredentialType.KeyReference)
                                                    .spec(SSHKeyCredential.builder()
                                                              .userName("username")
                                                              .key(SecretRefData.builder().build())
                                                              .build())
                                                    .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO = secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testValidationForSSHWithKeyReferenceNoDelegateException() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.SSH)
                                       .sshSpec(SSHConfig.builder()
                                                    .credentialType(SSHCredentialType.KeyReference)
                                                    .spec(SSHKeyCredential.builder()
                                                              .userName("username")
                                                              .key(SecretRefData.builder().build())
                                                              .build())
                                                    .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenThrow(new DelegateServiceDriverException("No eligible delegate(s) found in the account."));
    exceptionRule.expect(HintException.class);
    exceptionRule.expectMessage(
        String.format(HintException.DELEGATE_NOT_AVAILABLE, DocumentLinksConstants.DELEGATE_INSTALLATION_LINK));
    secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidationForSSHWithKeyPath() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(
        SSHExecutionCredentialSpec.builder()
            .port(22)
            .auth(SSHAuth.builder()
                      .type(SSHAuthScheme.SSH)
                      .sshSpec(SSHConfig.builder()
                                   .credentialType(SSHCredentialType.KeyPath)
                                   .spec(SSHKeyPathCredential.builder().userName("username").keyPath("/a/b/c").build())
                                   .build())
                      .build())
            .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO = secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testValidationForKerberos() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.Kerberos)
                                       .sshSpec(KerberosConfig.builder()
                                                    .principal("principal")
                                                    .realm("realm")
                                                    .tgtGenerationMethod(TGTGenerationMethod.KeyTabFilePath)
                                                    .spec(TGTKeyTabFilePathSpec.builder().keyPath("/a/b/c").build())
                                                    .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO = secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testValidationForWinRmNTLM() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getWinRmSecret();
    secret.setSecretSpec(
        WinRmCredentialsSpec.builder()
            .port(5986)
            .auth(WinRmAuth.builder()
                      .type(WinRmAuthScheme.NTLM)
                      .spec(NTLMConfig.builder().username("user").password(SecretRefData.builder().build()).build())
                      .build())
            .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(WinRmConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO =
        secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getWinRmMetaData());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testValidationForWinRmKerberosPassword() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getWinRmSecret();
    secret.setSecretSpec(
        WinRmCredentialsSpec.builder()
            .port(5986)
            .auth(WinRmAuth.builder()
                      .type(WinRmAuthScheme.Kerberos)
                      .spec(KerberosWinRmConfig.builder()
                                .principal("principal")
                                .realm("realm")
                                .tgtGenerationMethod(TGTGenerationMethod.Password)
                                .spec(TGTPasswordSpec.builder().password(SecretRefData.builder().build()).build())
                                .build())
                      .build())
            .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(WinRmConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO =
        secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getWinRmMetaData());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testValidationForWinRmKerberosKeyTab() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getWinRmSecret();
    secret.setSecretSpec(WinRmCredentialsSpec.builder()
                             .port(5986)
                             .auth(WinRmAuth.builder()
                                       .type(WinRmAuthScheme.Kerberos)
                                       .spec(KerberosWinRmConfig.builder()
                                                 .principal("principal")
                                                 .realm("realm")
                                                 .tgtGenerationMethod(TGTGenerationMethod.KeyTabFilePath)
                                                 .spec(TGTKeyTabFilePathSpec.builder().keyPath("/a/b/c").build())
                                                 .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(WinRmConfigValidationTaskResponse.builder().connectionSuccessful(true).build());
    SecretValidationResultDTO resultDTO =
        secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getWinRmMetaData());
    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testGetPaginatedResults() {
    Secret secret1 =
        Secret.builder().name("name1").type(SecretType.SecretText).identifier("id1").createdAt((long) 2).build();
    Secret secret2 =
        Secret.builder().name("name2").type(SecretType.SecretText).identifier("id2").createdAt((long) 7).build();
    Secret secret3 =
        Secret.builder().name("name3").type(SecretType.SecretText).identifier("id3").createdAt((long) 3).build();
    Page<Secret> paginatedResult = secretServiceV2.getPaginatedResult(Arrays.asList(secret1, secret2, secret3), 1, 2);
    assertThat(paginatedResult.getContent().size()).isEqualTo(1);
    assertThat(paginatedResult.getContent()).isEqualTo(Arrays.asList(secret3));
  }

  @Test
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetPermittedUsingCheckForAccessOrThrow() {
    Secret secret1 =
        Secret.builder().name("name1").type(SecretType.SecretText).identifier("id1").createdAt((long) 2).build();

    AccessCheckResponseDTO accessCheckResponseDTO =
        AccessCheckResponseDTO.builder().accessControlList(of(AccessControlDTO.builder().build())).build();
    when(accessControlClient.checkForAccessOrThrow(any())).thenReturn(accessCheckResponseDTO);

    secretServiceV2.getPermitted(of(secret1));

    verify(accessControlClient, times(1)).checkForAccessOrThrow(anyList());
  }

  @Test(expected = NGAccessDeniedException.class)
  @Owner(developers = ASHISHSANODIA)
  @Category(UnitTests.class)
  public void testGetPermittedUsingCheckForAccessOrThrowAndGotException() {
    Secret secret1 =
        Secret.builder().name("name1").type(SecretType.SecretText).identifier("id1").createdAt((long) 2).build();

    doThrow(NGAccessDeniedException.class).when(accessControlClient).checkForAccessOrThrow(any());

    secretServiceV2.getPermitted(of(secret1));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHSuccess() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecretPasswordCredentialType();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build());

    SecretValidationResultDTO resultDTO = secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata());

    assertThat(resultDTO.isSuccess()).isEqualTo(true);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHHostNotReachable() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecretPasswordCredentialType();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("Unable to connect to host").build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage())
        .isEqualTo(
            "Please ensure if port is opened on host. Check firewall rules between the delegate and host. Try to test connectivity by telnet");
    ExplanationException explanationException = (ExplanationException) exception.getCause();
    assertThat(explanationException.getMessage())
        .isEqualTo(
            "Delegate(s) is(are) not able to establish socket connection to host(s). If the port is not specified with the host, it will default to 22.");
    InvalidRequestException ex = (InvalidRequestException) explanationException.getCause();
    assertThat(ex.getMessage()).isEqualTo("Unable to connect to host");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHNoErrorMsg() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecretPasswordCredentialType();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().errorMessage(null).build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage())
        .isEqualTo("Validation failed, reason not found, please contact Harness support for more help");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHCheckAllSettingsOnConfigurationPage() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecretPasswordCredentialType();
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder().errorMessage("Gateway timeout").build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage())
        .isEqualTo("Please validate the settings on the configuration page and try again.");
    InvalidRequestException ex = (InvalidRequestException) exception.getCause();
    assertThat(ex.getMessage()).isEqualTo("Gateway timeout");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHInvalidCredentials() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.SSH)
                                       .sshSpec(SSHConfig.builder()
                                                    .credentialType(SSHCredentialType.Password)
                                                    .spec(SSHPasswordCredential.builder()
                                                              .userName("notValidUsername")
                                                              .password(SecretRefData.builder().build())
                                                              .build())
                                                    .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(
            SSHConfigValidationTaskResponse.builder()
                .errorMessage(String.format("Failed to get session due to - %s", ErrorCode.INVALID_CREDENTIAL.name()))
                .build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage()).isEqualTo("Please provide valid credentials on configuration page.");
    ExplanationException explanationException = (ExplanationException) exception.getCause();
    assertThat(explanationException.getMessage()).isEqualTo("Username or SSK key/Password is not valid");
    InvalidRequestException ex = (InvalidRequestException) explanationException.getCause();
    assertThat(ex.getMessage()).isEqualTo("Failed to get session due to - INVALID_CREDENTIAL");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHInvalidKeyPath() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.SSH)
                                       .sshSpec(SSHConfig.builder()
                                                    .credentialType(SSHCredentialType.KeyPath)
                                                    .spec(SSHKeyPathCredential.builder()
                                                              .userName("notValidUsername")
                                                              .keyPath("not/existing/key/path")
                                                              .build())
                                                    .build())
                                       .build())
                             .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(
            SSHConfigValidationTaskResponse.builder()
                .errorMessage(String.format("Failed to get session due to - %s", ErrorCode.INVALID_KEYPATH.name()))
                .build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage()).isEqualTo("Please provide valid credentials on configuration page.");
    ExplanationException explanationException = (ExplanationException) exception.getCause();
    assertThat(explanationException.getMessage()).isEqualTo("SSH Key File Path is not valid");
    InvalidRequestException ex = (InvalidRequestException) explanationException.getCause();
    assertThat(ex.getMessage()).isEqualTo("Failed to get session due to - INVALID_KEYPATH");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testProcessValidationTaskResponseSSHInvalidKey() {
    ScopeInfo scopeInfo =
        ScopeInfo.builder().accountIdentifier("account").uniqueId("account").scopeType(ScopeLevel.ACCOUNT).build();
    Secret secret = getSecret();
    secret.setSecretSpec(
        SSHExecutionCredentialSpec.builder()
            .port(22)
            .auth(SSHAuth.builder()
                      .type(SSHAuthScheme.SSH)
                      .sshSpec(SSHConfig.builder()
                                   .credentialType(SSHCredentialType.KeyReference)
                                   .spec(SSHKeyCredential.builder()
                                             .userName("Username")
                                             .key(SecretRefData.builder().identifier("not_valid_ssh_key").build())
                                             .build())
                                   .build())
                      .build())
            .build());
    doReturn(Optional.of(secret)).when(secretServiceV2Spy).get(eq(scopeInfo), any());
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(SSHConfigValidationTaskResponse.builder()
                        .errorMessage(String.format("Failed to get session due to - %s", ErrorCode.INVALID_KEY.name()))
                        .build());
    doReturn(new WingsException("wings exception message"))
        .when(exceptionManager)
        .processException(any(), any(), any());

    assertThatThrownBy(() -> secretServiceV2Spy.validateSecret(scopeInfo, "identifier", getMetadata()))
        .hasMessage("wings exception message")
        .isInstanceOf(WingsException.class);

    verify(exceptionManager, times(1)).processException(exceptionArgumentCaptor.capture(), any(), any());
    Exception exception = exceptionArgumentCaptor.getValue();
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage()).isEqualTo("Please provide valid credentials on configuration page.");
    ExplanationException explanationException = (ExplanationException) exception.getCause();
    assertThat(explanationException.getMessage()).isEqualTo("SSH Key is not valid");
    InvalidRequestException ex = (InvalidRequestException) explanationException.getCause();
    assertThat(ex.getMessage()).isEqualTo("Failed to get session due to - INVALID_KEY");
  }

  @NotNull
  private Secret getSecretPasswordCredentialType() {
    Secret secret = getSecret();
    secret.setSecretSpec(SSHExecutionCredentialSpec.builder()
                             .port(22)
                             .auth(SSHAuth.builder()
                                       .type(SSHAuthScheme.SSH)
                                       .sshSpec(SSHConfig.builder()
                                                    .credentialType(SSHCredentialType.Password)
                                                    .spec(SSHPasswordCredential.builder()
                                                              .userName("username")
                                                              .password(SecretRefData.builder().build())
                                                              .build())
                                                    .build())
                                       .build())
                             .build());
    return secret;
  }
}
