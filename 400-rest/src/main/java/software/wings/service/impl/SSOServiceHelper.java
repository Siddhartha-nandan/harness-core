package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.sso.LdapAuthType;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
@OwnedBy(HarnessTeam.PL)
public class SSOServiceHelper {
  @Inject private EncryptionService encryptionService;

  public void encryptLdapSecret(
      LdapConnectionSettings connectionSettings, SecretManager secretManager, String accountId) {
    LdapAuthType existingPasswordType = connectionSettings.getPasswordType();
    if (isNotEmpty(connectionSettings.getBindSecret())) {
      if ((isNotEmpty(connectionSettings.getBindPassword()))
          && (!connectionSettings.getBindPassword().equals(LdapConstants.MASKED_STRING))) {
        throw new InvalidRequestException("Either Enter password or select a secret");
      }
      connectionSettings.setPasswordType(LdapAuthType.SECRET_REF);
      connectionSettings.setEncryptedBindSecret(String.valueOf(connectionSettings.getBindSecret()));
      connectionSettings.setBindSecret(null);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) connectionSettings, null, null);
      encryptionService.decrypt(connectionSettings, encryptionDetails, false);
      if (existingPasswordType.equals(LdapAuthType.INLINE_SECRET)) {
        // If the LDAP Setting was already used with Inline Password, and now when they have choosed Secret, hence
        // deleting the orphan secret
        String oldEncryptedBindPassword = connectionSettings.getEncryptedBindPassword();
        connectionSettings.setBindPassword(LdapConstants.MASKED_STRING);
        if (isNotEmpty(oldEncryptedBindPassword)) {
          secretManager.deleteSecret(accountId, oldEncryptedBindPassword, new HashMap<>(), false);
          connectionSettings.setEncryptedBindPassword(null);
        }
      }
    }
  }
}
