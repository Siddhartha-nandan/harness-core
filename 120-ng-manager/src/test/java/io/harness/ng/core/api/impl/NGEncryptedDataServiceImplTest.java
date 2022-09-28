/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.TEJAS;
import static io.harness.rule.OwnerRule.VIKAS_M;
import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptedSecretValue;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.connector.helper.CustomSecretManagerHelper;
import io.harness.connector.services.NGConnectorSecretManagerService;
import io.harness.encryptors.CustomEncryptorsRegistry;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.exception.InvalidRequestException;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.core.dao.NGEncryptedDataDao;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.azurekeyvault.AzureKeyVaultConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.utils.featureflaghelper.NGFeatureFlagHelperService;

import software.wings.beans.AzureVaultConfig;
import software.wings.beans.BaseVaultConfig;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.impl.security.NGEncryptorService;
import software.wings.settings.SettingVariableTypes;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@OwnedBy(PL)
public class NGEncryptedDataServiceImplTest extends CategoryTest {
  private NGEncryptedDataServiceImpl ngEncryptedDataService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private SecretManagerClient secretManagerClient;
  @Mock private NGEncryptedDataDao encryptedDataDao;
  @Mock private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Mock private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Mock private SecretsFileService secretsFileService;
  @Mock private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  NGConnectorSecretManagerService ngConnectorSecretManagerService;
  @Mock private NGFeatureFlagHelperService ngFeatureFlagHelperService;
  @Mock private VaultEncryptor vaultEncryptor;
  @Mock private CustomEncryptorsRegistry customEncryptorsRegistry;
  @Mock private CustomSecretManagerHelper customSecretManagerHelper;
  @Mock private NGEncryptorService ngEncryptorService;
  @Mock private LocalEncryptor localEncryptor;
  public static final String HTTP_VAULT_URL = "http://vault.com";
  private String accountIdentifier = randomAlphabetic(10);
  private String orgIdentifier = randomAlphabetic(10);
  private String projectIdentifier = randomAlphabetic(10);
  private String identifier = randomAlphabetic(10);

