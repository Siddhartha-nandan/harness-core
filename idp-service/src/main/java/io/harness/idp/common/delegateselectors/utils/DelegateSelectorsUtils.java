/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.common.delegateselectors.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.IDP)
public class DelegateSelectorsUtils {
  public static Set<String> extractDelegateSelectors(ConnectorInfoDTO connectorInfoDTO) {
    Set<String> delegateSelectors = new HashSet<>();
    ConnectorConfigDTO connectorConfig = connectorInfoDTO.getConnectorConfig();
    if (connectorConfig instanceof DelegateSelectable) {
      delegateSelectors = ((DelegateSelectable) connectorConfig).getDelegateSelectors();
    }
    return delegateSelectors;
  }
}
