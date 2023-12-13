/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.gcp.connector;

import static io.harness.oidc.gcp.constants.GcpOidcIdTokenConstants.BEARER_TOKEN_TYPE;

import io.harness.oidc.accesstoken.OidcWorkloadAccessTokenResponse;
import io.harness.oidc.exception.OidcException;
import io.harness.oidc.gcp.dto.GcpOidcAccessTokenRequestDTO;
import io.harness.oidc.gcp.dto.GcpOidcTokenRequestDTO;
import io.harness.oidc.gcp.utility.GcpOidcTokenUtility;

import com.google.inject.Inject;

public class GcpOidcPlatformConnectorValidatorUtility implements GcpOidcConnectorValidatorUtility {
  @Inject private GcpOidcTokenUtility gcpOidcTokenUtility;

  @Override
  public void validateOidcAccessTokenExchange(
      String workloadPoolId, String providerId, String gcpProjectId, String serviceAccountEmail, String accountId) {
    GcpOidcTokenRequestDTO gcpOidcTokenRequestDTO = GcpOidcTokenRequestDTO.builder()
                                                        .workloadPoolId(workloadPoolId)
                                                        .providerId(providerId)
                                                        .gcpProjectId(gcpProjectId)
                                                        .serviceAccountEmail(serviceAccountEmail)
                                                        .accountId(accountId)
                                                        .build();

    // 1. Generate the OIDC ID Token
    String idToken = gcpOidcTokenUtility.generateGcpOidcIdToken(gcpOidcTokenRequestDTO);

    // 2. Exchange the OIDC ID Token for a Federated Token
    GcpOidcAccessTokenRequestDTO gcpOidcAccessTokenRequestDTO = GcpOidcAccessTokenRequestDTO.builder()
                                                                    .oidcIdToken(idToken)
                                                                    .gcpOidcTokenRequestDTO(gcpOidcTokenRequestDTO)
                                                                    .build();

    try {
      OidcWorkloadAccessTokenResponse oidcWorkloadAccessTokenResponse =
          gcpOidcTokenUtility.exchangeOidcWorkloadAccessToken(gcpOidcAccessTokenRequestDTO);
      if (!oidcWorkloadAccessTokenResponse.getToken_type().equals(BEARER_TOKEN_TYPE)) {
        throw new OidcException("Invalid OIDC Token Exchange");
      }
    } catch (RuntimeException ex) {
      throw ex;
    }
  }
}