  @Before
  public void setup() {
    initMocks(this);
    ngConnectorSecretManagerService = mock(NGConnectorSecretManagerService.class);
    ngEncryptedDataService =
        spy(new NGEncryptedDataServiceImpl(encryptedDataDao, kmsEncryptorsRegistry, vaultEncryptorsRegistry,
            secretsFileService, secretManagerClient, globalEncryptDecryptClient, ngConnectorSecretManagerService,
            ngFeatureFlagHelperService, customEncryptorsRegistry, customSecretManagerHelper, ngEncryptorService));
    when(vaultEncryptorsRegistry.getVaultEncryptor(any())).thenReturn(vaultEncryptor);
    when(kmsEncryptorsRegistry.getKmsEncryptor(any())).thenReturn(localEncryptor);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateSecret_withVault_doNotRenewAppRoleToken_FF_disabled() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .type(SecretType.SecretText)
                                  .spec(SecretTextSpecDTO.builder()
                                            .secretManagerIdentifier(identifier)
                                            .valueType(ValueType.Inline)
                                            .value("value")
                                            .build())
                                  .build();
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(null);
    when(encryptedDataDao.save(any())).thenReturn(encryptedDataDTO);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(false);
    SecretManagerConfigDTO vaultConfigDTO = VaultConfigDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .appRoleId("appRoleId")
                                                .secretId("secretId")
                                                .renewAppRoleToken(true)
                                                .build();
    vaultConfigDTO.setEncryptionType(VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.createSecret(any(), any(), any(), argumentCaptor.capture()))
        .thenReturn(NGEncryptedData.builder()
                        .name("name")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionKey("encryptionKey")
                        .build());
    NGEncryptedData result = ngEncryptedDataService.createSecretText(accountIdentifier, secretDTOV2);
    assertThat(result).isNotNull();
    verify(ngFeatureFlagHelperService, times(1)).isEnabled(any(), any());
    BaseVaultConfig vaultConfig = (BaseVaultConfig) argumentCaptor.getValue();
    assertThat(vaultConfig.getRenewAppRoleToken()).isEqualTo(true);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateSecret_withVault_doNotRenewAppRoleToken_FF_enabled() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .type(SecretType.SecretText)
                                  .spec(SecretTextSpecDTO.builder()
                                            .secretManagerIdentifier(identifier)
                                            .valueType(ValueType.Inline)
                                            .value("value")
                                            .build())
                                  .build();
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(null);
    when(encryptedDataDao.save(any())).thenReturn(encryptedDataDTO);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(true);
    SecretManagerConfigDTO vaultConfigDTO = VaultConfigDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .appRoleId("appRoleId")
                                                .secretId("secretId")
                                                .renewAppRoleToken(true)
                                                .build();
    vaultConfigDTO.setEncryptionType(VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.createSecret(any(), any(), any(), argumentCaptor.capture()))
        .thenReturn(NGEncryptedData.builder()
                        .name("name")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionKey("encryptionKey")
                        .build());
    NGEncryptedData result = ngEncryptedDataService.createSecretText(accountIdentifier, secretDTOV2);
    assertThat(result).isNotNull();
    verify(ngFeatureFlagHelperService, times(1)).isEnabled(any(), any());
    BaseVaultConfig vaultConfig = (BaseVaultConfig) argumentCaptor.getValue();
    assertThat(vaultConfig.getRenewAppRoleToken()).isEqualTo(false);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSecret_appRoleBased_doNotRenewToken_ff_enabled() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String encryptedValue = randomAlphabetic(10);
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .encryptedValue(encryptedValue.toCharArray())
                                           .secretManagerIdentifier(identifier)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(encryptedDataDTO);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(true);
    SecretManagerConfigDTO vaultConfigDTO = VaultConfigDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .appRoleId("appRoleId")
                                                .secretId("secretId")
                                                .renewAppRoleToken(true)
                                                .build();
    vaultConfigDTO.setEncryptionType(VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.deleteSecret(any(), any(), argumentCaptor.capture())).thenReturn(true);
    when(encryptedDataDao.delete(any(), any(), any(), any())).thenReturn(true);
    boolean deleted = ngEncryptedDataService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    verify(ngFeatureFlagHelperService, times(1)).isEnabled(any(), any());
    BaseVaultConfig vaultConfig = (BaseVaultConfig) argumentCaptor.getValue();
    assertThat(vaultConfig.getRenewAppRoleToken()).isEqualTo(false);
    assertThat(deleted).isEqualTo(true);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSecret_appRoleBased_doNotRenewToken_ff_disabled() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String encryptedValue = randomAlphabetic(10);
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .encryptedValue(encryptedValue.toCharArray())
                                           .secretManagerIdentifier(identifier)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(encryptedDataDTO);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(false);
    SecretManagerConfigDTO vaultConfigDTO = VaultConfigDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .appRoleId("appRoleId")
                                                .secretId("secretId")
                                                .renewAppRoleToken(true)
                                                .build();
    vaultConfigDTO.setEncryptionType(VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.deleteSecret(any(), any(), argumentCaptor.capture())).thenReturn(true);
    when(encryptedDataDao.delete(any(), any(), any(), any())).thenReturn(true);
    boolean deleted = ngEncryptedDataService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    verify(ngFeatureFlagHelperService, times(1)).isEnabled(any(), any());
    BaseVaultConfig vaultConfig = (BaseVaultConfig) argumentCaptor.getValue();
    assertThat(vaultConfig.getRenewAppRoleToken()).isEqualTo(true);
    assertThat(deleted).isEqualTo(true);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateSecret_azureVault() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    SecretDTOV2 secretDTOV2 = SecretDTOV2.builder()
                                  .type(SecretType.SecretText)
                                  .spec(SecretTextSpecDTO.builder()
                                            .secretManagerIdentifier(identifier)
                                            .valueType(ValueType.Inline)
                                            .value("value")
                                            .build())
                                  .build();
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(null);
    when(encryptedDataDao.save(any())).thenReturn(encryptedDataDTO);
    AzureKeyVaultConfigDTO vaultConfigDTO = AzureKeyVaultConfigDTO.builder()
                                                .clientId("cliendId")
                                                .secretKey("secretKey")
                                                .tenantId("tenantId")
                                                .subscription("subscription")
                                                .build();
    vaultConfigDTO.setEncryptionType(AZURE_VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.createSecret(any(), any(), any(), argumentCaptor.capture()))
        .thenReturn(NGEncryptedData.builder()
                        .name("name")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionKey("encryptionKey")
                        .build());
    NGEncryptedData result = ngEncryptedDataService.createSecretText(accountIdentifier, secretDTOV2);
    assertThat(result).isNotNull();
    verify(ngFeatureFlagHelperService, times(0)).isEnabled(any(), any());
    SecretManagerConfig secretManagerConfig = argumentCaptor.getValue();
    assertThat(secretManagerConfig instanceof AzureVaultConfig).isEqualTo(true);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSecret_azureVault() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String encryptedValue = randomAlphabetic(10);
    NGEncryptedData encryptedDataDTO = NGEncryptedData.builder()
                                           .accountIdentifier(accountIdentifier)
                                           .orgIdentifier(orgIdentifier)
                                           .projectIdentifier(projectIdentifier)
                                           .type(SettingVariableTypes.SECRET_TEXT)
                                           .encryptedValue(encryptedValue.toCharArray())
                                           .secretManagerIdentifier(identifier)
                                           .build();
    when(encryptedDataDao.get(any(), any(), any(), any())).thenReturn(encryptedDataDTO);
    AzureKeyVaultConfigDTO vaultConfigDTO = AzureKeyVaultConfigDTO.builder()
                                                .clientId("cliendId")
                                                .secretKey("secretKey")
                                                .tenantId("tenantId")
                                                .subscription("subscription")
                                                .build();
    vaultConfigDTO.setEncryptionType(AZURE_VAULT);
    when(ngConnectorSecretManagerService.getUsingIdentifier(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(vaultConfigDTO);
    ArgumentCaptor<SecretManagerConfig> argumentCaptor = ArgumentCaptor.forClass(SecretManagerConfig.class);
    when(vaultEncryptor.deleteSecret(any(), any(), argumentCaptor.capture())).thenReturn(true);
    when(encryptedDataDao.delete(any(), any(), any(), any())).thenReturn(true);
    boolean deleted = ngEncryptedDataService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    verify(ngFeatureFlagHelperService, times(0)).isEnabled(any(), any());
    SecretManagerConfig secretManagerConfig = argumentCaptor.getValue();
    assertThat(secretManagerConfig instanceof AzureVaultConfig).isEqualTo(true);
    assertThat(deleted).isEqualTo(true);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testDecryptSecret_Success() {
    String secretManagerIdentifier = randomAlphabetic(10);
    char[] secretValue = randomAlphabetic(10).toCharArray();
    SecretManagerConfigDTO secretManagerConfigDTO =
        LocalConfigDTO.builder().harnessManaged(true).encryptionType(LOCAL).build();
    NGEncryptedData encryptedData = NGEncryptedData.builder().secretManagerIdentifier(secretManagerIdentifier).build();
    EncryptedRecordData encryptedRecordData = EncryptedRecordData.builder().build();
    EncryptionConfig encryptionConfig = SecretManagerConfigMapper.fromDTO(secretManagerConfigDTO);

    when(encryptedDataDao.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier))
        .thenReturn(encryptedData);
    when(ngConnectorSecretManagerService.getUsingIdentifier(
             accountIdentifier, orgIdentifier, projectIdentifier, secretManagerIdentifier, false))
        .thenReturn(secretManagerConfigDTO);
    when(globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
             encryptedData, accountIdentifier, encryptionConfig))
        .thenReturn(encryptedRecordData);
    when(localEncryptor.fetchSecretValue(accountIdentifier, encryptedData, encryptionConfig)).thenReturn(secretValue);
    DecryptedSecretValue decryptedSecretValue =
        ngEncryptedDataService.decryptSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    assertEquals(decryptedSecretValue.getDecryptedValue(), String.valueOf(secretValue));
    assertEquals(decryptedSecretValue.getAccountIdentifier(), accountIdentifier);
    assertEquals(decryptedSecretValue.getOrgIdentifier(), orgIdentifier);
    assertEquals(decryptedSecretValue.getProjectIdentifier(), projectIdentifier);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testDecryptSecret_secretManagerNotFound() {
    String secretManagerIdentifier = randomAlphabetic(10);
    char[] secretValue = randomAlphabetic(10).toCharArray();
    NGEncryptedData encryptedData = NGEncryptedData.builder().secretManagerIdentifier(secretManagerIdentifier).build();

    when(encryptedDataDao.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier))
        .thenReturn(encryptedData);
    when(ngConnectorSecretManagerService.getUsingIdentifier(
             accountIdentifier, orgIdentifier, projectIdentifier, secretManagerIdentifier, false))
        .thenReturn(null);
    try {
      DecryptedSecretValue decryptedSecretValue =
          ngEncryptedDataService.decryptSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
      fail("InvalidRequestException should be thrown as Secret Manager is not found");
    } catch (InvalidRequestException ex) {
      assertEquals(ex.getMessage(),
          String.format("Secret manager with the identifier {%s} does not exist", secretManagerIdentifier));
    } catch (Exception ex) {
      fail("Unexpected exception occured");
    }
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testDecryptSecret_secretManagerNotHarnessManaged() {
    String secretManagerIdentifier = randomAlphabetic(10);
    char[] secretValue = randomAlphabetic(10).toCharArray();
    SecretManagerConfigDTO secretManager = VaultConfigDTO.builder()
                                               .harnessManaged(false)
                                               .encryptionType(VAULT)
                                               .secretId(randomAlphabetic(10))
                                               .accountIdentifier(accountIdentifier)
                                               .authToken(randomAlphabetic(10))
                                               .build();
    NGEncryptedData encryptedData = NGEncryptedData.builder().secretManagerIdentifier(secretManagerIdentifier).build();
    EncryptedRecordData encryptedRecordData = EncryptedRecordData.builder().build();
    EncryptionConfig encryptionConfig = SecretManagerConfigMapper.fromDTO(secretManager);

    when(encryptedDataDao.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier))
        .thenReturn(encryptedData);
    when(ngConnectorSecretManagerService.getUsingIdentifier(
             accountIdentifier, orgIdentifier, projectIdentifier, secretManagerIdentifier, false))
        .thenReturn(secretManager);
    try {
      DecryptedSecretValue decryptedSecretValue =
          ngEncryptedDataService.decryptSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
      fail("InvalidRequestException should be thrown as Secret Manager is not Harness Managed");
    } catch (InvalidRequestException ex) {
      assertEquals(
          ex.getMessage(), "Decryption is supported only for secrets encrypted via harness managed secret managers");
    } catch (Exception ex) {
      fail("Unexpected exception occured");
    }
  }
}
