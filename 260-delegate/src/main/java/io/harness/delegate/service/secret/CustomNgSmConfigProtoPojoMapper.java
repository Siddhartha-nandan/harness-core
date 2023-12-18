/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.secret;

import static io.harness.delegate.core.beans.KerberosConfig.TGTGenerationMethod.KEY_TAB_FILE_PATH;
import static io.harness.ng.core.dto.secrets.TGTGenerationMethod.KeyTabFilePath;
import static io.harness.ng.core.dto.secrets.TGTGenerationMethod.Password;

import io.harness.delegate.core.beans.SSHConfig;
import io.harness.delegate.core.beans.SSHKey;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.*;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;

import java.util.ArrayList;
import java.util.List;

public class CustomNgSmConfigProtoPojoMapper {
  public static EncryptionConfig protoPojoMapper(io.harness.delegate.core.beans.EncryptionConfig config) {
    software.wings.beans.CustomSecretNGManagerConfig customSecretNGManagerConfig =
        new software.wings.beans.CustomSecretNGManagerConfig();
    customSecretNGManagerConfig.setAccountId(config.getAccountId());
    customSecretNGManagerConfig.setUuid(config.getUuid());
    customSecretNGManagerConfig.setEncryptionType(EncryptionType.CUSTOM);
    customSecretNGManagerConfig.setScript(config.getCustomSecretManagerConfig().getScript());
    customSecretNGManagerConfig.setOnDelegate(config.getCustomSecretManagerConfig().getIsOnDelegate());
    customSecretNGManagerConfig.setWorkingDirectory(config.getCustomSecretManagerConfig().getWorkingDirectory());
    customSecretNGManagerConfig.setHost(config.getCustomSecretManagerConfig().getHost());
    customSecretNGManagerConfig.setConnectorRef(config.getCustomSecretManagerConfig().getConnectorRef());
    EncryptedDataDetail encryptedDataDetail = new EncryptedDataDetail();
    encryptedDataDetail.setEncryptedData(EncryptedDataRecordProtoPojoMapper.map(
        config.getCustomSecretManagerConfig().getSshEncryptionDetails(0).getEncryptedData()));
    encryptedDataDetail.setEncryptionConfig(EncryptionConfigProtoPojoMapper.INSTANCE.map(
        config.getCustomSecretManagerConfig().getSshEncryptionDetails(0).getConfig()));
    encryptedDataDetail.setFieldName("key");
    List<EncryptedDataDetail> sshEncryptionDetails = new ArrayList<>();
    sshEncryptionDetails.add(encryptedDataDetail);
    customSecretNGManagerConfig.setSshKeyEncryptionDetails(sshEncryptionDetails);
    if ((config.getCustomSecretManagerConfig().getSshKey().getSshAuthScheme()) == SSHKey.SSHAuthScheme.SSH) {
      SSHAuthDTO sshAuthDTO = new SSHAuthDTO();
      sshAuthDTO.setAuthScheme(SSHAuthScheme.SSH);
      SSHConfigDTO sshConfigDTO = new SSHConfigDTO();
      if ((config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getSshCredentialType())
          == SSHConfig.SSHCredentialType.PASSWORD) {
        // handle password
        SSHPasswordCredentialDTO sshPasswordCredentialDTO =
            SSHPasswordCredentialDTO.builder()
                .userName(config.getCustomSecretManagerConfig()
                              .getSshKey()
                              .getSshConfig()
                              .getPasswordCredential()
                              .getUsername())
                .password(new SecretRefData(config.getCustomSecretManagerConfig()
                                                .getSshKey()
                                                .getSshConfig()
                                                .getPasswordCredential()
                                                .getPassword()))
                .build();
        sshConfigDTO.setCredentialType(SSHCredentialType.Password);
        sshConfigDTO.setSpec(sshPasswordCredentialDTO);

      } else if ((config.getCustomSecretManagerConfig().getSshKey().getSshConfig().getSshCredentialType())
          == SSHConfig.SSHCredentialType.KEY_PATH) {
        // handle key_path
        SSHKeyPathCredentialDTO sshKeyPathCredentialDTO =
            SSHKeyPathCredentialDTO.builder()
                .userName(config.getCustomSecretManagerConfig()
                              .getSshKey()
                              .getSshConfig()
                              .getKeyPathCredential()
                              .getUsername())
                .keyPath(config.getCustomSecretManagerConfig()
                             .getSshKey()
                             .getSshConfig()
                             .getKeyPathCredential()
                             .getKeyPath())
                .encryptedPassphrase(new SecretRefData(config.getCustomSecretManagerConfig()
                                                           .getSshKey()
                                                           .getSshConfig()
                                                           .getKeyPathCredential()
                                                           .getPassPhrase()))
                .build();
        sshConfigDTO.setCredentialType(SSHCredentialType.KeyPath);
        sshConfigDTO.setSpec(sshKeyPathCredentialDTO);
      } else {
        // handle key_reference
        SSHKeyReferenceCredentialDTO sshKeyReferenceCredentialDTO =
            SSHKeyReferenceCredentialDTO.builder()
                .userName(config.getCustomSecretManagerConfig()
                              .getSshKey()
                              .getSshConfig()
                              .getKeyReferenceCredential()
                              .getUsername())
                .key(new SecretRefData(config.getCustomSecretManagerConfig()
                                           .getSshKey()
                                           .getSshConfig()
                                           .getKeyReferenceCredential()
                                           .getKey()))
                .encryptedPassphrase(new SecretRefData(config.getCustomSecretManagerConfig()
                                                           .getSshKey()
                                                           .getSshConfig()
                                                           .getKeyReferenceCredential()
                                                           .getPassPhrase()))
                .build();
        sshConfigDTO.setCredentialType(SSHCredentialType.KeyReference);
        sshConfigDTO.setSpec(sshKeyReferenceCredentialDTO);
      }
      sshAuthDTO.setSpec(sshConfigDTO);
      sshAuthDTO.setUseSshClient(config.getCustomSecretManagerConfig().getSshKey().getUseSshClient());
      sshAuthDTO.setUseSshj(config.getCustomSecretManagerConfig().getSshKey().getUseSshJ());
      SSHKeySpecDTO sshKeySpecDTO =
          new SSHKeySpecDTO(config.getCustomSecretManagerConfig().getSshKey().getPort(), sshAuthDTO);
      customSecretNGManagerConfig.setSshKeySpecDTO(sshKeySpecDTO);
    } else {
      // KERBEROS
      SSHAuthDTO sshAuthDTO = new SSHAuthDTO();
      sshAuthDTO.setAuthScheme(SSHAuthScheme.Kerberos);
      KerberosConfigDTO kerberosConfigDTO = new KerberosConfigDTO();
      if (config.getCustomSecretManagerConfig().getSshKey().getKerberosConfig().getTgtGenerationMethod()
          == KEY_TAB_FILE_PATH) {
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
        sshAuthDTO.setSpec(kerberosConfigDTO);
        sshAuthDTO.setUseSshClient(config.getCustomSecretManagerConfig().getSshKey().getUseSshClient());
        sshAuthDTO.setUseSshj(config.getCustomSecretManagerConfig().getSshKey().getUseSshJ());
        SSHKeySpecDTO sshKeySpecDTO =
            new SSHKeySpecDTO(config.getCustomSecretManagerConfig().getSshKey().getPort(), sshAuthDTO);
        customSecretNGManagerConfig.setSshKeySpecDTO(sshKeySpecDTO);
      } else {
        TGTPasswordSpecDTO tgtPasswordSpecDTO = TGTPasswordSpecDTO.builder()
                                                    .password(new SecretRefData(config.getCustomSecretManagerConfig()
                                                                                    .getSshKey()
                                                                                    .getKerberosConfig()
                                                                                    .getTgtPasswordSpec()
                                                                                    .getPassword()))
                                                    .build();
        kerberosConfigDTO.setTgtGenerationMethod(Password);
        kerberosConfigDTO.setSpec(tgtPasswordSpecDTO);
        sshAuthDTO.setSpec(kerberosConfigDTO);
        sshAuthDTO.setUseSshClient(config.getCustomSecretManagerConfig().getSshKey().getUseSshClient());
        sshAuthDTO.setUseSshj(config.getCustomSecretManagerConfig().getSshKey().getUseSshJ());
        SSHKeySpecDTO sshKeySpecDTO =
            new SSHKeySpecDTO(config.getCustomSecretManagerConfig().getSshKey().getPort(), sshAuthDTO);
        customSecretNGManagerConfig.setSshKeySpecDTO(sshKeySpecDTO);
      }
      return customSecretNGManagerConfig;
    }
    return customSecretNGManagerConfig;
  }
}
