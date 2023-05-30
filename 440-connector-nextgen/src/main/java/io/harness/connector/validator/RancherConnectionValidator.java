/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.scmValidators.AbstractKubernetesConnectorValidator;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.rancher.RancherConnectorDTO;
import io.harness.delegate.beans.connector.rancher.RancherTaskParams;
import io.harness.delegate.beans.connector.rancher.RancherTestConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;

import java.util.List;

@OwnedBy(HarnessTeam.CDP)
public class RancherConnectionValidator extends AbstractKubernetesConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    RancherConnectorDTO rancherConnectorDTO = (RancherConnectorDTO) connectorConfig;
    List<EncryptedDataDetail> encryptedDataDetails =
        super.fetchEncryptionDetailsList(rancherConnectorDTO, accountIdentifier, orgIdentifier, projectIdentifier);

    return RancherTaskParams.builder()
        .rancherConnectorDTO(rancherConnectorDTO)
        .encryptionDetails(encryptedDataDetails)
        .build();
  }

  @Override
  public String getTaskType() {
    return TaskType.RANCHER_TEST_CONNECTION_TASK_NG.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    DelegateResponseData responseData =
        super.validateConnector(connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    RancherTestConnectionTaskResponse taskResponse = (RancherTestConnectionTaskResponse) responseData;
    return taskResponse.getConnectorValidationResult();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }
}
