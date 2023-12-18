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
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;

import software.wings.beans.CustomSecretNGManagerConfig;

import java.util.List;
import java.util.Map;

public class CustomSmMapper {
  public static EncryptionConfig pojoProtoMapper(CustomSecretNGManagerConfig config) {
    SSHKeySpecDTO specDTO = config.getSshKeySpecDTO();
    SSHAuthDTO authDTO = config.getSshKeySpecDTO().getAuth();
    BaseSSHSpecDTO baseSSHSpecDTO = authDTO.getSpec();
    SSHConfig sshConfig;
    SSHKey.Builder sshKey = SSHKey.newBuilder()
                                .setPort(specDTO.getPort())
                                .setUseSshJ(authDTO.isUseSshj())
                                .setUseSshClient(authDTO.isUseSshClient());
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
    EncryptionConfig.Builder encryptionConfig = EncryptionConfig.newBuilder()
                                                    .setAccountId(config.getAccountId())
                                                    .setIsGlobalKms(config.isGlobalKms())
                                                    .setEncryptionType(CUSTOM_NG)
                                                    .setSecretManagerType(CUSTOM);
    if (null != config.getEncryptionServiceUrl()) {
      encryptionConfig.setEncryptionServiceUrl(config.getEncryptionServiceUrl());
    }
    if (baseSSHSpecDTO instanceof SSHConfigDTO) {
      SSHCredentialSpecDTO sshCredentialSpecDTO = ((SSHConfigDTO) baseSSHSpecDTO).getSpec();
      if (sshCredentialSpecDTO instanceof SSHKeyPathCredentialDTO) {
        SSHKeyPathCredential sshKeyPathCredential =
            SSHKeyPathCredential.newBuilder()
                .setUsername(((SSHKeyPathCredentialDTO) sshCredentialSpecDTO).getUserName())
                .setKeyPath(((SSHKeyPathCredentialDTO) sshCredentialSpecDTO).getKeyPath())
                .setPassPhrase(
                    ((SSHKeyPathCredentialDTO) sshCredentialSpecDTO).getEncryptedPassphrase().toSecretRefStringValue())
                .build();
        sshConfig =
            SSHConfig.newBuilder().setSshCredentialType(KEY_PATH).setKeyPathCredential(sshKeyPathCredential).build();
        sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
      } else if (sshCredentialSpecDTO instanceof SSHKeyReferenceCredentialDTO) {
        SSHKeyReferenceCredential sshKeyReferenceCredential;
        if (null != ((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO).getEncryptedPassphrase()) {
          sshKeyReferenceCredential =
              SSHKeyReferenceCredential.newBuilder()
                  .setUsername(((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO).getUserName())
                  .setKey(((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO).getKey().toSecretRefStringValue())
                  .setPassPhrase(((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO)
                                     .getEncryptedPassphrase()
                                     .toSecretRefStringValue())
                  .build();
        } else {
          sshKeyReferenceCredential =
              SSHKeyReferenceCredential.newBuilder()
                  .setUsername(((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO).getUserName())
                  .setKey(((SSHKeyReferenceCredentialDTO) sshCredentialSpecDTO).getKey().toSecretRefStringValue())
                  .build();
        }
        sshConfig = SSHConfig.newBuilder()
                        .setSshCredentialType(KEY_REFERENCE)
                        .setKeyReferenceCredential(sshKeyReferenceCredential)
                        .build();
        sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
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
      } else {
        SSHPasswordCredential sshPasswordCredential;
        if (null != ((SSHPasswordCredentialDTO) sshCredentialSpecDTO).getPassword()) {
          sshPasswordCredential =
              SSHPasswordCredential.newBuilder()
                  .setUsername(((SSHPasswordCredentialDTO) sshCredentialSpecDTO).getUserName())
                  .setPassword(((SSHPasswordCredentialDTO) sshCredentialSpecDTO).getPassword().toSecretRefStringValue())
                  .build();
        } else {
          sshPasswordCredential = SSHPasswordCredential.newBuilder()
                                      .setUsername(((SSHPasswordCredentialDTO) sshCredentialSpecDTO).getUserName())
                                      .build();
        }
        sshConfig =
            SSHConfig.newBuilder().setSshCredentialType(PASSWORD).setPasswordCredential(sshPasswordCredential).build();
        sshKey.setSshAuthScheme(SSH).setSshConfig(sshConfig);
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
      }
    } else {
      KerberosConfig kerberosConfig;
      if (((KerberosBaseConfigDTO) baseSSHSpecDTO).getSpec() instanceof TGTKeyTabFilePathSpecDTO) {
        TGTKeyTabFilePathSpec tgtKeyTabFilePathSpec =
            TGTKeyTabFilePathSpec.newBuilder()
                .setKeyPath(
                    ((TGTKeyTabFilePathSpecDTO) ((KerberosBaseConfigDTO) baseSSHSpecDTO).getSpec()).getKeyPath())
                .build();
        kerberosConfig = KerberosConfig.newBuilder()
                             .setPrincipal(((KerberosBaseConfigDTO) baseSSHSpecDTO).getPrincipal())
                             .setRealm(((KerberosBaseConfigDTO) baseSSHSpecDTO).getRealm())
                             .setTgtGenerationMethod(KEY_TAB_FILE_PATH)
                             .setTgtTabFilePathSpec(tgtKeyTabFilePathSpec)
                             .build();
        sshKey.setSshAuthScheme(KERBEROS).setKerberosConfig(kerberosConfig);
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
      } else {
        TGTPasswordSpec tgtPasswordSpec =
            TGTPasswordSpec.newBuilder()
                .setPassword(((TGTPasswordSpecDTO) ((KerberosBaseConfigDTO) baseSSHSpecDTO).getSpec())
                                 .getPassword()
                                 .toSecretRefStringValue())
                .build();
        kerberosConfig = KerberosConfig.newBuilder()
                             .setPrincipal(((KerberosBaseConfigDTO) baseSSHSpecDTO).getPrincipal())
                             .setRealm(((KerberosBaseConfigDTO) baseSSHSpecDTO).getRealm())
                             .setTgtGenerationMethod(KerberosConfig.TGTGenerationMethod.PASSWORD)
                             .setTgtPasswordSpec(tgtPasswordSpec)
                             .build();
        sshKey.setSshAuthScheme(KERBEROS).setKerberosConfig(kerberosConfig);
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
      }
    }
    return encryptionConfig.setCustomSecretManagerConfig(protoConfig.setSshKey(sshKey.build())).build();
  }

  public static TemplateLinkConfig mapTemplateInputs(TemplateLinkConfigForCustomSecretManager pojo) {
    TemplateLinkConfig.Builder builder =
        TemplateLinkConfig.newBuilder().setTemplateRef(pojo.getTemplateRef()).setVersionLabel(pojo.getVersionLabel());

    // Iterate over the map and convert values to Proto
    for (Map.Entry<String, List<software.wings.beans.NameValuePairWithDefault>> entry :
        pojo.getTemplateInputs().entrySet()) {
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
    return builder.build();
  }
}
