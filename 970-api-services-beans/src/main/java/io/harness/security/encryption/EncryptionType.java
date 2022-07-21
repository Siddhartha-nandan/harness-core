/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security.encryption;

public enum EncryptionType {
  LOCAL("safeharness"),
  KMS("amazonkms"),
  GCP_KMS("gcpkms"),
  AWS_SECRETS_MANAGER("awssecretsmanager"),
  AZURE_VAULT("azurevault"),
  CYBERARK("cyberark"),
  VAULT("hashicorpvault"),
  GCP_SECRETS_MANAGER("gcpsecretsmanager"),
  CUSTOM("custom"),
  CUSTOM_NG("customNG"),
  VAULT_SSH("vaultssh");

  private final String yamlName;

  EncryptionType(String yamlName) {
    this.yamlName = yamlName;
  }

  public String getYamlName() {
    return yamlName;
  }
}
