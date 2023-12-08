/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.encryption.Encrypted;
import io.harness.encryption.SecretRefData;
import io.harness.secret.SecretReference;

import software.wings.annotation.EncryptableSetting;
import software.wings.helpers.ext.ldap.LdapConnectionConfig;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

/**
 * Class denotes Connection settings for Ldap.
 */
@OwnedBy(PL)
@JsonIgnoreProperties(value = {"encryptedBindPassword", "encryptedBindSecret"})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class LdapConnectionSettings implements LdapConnectionConfig, EncryptableSetting {
  public static final String INLINE_SECRET = "INLINE";
  public static final String SECRET = "ENCRYPTED_SECRET";
  public static final String NG_SECRET = "NG_ENCRYPTED_SECRET";
  public static final String BOTH_CG_NG_SECRET = "BOTH_ENCRYPTED_SECRET";
  @NotNull String host;
  int port = LdapConstants.DEFAULT_CONNECTION_PORT;
  boolean sslEnabled = LdapConstants.DEFAULT_SSL_STATE;
  boolean referralsEnabled;
  @Min(1) int maxReferralHops = LdapConstants.DEFAULT_MAX_REFERRAL_HOPS;
  String bindDN = "";
  String bindPassword = "";
  String encryptedBindPassword;
  @Attributes(title = "Bind Password Type", enums = {INLINE_SECRET, SECRET, NG_SECRET, BOTH_CG_NG_SECRET}) String passwordType;
  @Encrypted(fieldName = "bindSecret") private char[] bindSecret;
  @JsonIgnore @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedBindSecret;

  // store ng secret ref here, example: 'account.ldap_secret'
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData ngBindSecret;

  int connectTimeout = LdapConstants.DEFAULT_CONNECT_TIMEOUT;
  int responseTimeout = LdapConstants.DEFAULT_RESPONSE_TIMEOUT;
  Boolean useRecursiveGroupMembershipSearch;
  private Set<String> delegateSelectors;

  @AssertTrue(message = "Bind password/Secret can't be empty if Bind DN is provided.")
  private boolean isNonEmptyCredentials() {
    if (StringUtils.isNotBlank(bindDN)) {
      return StringUtils.isNotBlank(bindPassword)
          || (ngBindSecret != null && StringUtils.isNotBlank(ngBindSecret.getIdentifier()))
          || (bindSecret != null && StringUtils.isNotBlank(String.valueOf(bindSecret)));
    }
    return StringUtils.isBlank(bindPassword)
        || (ngBindSecret != null && StringUtils.isBlank(ngBindSecret.getIdentifier()))
        || (bindSecret != null && StringUtils.isBlank(String.valueOf(bindSecret)));
  }

  /**
   * API to fetch ldap server url based on given host and port.
   * @return
   */
  @Override
  public String generateUrl() {
    return String.format("%s://%s:%d", sslEnabled ? "ldaps" : "ldap", host, port);
  }

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.LDAP;
  }

  public String fetchPassword() {
    if (encryptedBindPassword != null) {
      return encryptedBindPassword;
    } else {
      return encryptedBindSecret;
    }
  }

  public String fetchPasswordNG() {
    if (encryptedBindPassword != null) {
      return encryptedBindPassword;
    } else if (ngBindSecret != null && isNotEmpty(ngBindSecret.getIdentifier())) {
      return ngBindSecret.getIdentifier();
    } else {
      return encryptedBindSecret;
    }
  }

  @Override
  public String getAccountId() {
    return null;
  }

  @Override
  public void setAccountId(String accountId) {}
}
