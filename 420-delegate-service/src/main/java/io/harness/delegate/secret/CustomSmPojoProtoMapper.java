/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.secret;

import static io.harness.delegate.core.beans.EncryptionType.CUSTOM_NG;
import static io.harness.delegate.core.beans.KerberosConfig.TGTGenerationMethod.KEY_TAB_FILE_PATH;
import static io.harness.delegate.core.beans.SSHConfig.SSHCredentialType.KEY_PATH;
import static io.harness.delegate.core.beans.SSHConfig.SSHCredentialType.KEY_REFERENCE;
import static io.harness.delegate.core.beans.SSHConfig.SSHCredentialType.PASSWORD;
import static io.harness.delegate.core.beans.SSHKey.SSHAuthScheme.KERBEROS;
import static io.harness.delegate.core.beans.SSHKey.SSHAuthScheme.SSH;
import static io.harness.delegate.core.beans.SecretManagerType.CUSTOM;

import io.harness.delegate.beans.connector.customsecretmanager.TemplateLinkConfigForCustomSecretManager;
import io.harness.delegate.core.beans.EncryptionConfig;
import io.harness.delegate.core.beans.KerberosConfig;
import io.harness.delegate.core.beans.NameValuePairWithDefault;
import io.harness.delegate.core.beans.NameValuePairWithDefaultList;
import io.harness.delegate.core.beans.SSHConfig;
import io.harness.delegate.core.beans.SSHKey;
import io.harness.delegate.core.beans.SSHKeyPathCredential;
import io.harness.delegate.core.beans.SSHKeyReferenceCredential;
import io.harness.delegate.core.beans.SSHPasswordCredential;
import io.harness.delegate.core.beans.SecretDetail;
import io.harness.delegate.core.beans.TGTKeyTabFilePathSpec;
import io.harness.delegate.core.beans.TGTPasswordSpec;
import io.harness.delegate.core.beans.TemplateLinkConfig;
import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;
import io.harness.ng.core.dto.secrets.KerberosBaseConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.TGTGenerationSpecDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;

import software.wings.beans.CustomSecretNGManagerConfig;

import java.util.List;
import java.util.Map;

public class CustomSmPojoProtoMapper {
  public static EncryptionConfig map(CustomSecretNGManagerConfig config) {
    SSHKey.Builder sshKey = createSSHKey(config.getSshKeySpecDTO());
    io.harness.delegate.core.beans.CustomSecretNGManagerConfig.Builder protoConfig = createProtoConfig(config);

    return createEncryptionConfig(config, sshKey, protoConfig);
  }

  private static SSHKey.Builder createSSHKey(SSHKeySpecDTO specDTO) {
    SSHAuthDTO authDTO = specDTO.getAuth();
    BaseSSHSpecDTO baseSSHSpecDTO = authDTO.getSpec();

    SSHKey.Builder sshKey = SSHKey.newBuilder()
                                .setPort(specDTO.getPort())
                                .setUseSshJ(authDTO.isUseSshj())
                                .setUseSshClient(authDTO.isUseSshClient());

    if (baseSSHSpecDTO instanceof SSHConfigDTO) {
      handleSSHConfigDTO((SSHConfigDTO) baseSSHSpecDTO, sshKey);
    } else {
      handleKerberosBaseConfigDTO((KerberosBaseConfigDTO) baseSSHSpecDTO, sshKey);
    }

    return sshKey;
  }

