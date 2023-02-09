/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.bamboo;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.bamboo.BambooValidationParams;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.bamboo.BambooArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.bamboo.BambooArtifactTaskHelper;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.errorhandling.NGErrorHelper;

import com.google.inject.Inject;
import java.util.Collections;

public class BambooValidationHandler implements ConnectorValidationHandler {
  @Inject private BambooArtifactTaskHelper bambooArtifactTaskHelper;
  @Inject private NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final BambooValidationParams bambooValidationParams = (BambooValidationParams) connectorValidationParams;
    ArtifactTaskParameters artifactTaskParameters =
        ArtifactTaskParameters.builder()
            .accountId(accountIdentifier)
            .artifactTaskType(ArtifactTaskType.VALIDATE_ARTIFACT_SERVER)
            .attributes(BambooArtifactDelegateRequest.builder()
                            .bambooConnectorDTO(bambooValidationParams.getBambooConnectorDTO())
                            .encryptedDataDetails(bambooValidationParams.getEncryptionDataDetails())
                            .build())
            .build();
    ArtifactTaskResponse validationResponse =
        bambooArtifactTaskHelper.getArtifactCollectResponse(artifactTaskParameters);
    boolean isJenkinsCredentialsValid = false;
    ConnectorValidationResultBuilder validationResultBuilder = ConnectorValidationResult.builder();
    if (validationResponse.getArtifactTaskExecutionResponse() != null) {
      isJenkinsCredentialsValid = validationResponse.getArtifactTaskExecutionResponse().isArtifactServerValid();
    }
    validationResultBuilder.status(isJenkinsCredentialsValid ? ConnectivityStatus.SUCCESS : ConnectivityStatus.FAILURE);
    if (!isJenkinsCredentialsValid) {
      String errorMessage = validationResponse.getErrorMessage();
      validationResultBuilder.errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
          .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)));
    }
    return validationResultBuilder.build();
  }
}
