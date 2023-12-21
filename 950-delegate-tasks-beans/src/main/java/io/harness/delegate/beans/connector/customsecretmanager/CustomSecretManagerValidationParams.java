/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.customsecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class CustomSecretManagerValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  CustomSecretManagerConnectorDTO customSecretManagerConnectorDTO;
  String connectorName;
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (customSecretManagerConnectorDTO != null) {
      populateDelegateSelectorCapability(executionCapabilities, customSecretManagerConnectorDTO.getDelegateSelectors(), getConnectorType());
    }
    return executionCapabilities;
  }

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.CUSTOM_SECRET_MANAGER;
  }
}
