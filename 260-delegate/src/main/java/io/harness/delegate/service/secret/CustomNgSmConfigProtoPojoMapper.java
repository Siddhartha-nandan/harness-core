/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import static io.harness.delegate.core.beans.KerberosConfig.TGTGenerationMethod;
import static io.harness.delegate.core.beans.KerberosConfig.TGTGenerationMethod.KEY_TAB_FILE_PATH;
import static io.harness.ng.core.dto.secrets.TGTGenerationMethod.KeyTabFilePath;
import static io.harness.ng.core.dto.secrets.TGTGenerationMethod.Password;

import io.harness.delegate.beans.connector.customsecretmanager.TemplateLinkConfigForCustomSecretManager;
import io.harness.delegate.core.beans.NameValuePairWithDefaultList;
import io.harness.delegate.core.beans.SSHConfig;
import io.harness.delegate.core.beans.SSHKey;
import io.harness.delegate.core.beans.TemplateLinkConfig;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialType;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.NameValuePairWithDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomNgSmConfigProtoPojoMapper {
  public static software.wings.beans.CustomSecretNGManagerConfig protoPojoMapper(
      io.harness.delegate.core.beans.EncryptionConfig config) {
    software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig =
        new software.wings.beans.CustomSecretNGManagerConfig();

    mapCommonFields(config, customSecretNGManagerConfig);
    mapSshKeyFields(config, customSecretNGManagerConfig);

    return customSecretNGManagerConfig;
  }

  private static void mapCommonFields(io.harness.delegate.core.beans.EncryptionConfig config,
      software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig) {
    customSecretNGManagerConfig.setAccountId(config.getAccountId());
    customSecretNGManagerConfig.setUuid(config.getUuid());
    customSecretNGManagerConfig.setEncryptionType(EncryptionType.CUSTOM);
    customSecretNGManagerConfig.setScript(config.getCustomSecretManagerConfig().getScript());
    customSecretNGManagerConfig.setOnDelegate(config.getCustomSecretManagerConfig().getIsOnDelegate());
    customSecretNGManagerConfig.setWorkingDirectory(config.getCustomSecretManagerConfig().getWorkingDirectory());
    customSecretNGManagerConfig.setHost(config.getCustomSecretManagerConfig().getHost());
    customSecretNGManagerConfig.setConnectorRef(config.getCustomSecretManagerConfig().getConnectorRef());

    mapEncryptedDataDetails(config, customSecretNGManagerConfig);
    customSecretNGManagerConfig.setTemplate(
        mapProtoToPojo(config.getCustomSecretManagerConfig().getTemplateLinkConfig()));
  }

  private static void mapEncryptedDataDetails(io.harness.delegate.core.beans.EncryptionConfig config,
      software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig) {
    EncryptedDataDetail encryptedDataDetail = new EncryptedDataDetail();
    encryptedDataDetail.setEncryptedData(EncryptedDataRecordProtoPojoMapper.map(
        config.getCustomSecretManagerConfig().getSshEncryptionDetails(0).getEncryptedData()));
    encryptedDataDetail.setEncryptionConfig(EncryptionConfigProtoPojoMapper.INSTANCE.map(
        config.getCustomSecretManagerConfig().getSshEncryptionDetails(0).getConfig()));
    encryptedDataDetail.setFieldName("key");
    List<EncryptedDataDetail> sshEncryptionDetails = new ArrayList<>();
    sshEncryptionDetails.add(encryptedDataDetail);
    customSecretNGManagerConfig.setSshKeyEncryptionDetails(sshEncryptionDetails);
  }

  private static void mapSshKeyFields(io.harness.delegate.core.beans.EncryptionConfig config,
      software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig) {
    SSHKey.SSHAuthScheme authScheme = config.getCustomSecretManagerConfig().getSshKey().getSshAuthScheme();

    if (authScheme == SSHKey.SSHAuthScheme.SSH) {
      mapSshAuthDTOFields(config, customSecretNGManagerConfig);
    } else {
      mapKerberosAuthDTOFields(config, customSecretNGManagerConfig);
    }
  }

  private static void mapSshAuthDTOFields(io.harness.delegate.core.beans.EncryptionConfig config,
      software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig) {
    // Implementation for SSH Auth DTO mapping
    SSHAuthDTO sshAuthDTO = new SSHAuthDTO();
    sshAuthDTO.setAuthScheme(SSHAuthScheme.SSH);
    SSHConfigDTO sshConfigDTO = new SSHConfigDTO();
    SSHConfig.SSHCredentialType sshCredentialType =
        config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getSshCredentialType();
    if (sshCredentialType == SSHConfig.SSHCredentialType.PASSWORD) {
      // handle password
      handleSSHCredentials_typePassword(config, sshConfigDTO);
    } else if (sshCredentialType == SSHConfig.SSHCredentialType.KEY_PATH) {
      // handle key_path
      handleSSHCredentials_typeKeyPath(config, sshConfigDTO);
    } else {
      // handle key_reference
      handleSSHCredentials_typeKeyReference(config, sshConfigDTO);
    }
    sshAuthDTO.setSpec(sshConfigDTO);
    sshAuthDTO.setUseSshClient(config.getCustomSecretManagerConfig().getSshKey().getUseSshClient());
    sshAuthDTO.setUseSshj(config.getCustomSecretManagerConfig().getSshKey().getUseSshJ());
    SSHKeySpecDTO sshKeySpecDTO =
        new SSHKeySpecDTO(config.getCustomSecretManagerConfig().getSshKey().getPort(), sshAuthDTO);
    customSecretNGManagerConfig.setSshKeySpecDTO(sshKeySpecDTO);
  }

  private static void handleSSHCredentials_typePassword(
      io.harness.delegate.core.beans.EncryptionConfig config, SSHConfigDTO sshConfigDTO) {
    SSHPasswordCredentialDTO sshPasswordCredentialDTO =
        SSHPasswordCredentialDTO.builder()
            .userName(
                config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getPasswordCredential().getUsername())
            .password(new SecretRefData(
                config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getPasswordCredential().getPassword()))
            .build();
    sshConfigDTO.setCredentialType(SSHCredentialType.Password);
    sshConfigDTO.setSpec(sshPasswordCredentialDTO);
  }

  private static void handleSSHCredentials_typeKeyPath(
      io.harness.delegate.core.beans.EncryptionConfig config, SSHConfigDTO sshConfigDTO) {
    SSHKeyPathCredentialDTO sshKeyPathCredentialDTO =
        SSHKeyPathCredentialDTO.builder()
            .userName(
                config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getKeyPathCredential().getUsername())
            .keyPath(
                config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getKeyPathCredential().getKeyPath())
            .encryptedPassphrase(new SecretRefData(config.getCustomSecretManagerConfig()
                                                       .getSshKey()
                                                       .getSshConfig()
                                                       .getKeyPathCredential()
                                                       .getPassPhrase()))
            .build();
    sshConfigDTO.setCredentialType(SSHCredentialType.KeyPath);
    sshConfigDTO.setSpec(sshKeyPathCredentialDTO);
  }

  private static void handleSSHCredentials_typeKeyReference(
      io.harness.delegate.core.beans.EncryptionConfig config, SSHConfigDTO sshConfigDTO) {
    SSHKeyReferenceCredentialDTO sshKeyReferenceCredentialDTO =
        SSHKeyReferenceCredentialDTO.builder()
            .userName(config.getCustomSecretManagerConfig()
                          .getSshKey()
                          .getSshConfig()
                          .getKeyReferenceCredential()
                          .getUsername())
            .key(new SecretRefData(
                config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getKeyReferenceCredential().getKey()))
            .encryptedPassphrase(new SecretRefData(config.getCustomSecretManagerConfig()
                                                       .getSshKey()
                                                       .getSshConfig()
                                                       .getKeyReferenceCredential()
                                                       .getPassPhrase()))
            .build();
    sshConfigDTO.setCredentialType(SSHCredentialType.KeyReference);
    sshConfigDTO.setSpec(sshKeyReferenceCredentialDTO);
  }

  private static void handleKerberosCredentials_typeKeyTabFilePath(
      io.harness.delegate.core.beans.EncryptionConfig config, KerberosConfigDTO kerberosConfigDTO) {
    TGTKeyTabFilePathSpecDTO tgtKeyTabFilePathSpecDTO = TGTKeyTabFilePathSpecDTO.builder()
                                                            .keyPath(config.getCustomSecretManagerConfig()
                                                                         .getSshKey()
                                                                         .getKerberosConfig()
                                                                         .getTgtTabFilePathSpec()
                                                                         .getKeyPath())
                                                            .build();
    kerberosConfigDTO.setPrincipal(
        config.getCustomSecretManagerConfig().getSshKey().getKerberosConfig().getPrincipal());
    kerberosConfigDTO.setRealm(config.getCustomSecretManagerConfig().getSshKey().getKerberosConfig().getRealm());
    kerberosConfigDTO.setTgtGenerationMethod(KeyTabFilePath);
    kerberosConfigDTO.setSpec(tgtKeyTabFilePathSpecDTO);
  }

  private static void handleKerberosCredentials_typePassword(
      io.harness.delegate.core.beans.EncryptionConfig config, KerberosConfigDTO kerberosConfigDTO) {
    TGTPasswordSpecDTO tgtPasswordSpecDTO = TGTPasswordSpecDTO.builder()
                                                .password(new SecretRefData(config.getCustomSecretManagerConfig()
                                                                                .getSshKey()
                                                                                .getKerberosConfig()
                                                                                .getTgtPasswordSpec()
                                                                                .getPassword()))
                                                .build();
    kerberosConfigDTO.setTgtGenerationMethod(Password);
    kerberosConfigDTO.setSpec(tgtPasswordSpecDTO);
  }

  private static void mapKerberosAuthDTOFields(io.harness.delegate.core.beans.EncryptionConfig config,
      software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig) {
    // Implementation for Kerberos Auth DTO mapping
    SSHAuthDTO sshAuthDTO = new SSHAuthDTO();
    sshAuthDTO.setAuthScheme(SSHAuthScheme.Kerberos);
    KerberosConfigDTO kerberosConfigDTO = new KerberosConfigDTO();
    TGTGenerationMethod tgtGenerationMethod =
        config.getCustomSecretManagerConfig().getSshKey().getKerberosConfig().getTgtGenerationMethod();
    if (tgtGenerationMethod == KEY_TAB_FILE_PATH) {
      handleKerberosCredentials_typeKeyTabFilePath(config, kerberosConfigDTO);
    } else {
      handleKerberosCredentials_typePassword(config, kerberosConfigDTO);
    }
    sshAuthDTO.setSpec(kerberosConfigDTO);
    sshAuthDTO.setUseSshClient(config.getCustomSecretManagerConfig().getSshKey().getUseSshClient());
    sshAuthDTO.setUseSshj(config.getCustomSecretManagerConfig().getSshKey().getUseSshJ());
    SSHKeySpecDTO sshKeySpecDTO =
        new SSHKeySpecDTO(config.getCustomSecretManagerConfig().getSshKey().getPort(), sshAuthDTO);
    customSecretNGManagerConfig.setSshKeySpecDTO(sshKeySpecDTO);
  }

  public static TemplateLinkConfigForCustomSecretManager mapProtoToPojo(TemplateLinkConfig proto) {
    TemplateLinkConfigForCustomSecretManager pojo = new TemplateLinkConfigForCustomSecretManager();
    pojo.setTemplateRef(proto.getTemplateRef());
    pojo.setVersionLabel(proto.getVersionLabel());

    mapTemplateInputs(proto, pojo);

    return pojo;
  }

  private static void mapTemplateInputs(TemplateLinkConfig proto, TemplateLinkConfigForCustomSecretManager pojo) {
    // Implementation for mapping template inputs
    Map<String, List<NameValuePairWithDefault>> templateInputs = new HashMap<>();
    for (Map.Entry<String, NameValuePairWithDefaultList> entry : proto.getTemplateInputsMap().entrySet()) {
      List<NameValuePairWithDefault> eachMap = new ArrayList<>();
      for (io.harness.delegate.core.beans.NameValuePairWithDefault eachPair : entry.getValue().getValuesList()) {
        eachMap.add(new NameValuePairWithDefault(
            eachPair.getName(), eachPair.getValue(), eachPair.getType(), eachPair.getUseAsDefault()));
      }
      templateInputs.put(entry.getKey(), eachMap);
    }
    pojo.setTemplateInputs(templateInputs);
  }
}
