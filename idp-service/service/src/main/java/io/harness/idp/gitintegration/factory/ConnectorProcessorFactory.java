/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.gitintegration.factory;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.idp.gitintegration.baseclass.ConnectorProcessor;
import io.harness.idp.gitintegration.implementation.GithubConnectorProcessor;

import javax.inject.Inject;

public class ConnectorProcessorFactory {
  @Inject GithubConnectorProcessor githubConnectorProcessor;

  public ConnectorProcessor getConnectorProcessor(ConnectorType connectorType) {
    if (connectorType == null) {
      return null;
    }

    switch (connectorType) {
      case GITHUB:
        return githubConnectorProcessor;
      default:
        throw new UnsupportedOperationException("Invalid Connector type for git integrations");
    }
  }
}
