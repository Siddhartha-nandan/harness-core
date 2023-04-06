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
  public static final String GITHUB_CONNECTOR_TYPE = "Github";
  public static final String USERNAME_TOKEN_AUTH_TYPE = "UsernameToken";
  public static final String USERNAME_PASSWORD_AUTH_TYPE = "UsernamePassword";
  public static final String GITHUB_APP_CONNECTOR_TYPE = "GithubApp";
  public static final String GITLAB_CONNECTOR_TYPE = "Gitlab";
  public static final String BITBUCKET_CONNECTOR_TYPE = "Bitbucket";
  public static final String AZURE_REPO_CONNECTOR_TYPE = "AzureRepo";
  public static final String CATALOG_INFRA_CONNECTOR_TYPE_DIRECT = "DIRECT";
  public static final String CATALOG_INFRA_CONNECTOR_TYPE_PROXY = "PROXY";
  public static final String TMP_LOCATION_FOR_GIT_CLONE = "/tmp/git_repos/";
  public static final String HARNESS_ENTITIES_IMPORT_AUTHOR_EMAIL = "idp-harness@harness.io";
  public static final String HARNESS_ENTITIES_IMPORT_COMMIT_MESSAGE = "Importing Harness Entities to IDP";
}
