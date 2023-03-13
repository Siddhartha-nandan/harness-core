/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.gitintegration.implementation;

import static io.harness.idp.gitintegration.utils.GitIntegrationConstants.CATALOG_INFRA_CONNECTOR_TYPE_DIRECT;
import static io.harness.idp.gitintegration.utils.GitIntegrationConstants.CATALOG_INFRA_CONNECTOR_TYPE_PROXY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.outcome.BitbucketHttpCredentialsOutcomeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.gitintegration.GitIntegrationUtil;
import io.harness.idp.gitintegration.baseclass.ConnectorProcessor;
import io.harness.idp.gitintegration.utils.GitIntegrationConstants;
import io.harness.remote.client.NGRestUtils;
import io.harness.spec.server.idp.v1.model.CatalogConnectorInfo;
import io.harness.spec.server.idp.v1.model.EnvironmentSecret;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.util.Pair;

@OwnedBy(HarnessTeam.IDP)
public class BitbucketConnectorProcessor extends ConnectorProcessor {
  @Override
  public String getInfraConnectorType(String accountIdentifier, String connectorIdentifier) {
    Optional<ConnectorDTO> connectorDTO =
        NGRestUtils.getResponse(connectorResourceClient.get(connectorIdentifier, accountIdentifier, null, null));
    if (connectorDTO.isEmpty()) {
      throw new InvalidRequestException(String.format(
          "Connector not found for identifier: [%s], accountId: [%s]", connectorIdentifier, accountIdentifier));
    }

    ConnectorInfoDTO connectorInfoDTO = connectorDTO.get().getConnectorInfo();
    BitbucketConnectorDTO config = (BitbucketConnectorDTO) connectorInfoDTO.getConnectorConfig();
    return config.getExecuteOnDelegate() ? CATALOG_INFRA_CONNECTOR_TYPE_PROXY : CATALOG_INFRA_CONNECTOR_TYPE_DIRECT;
  }

  @Override
  public Pair<ConnectorInfoDTO, List<EnvironmentSecret>> getConnectorSecretsInfo(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Optional<ConnectorDTO> connectorDTO = NGRestUtils.getResponse(
        connectorResourceClient.get(connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier));
    List<EnvironmentSecret> resultList = new ArrayList<>();

    if (connectorDTO.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Bitbucket Connector not found for identifier: [%s], accountId: [%s]", connectorIdentifier,
              accountIdentifier));
    }

    ConnectorInfoDTO connectorInfoDTO = connectorDTO.get().getConnectorInfo();
    if (!connectorInfoDTO.getConnectorType().toString().equals(GitIntegrationConstants.BITBUCKET_CONNECTOR_TYPE)) {
      throw new InvalidRequestException(
          String.format("Connector with id - [%s] is not bitbucket connector for accountId: [%s]", connectorIdentifier,
              accountIdentifier));
    }

    BitbucketConnectorDTO config = (BitbucketConnectorDTO) connectorInfoDTO.getConnectorConfig();
    BitbucketHttpCredentialsOutcomeDTO outcome =
        (BitbucketHttpCredentialsOutcomeDTO) config.getAuthentication().getCredentials().toOutcome();
    if (!outcome.getType().toString().equals(GitIntegrationConstants.USERNAME_PASSWORD_AUTH_TYPE)) {
      throw new InvalidRequestException(String.format(
          " Authentication is not Username and Password for Bitbucket Connector with id - [%s], accountId: [%s]",
          connectorIdentifier, accountIdentifier));
    }

    BitbucketUsernamePasswordDTO spec = (BitbucketUsernamePasswordDTO) outcome.getSpec();
    String pwdSecretIdentifier = spec.getPasswordRef().getIdentifier();
    if (pwdSecretIdentifier.isEmpty()) {
      throw new InvalidRequestException(String.format(
          "Secret identifier not found for connector: [%s], accountId: [%s]", connectorIdentifier, accountIdentifier));
    }

    resultList.add(GitIntegrationUtil.getEnvironmentSecret(ngSecretService, accountIdentifier, orgIdentifier,
        projectIdentifier, pwdSecretIdentifier, connectorIdentifier, GitIntegrationConstants.BITBUCKET_TOKEN));
    return new Pair<>(connectorInfoDTO, resultList);
  }

  @Override
  public void performPushOperation(
      String accountIdentifier, CatalogConnectorInfo catalogConnectorInfo, List<String> locationToPush) {}
}