  private static void handleSSHConfigDTO(SSHConfigDTO sshConfigDTO, SSHKey.Builder sshKey) {
    SSHCredentialSpecDTO sshCredentialSpecDTO = sshConfigDTO.getSpec();

    if (sshCredentialSpecDTO instanceof SSHKeyPathCredentialDTO) {
      handleSSHKeyPathCredential((SSHKeyPathCredentialDTO) sshCredentialSpecDTO, sshKey);
    } else if (sshCredentialSpecDTO instanceof SSHKeyReferenceCredentialDTO) {
      handleSSHKeyReferenceCredential((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO, sshKey);
    } else {
      handleSSHPasswordCredential((SSHPasswordCredentialDTO) sshCredentialSpecDTO, sshKey);
    }
  }

  private static void handleSSHKeyPathCredential(SSHKeyPathCredentialDTO credentialDTO, SSHKey.Builder sshKey) {
    // Implementation for SSHKeyPathCredential handling
    SSHKeyPathCredential.Builder sshKeyPathCredential = SSHKeyPathCredential.newBuilder()
                                                            .setUsername(credentialDTO.getUserName())
                                                            .setKeyPath(credentialDTO.getKeyPath());
    if (null != credentialDTO.getEncryptedPassphrase()) {
      sshKeyPathCredential.setPassPhrase(credentialDTO.getEncryptedPassphrase().toSecretRefStringValue());
    }
    SSHConfig sshConfig = SSHConfig.newBuilder()
                              .setSshCredentialType(KEY_PATH)
                              .setKeyPathCredential(sshKeyPathCredential.build())
                              .build();
    sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
  }

  private static void handleSSHKeyReferenceCredential(
      SSHKeyReferenceCredentialDTO credentialDTO, SSHKey.Builder sshKey) {
    // Implementation for SSHKeyReferenceCredential handling
    SSHKeyReferenceCredential.Builder sshKeyReferenceCredential =
        SSHKeyReferenceCredential.newBuilder()
            .setUsername(credentialDTO.getUserName())
            .setKey(credentialDTO.getKey().toSecretRefStringValue());
    if (null != credentialDTO.getEncryptedPassphrase()) {
      sshKeyReferenceCredential.setPassPhrase(credentialDTO.getEncryptedPassphrase().toSecretRefStringValue());
    }
    SSHConfig sshConfig = SSHConfig.newBuilder()
                              .setSshCredentialType(KEY_REFERENCE)
                              .setKeyReferenceCredential(sshKeyReferenceCredential.build())
                              .build();
    sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
  }

  private static void handleSSHPasswordCredential(SSHPasswordCredentialDTO credentialDTO, SSHKey.Builder sshKey) {
    // Implementation for SSHPasswordCredential handling
    SSHPasswordCredential sshPasswordCredential = SSHPasswordCredential.newBuilder()
                                                      .setUsername(credentialDTO.getUserName())
                                                      .setPassword(credentialDTO.getPassword().toSecretRefStringValue())
                                                      .build();
    SSHConfig sshConfig =
        SSHConfig.newBuilder().setSshCredentialType(PASSWORD).setPasswordCredential(sshPasswordCredential).build();
    sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
  }

  private static void handleKerberosBaseConfigDTO(KerberosBaseConfigDTO kerberosBaseConfigDTO, SSHKey.Builder sshKey) {
    // Implementation for KerberosBaseConfigDTO handling
    KerberosConfig.Builder kerberosConfig = KerberosConfig.newBuilder()
                                                .setPrincipal(kerberosBaseConfigDTO.getPrincipal())
                                                .setRealm(kerberosBaseConfigDTO.getRealm());
    TGTGenerationSpecDTO tgtGenerationSpecDTO = kerberosBaseConfigDTO.getSpec();

    if (tgtGenerationSpecDTO instanceof TGTKeyTabFilePathSpecDTO) {
      handleTgtKeyTabFilePath((TGTKeyTabFilePathSpecDTO) tgtGenerationSpecDTO, sshKey, kerberosConfig);
    } else {
      handleTgtPasswordSpec((TGTPasswordSpecDTO) tgtGenerationSpecDTO, sshKey, kerberosConfig);
    }
  }

  private static void handleTgtKeyTabFilePath(
      TGTKeyTabFilePathSpecDTO credentialDTO, SSHKey.Builder sshKey, KerberosConfig.Builder kerberosConfig) {
    // Implementation for TGTKeyTabFilePathSpecDTO handling
    kerberosConfig.setTgtGenerationMethod(KEY_TAB_FILE_PATH)
        .setTgtTabFilePathSpec(TGTKeyTabFilePathSpec.newBuilder().setKeyPath(credentialDTO.getKeyPath()).build())
        .build();
    sshKey.setSshAuthScheme(KERBEROS).setKerberosConfig(kerberosConfig);
  }

  private static void handleTgtPasswordSpec(
      TGTPasswordSpecDTO credentialDTO, SSHKey.Builder sshKey, KerberosConfig.Builder kerberosConfig) {
    // Implementation for TGTPasswordSpecDTO handling
    kerberosConfig.setTgtGenerationMethod(KerberosConfig.TGTGenerationMethod.PASSWORD)
        .setTgtPasswordSpec(
            TGTPasswordSpec.newBuilder().setPassword(credentialDTO.getPassword().toSecretRefStringValue()).build())
        .build();
    sshKey.setSshAuthScheme(KERBEROS).setKerberosConfig(kerberosConfig);
  }

  private static io.harness.delegate.core.beans.CustomSecretNGManagerConfig.Builder createProtoConfig(
      CustomSecretNGManagerConfig config) {
    // Implementation for creating proto config
    io.harness.delegate.core.beans.CustomSecretNGManagerConfig.Builder protoConfig =
        io.harness.delegate.core.beans.CustomSecretNGManagerConfig.newBuilder()
            .setScript(config.getScript())
            .setIsOnDelegate(config.isOnDelegate())
            .setWorkingDirectory(config.getWorkingDirectory())
            .setHost(config.getHost())
            .setConnectorRef(config.getConnectorRef());
    if (null != config.getTemplate()) {
      protoConfig = protoConfig.setTemplateLinkConfig(mapTemplateInputs(config.getTemplate()));
    }
    SecretDetail encryptionDetails;
    if (null != config.getSshKeyEncryptionDetails()) {
      encryptionDetails = SecretDetail.newBuilder()
                              .setEncryptedData(EncryptedDataRecordPojoProtoMapper.INSTANCE.map(
                                  config.getSshKeyEncryptionDetails().get(0).getEncryptedData()))
                              .setConfig(EncryptionConfigPojoProtoMapper.INSTANCE.map(
                                  config.getSshKeyEncryptionDetails().get(0).getEncryptionConfig()))
                              .build();
    } else {
      encryptionDetails = SecretDetail.newBuilder().build();
    }
    protoConfig.addSshEncryptionDetails(0, encryptionDetails);
    return protoConfig;
  }

  private static EncryptionConfig createEncryptionConfig(CustomSecretNGManagerConfig config, SSHKey.Builder sshKey,
      io.harness.delegate.core.beans.CustomSecretNGManagerConfig.Builder protoConfig) {
    // Implementation for creating encryption config
    EncryptionConfig.Builder encryptionConfig = EncryptionConfig.newBuilder()
                                                    .setAccountId(config.getAccountId())
                                                    .setIsGlobalKms(config.isGlobalKms())
                                                    .setEncryptionType(CUSTOM_NG)
                                                    .setSecretManagerType(CUSTOM);
    if (null != config.getEncryptionServiceUrl()) {
      encryptionConfig.setEncryptionServiceUrl(config.getEncryptionServiceUrl());
    }
    return encryptionConfig.setCustomSecretManagerConfig(protoConfig.setSshKey(sshKey.build())).build();
  }

  public static TemplateLinkConfig mapTemplateInputs(TemplateLinkConfigForCustomSecretManager pojo) {
    TemplateLinkConfig.Builder builder =
        TemplateLinkConfig.newBuilder().setTemplateRef(pojo.getTemplateRef()).setVersionLabel(pojo.getVersionLabel());

    mapTemplateInputs(pojo.getTemplateInputs(), builder);

    return builder.build();
  }

  private static void mapTemplateInputs(Map<String, List<software.wings.beans.NameValuePairWithDefault>> templateInputs,
      TemplateLinkConfig.Builder builder) {
    // Implementation for mapping template inputs
    for (Map.Entry<String, List<software.wings.beans.NameValuePairWithDefault>> entry : templateInputs.entrySet()) {
      NameValuePairWithDefaultList.Builder listBuilder = NameValuePairWithDefaultList.newBuilder();
      for (software.wings.beans.NameValuePairWithDefault val : entry.getValue()) {
        listBuilder.addValues(NameValuePairWithDefault.newBuilder()
                                  .setName(val.getName())
                                  .setValue(val.getValue())
                                  .setType(val.getType())
                                  .setUseAsDefault(null != val.getUseAsDefault() && val.getUseAsDefault())
                                  .build());
      }
      builder.putTemplateInputs(entry.getKey(), listBuilder.build());
    }
  }
}
