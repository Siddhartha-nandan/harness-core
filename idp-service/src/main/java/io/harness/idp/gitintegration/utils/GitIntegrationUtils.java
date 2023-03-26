/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.gitintegration.utils;

import io.harness.beans.DecryptedSecretValue;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GitIntegrationUtils {
  private static final String PATH_SEPARATOR_FOR_URL = "/";

  public BackstageEnvSecretVariable getBackstageEnvSecretVariable(String tokenSecretIdentifier, String tokenType) {
    BackstageEnvSecretVariable environmentSecret = new BackstageEnvSecretVariable();
    environmentSecret.harnessSecretIdentifier(tokenSecretIdentifier);
    environmentSecret.setEnvName(tokenType);
    return environmentSecret;
  }

  public String decryptSecret(SecretManagerClientService ngSecretService, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String tokenSecretIdentifier, String connectorIdentifier) {
    DecryptedSecretValue decryptedSecretValue = ngSecretService.getDecryptedSecretValue(
        accountIdentifier, orgIdentifier, projectIdentifier, tokenSecretIdentifier);
    if (decryptedSecretValue == null) {
      throw new InvalidRequestException(String.format(
          "Secret not found for identifier : [%s], accountId: [%s]", connectorIdentifier, accountIdentifier));
    }
    return decryptedSecretValue.getDecryptedValue();
  }

  public String getHostForConnector(ConnectorInfoDTO connectorInfoDTO, ConnectorType connectorType) {
    switch (connectorType) {
      case GITHUB:
        GithubConnectorDTO configGithub = (GithubConnectorDTO) connectorInfoDTO.getConnectorConfig();
        return getHostFromURL(configGithub.getUrl());
      case GITLAB:
        GitlabConnectorDTO configGitlab = (GitlabConnectorDTO) connectorInfoDTO.getConnectorConfig();
        return getHostFromURL(configGitlab.getUrl());
      case BITBUCKET:
        BitbucketConnectorDTO configBitbucket = (BitbucketConnectorDTO) connectorInfoDTO.getConnectorConfig();
        return getHostFromURL(configBitbucket.getUrl());
      case AZURE_REPO:
        AzureRepoConnectorDTO configAzure = (AzureRepoConnectorDTO) connectorInfoDTO.getConnectorConfig();
        return getHostFromURL(configAzure.getUrl());
      default:
        return null;
    }
  }

  private String getHostFromURL(String url) {
    String[] splitURL = url.split(PATH_SEPARATOR_FOR_URL);
    return splitURL[2];
  }

  public boolean checkIfGithubAppConnector(ConnectorInfoDTO connectorInfoDTO) {
    GithubConnectorDTO config = (GithubConnectorDTO) connectorInfoDTO.getConnectorConfig();
    GithubApiAccessDTO apiAccess = config.getApiAccess();
    return (apiAccess != null
               && apiAccess.getType().toString().equals(GitIntegrationConstants.GITHUB_APP_CONNECTOR_TYPE))
        ? true
        : false;
  }
}
