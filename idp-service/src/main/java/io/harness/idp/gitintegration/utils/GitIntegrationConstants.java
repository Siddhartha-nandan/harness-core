/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.gitintegration.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.IDP)
public class GitIntegrationConstants {
  public static final String ACCOUNT_SCOPED = "account.";
  public static final String GITHUB_TOKEN = "GITHUB_TOKEN";
  public static final String GITHUB_APP_ID = "GITHUB_APP_APPLICATION_ID";
  public static final String GITHUB_APP_PRIVATE_KEY_REF = "GITHUB_APP_PRIVATE_KEY_REF";
  public static final String GITHUB_CONNECTOR_TYPE = "Github";
  public static final String USERNAME_TOKEN_AUTH_TYPE = "UsernameToken";
  public static final String USERNAME_PASSWORD_AUTH_TYPE = "UsernamePassword";
  public static final String GITHUB_APP_CONNECTOR_TYPE = "GithubApp";
  public static final String GITLAB_TOKEN = "GITLAB_TOKEN";
  public static final String GITLAB_CONNECTOR_TYPE = "Gitlab";
  public static final String BITBUCKET_TOKEN = "BITBUCKET_TOKEN";
  public static final String BITBUCKET_CONNECTOR_TYPE = "Bitbucket";
  public static final String AZURE_REPO_CONNECTOR_TYPE = "AzureRepo";
  public static final String AZURE_REPO_TOKEN = "AZURE_REPO_TOKEN";
  public static final String CATALOG_INFRA_CONNECTOR_TYPE_DIRECT = "DIRECT";
  public static final String CATALOG_INFRA_CONNECTOR_TYPE_PROXY = "PROXY";
  public static final String HARNESS_ENTITIES_IMPORT_COMMIT_MESSAGE = "Importing Harness Entities to IDP";
}
