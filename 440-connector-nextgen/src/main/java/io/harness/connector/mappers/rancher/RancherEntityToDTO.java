/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.rancher;

import io.harness.connector.entities.embedded.rancher.RancherClusterConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.rancher.RancherConnectorDTO;

public class RancherEntityToDTO implements ConnectorEntityToDTOMapper<RancherConnectorDTO, RancherClusterConfig> {
  @Override
  public RancherConnectorDTO createConnectorDTO(RancherClusterConfig connector) {
    // TODO (Abhi) create DTO from connector
    return null;
  }
}
