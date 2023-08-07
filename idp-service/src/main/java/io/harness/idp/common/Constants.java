/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.IDP)
public class Constants {
  private Constants() {}

  public static final String IDP_PREFIX = "idp_";
  public static final String IDP_RESOURCE_TYPE = "IDP_SETTINGS";
  public static final String IDP_PERMISSION = "idp_idpsettings_manage";
  public static final List<String> pluginIds = List.of("circleci", "confluence", "firehydrant", "github-actions",
      "github-catalog-discovery", "github-insights", "github-pull-requests", "harness-ci-cd", "harness-feature-flags",
      "jenkins", "jira", "kubernetes", "pager-duty", "todo");
  public static final String GITHUB_TOKEN = "HARNESS_GITHUB_TOKEN";
  public static final String GITHUB_APP_ID = "HARNESS_GITHUB_APP_APPLICATION_ID";
  public static final String GITHUB_APP_PRIVATE_KEY_REF = "HARNESS_GITHUB_APP_PRIVATE_KEY_REF";
  public static final String PRIVATE_KEY_START = "-----BEGIN PRIVATE KEY-----";
  public static final String PRIVATE_KEY_END = "-----END PRIVATE KEY-----";
  public static final String GITLAB_TOKEN = "HARNESS_GITLAB_TOKEN";
  public static final String BITBUCKET_USERNAME = "HARNESS_BITBUCKET_USERNAME";
  public static final String BITBUCKET_USERNAME_API_ACCESS = "HARNESS_BITBUCKET_API_ACCESS_USERNAME";
  public static final String BITBUCKET_TOKEN = "HARNESS_BITBUCKET_TOKEN";
  public static final String BITBUCKET_API_ACCESS_TOKEN = "HARNESS_BITBUCKET_API_ACCESS_TOKEN";
  public static final String AZURE_REPO_TOKEN = "HARNESS_AZURE_REPO_TOKEN";
  public static final String BACKEND_SECRET = "BACKEND_SECRET";
  public static final String IDP_BACKEND_SECRET = "IDP_BACKEND_SECRET";
  public static final String PROXY_ENV_NAME = "HOST_PROXY_MAP";
  public static final String GITHUB_AUTH = "github-auth";
  public static final String GOOGLE_AUTH = "google-auth";
  public static final String AUTH_GITHUB_CLIENT_ID = "AUTH_GITHUB_CLIENT_ID";
  public static final String AUTH_GITHUB_CLIENT_SECRET = "AUTH_GITHUB_CLIENT_SECRET";
  public static final String AUTH_GITHUB_ENTERPRISE_INSTANCE_URL = "AUTH_GITHUB_ENTERPRISE_INSTANCE_URL";
  public static final List<String> GITHUB_AUTH_ENV_VARIABLES =
      new ArrayList<>(List.of(AUTH_GITHUB_CLIENT_ID, AUTH_GITHUB_CLIENT_SECRET, AUTH_GITHUB_ENTERPRISE_INSTANCE_URL));
  public static final String AUTH_GOOGLE_CLIENT_ID = "AUTH_GOOGLE_CLIENT_ID";
  public static final String AUTH_GOOGLE_CLIENT_SECRET = "AUTH_GOOGLE_CLIENT_SECRET";
  public static final List<String> GOOGLE_AUTH_ENV_VARIABLES =
      new ArrayList<>(List.of(AUTH_GOOGLE_CLIENT_ID, AUTH_GOOGLE_CLIENT_SECRET));
  public static final String LAST_UPDATED_TIMESTAMP_FOR_PLUGIN_WITH_NO_CONFIG =
      "LAST_UPDATED_TIMESTAMP_FOR_PLUGIN_WITH_NO_CONFIG";
  public static final String SLASH_DELIMITER = "/";
  public static final String SOURCE_FORMAT = "blob";
  public static final String LAST_UPDATED_TIMESTAMP_FOR_ENV_VARIABLES = "LAST_UPDATED_TIMESTAMP_FOR_ENV_VARIABLES";
  public static final String PLUGIN_REQUEST_NOTIFICATION_SLACK_WEBHOOK = "pluginRequestsNotificationSlack";
}
