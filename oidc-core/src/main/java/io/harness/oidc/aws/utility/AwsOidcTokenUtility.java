/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.utility;

import static io.harness.oidc.aws.constants.AwsOidcConstants.CONVERSION_FACTOR_FOR_MS;
import static io.harness.oidc.idtoken.OidcIdTokenConstants.ACCOUNT_ID;
import static io.harness.oidc.idtoken.OidcIdTokenUtility.generateOidcIdToken;
import static io.harness.oidc.idtoken.OidcIdTokenUtility.updateClaim;

import static java.lang.System.currentTimeMillis;

import io.harness.oidc.aws.dto.AwsOidcTokenRequestDto;
import io.harness.oidc.config.OidcConfigurationUtility;
import io.harness.oidc.entities.OidcJwks;
import io.harness.oidc.idtoken.OidcIdTokenHeaderStructure;
import io.harness.oidc.idtoken.OidcIdTokenPayloadStructure;
import io.harness.oidc.jwks.OidcJwksUtility;
import io.harness.oidc.rsa.OidcRsaKeyService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class AwsOidcTokenUtility {
  @Inject private OidcConfigurationUtility oidcConfigurationUtility;
  @Inject private OidcJwksUtility oidcJwksUtility;
  @Inject private OidcRsaKeyService oidcRsaKeyService;
  /**
   * Utility function to generate the OIDC ID Token for AWS.
   *
   * @param awsOidcTokenRequestDto AWS metadata needed to generate ID token
   * @return OIDC ID Token for AWS
   */
  public String generateAwsOidcIdToken(AwsOidcTokenRequestDto awsOidcTokenRequestDto) {
    // Get the base OIDC ID Token Header and Payload structure.
    OidcIdTokenHeaderStructure baseOidcIdTokenHeaderStructure =
        oidcConfigurationUtility.getAwsOidcTokenStructure().getOidcIdTokenHeaderStructure();
    OidcIdTokenPayloadStructure baseOidcIdTokenPayloadStructure =
        oidcConfigurationUtility.getAwsOidcTokenStructure().getOidcIdTokenPayloadStructure();

    // Get the JWKS private key and kid
    OidcJwks oidcJwks = oidcJwksUtility.getJwksKeys(awsOidcTokenRequestDto.getAccountId());

    // parse the base token structure and generate appropriate values
    OidcIdTokenHeaderStructure finalOidcIdTokenHeader =
        parseOidcIdTokenHeader(baseOidcIdTokenHeaderStructure, oidcJwks.getKeyId());
    OidcIdTokenPayloadStructure finalOidcIdTokenPayload =
        parseOidcIdTokenPayload(baseOidcIdTokenPayloadStructure, awsOidcTokenRequestDto);

    // Generate the OIDC ID Token JWT
    return generateOidcIdToken(finalOidcIdTokenHeader, finalOidcIdTokenPayload,
        oidcRsaKeyService.getDecryptedJwksPrivateKeyPem(
            awsOidcTokenRequestDto.getAccountId(), oidcJwks.getRsaKeyPair()));
  }

  private OidcIdTokenHeaderStructure parseOidcIdTokenHeader(
      OidcIdTokenHeaderStructure baseOidcIdTokenHeaderStructure, String kid) {
    return OidcIdTokenHeaderStructure.builder()
        .typ(baseOidcIdTokenHeaderStructure.getTyp())
        .alg(baseOidcIdTokenHeaderStructure.getAlg())
        .kid(kid)
        .build();
  }

  /**
   * This function is used to parse the base Oidc ID token payload structure
   * and generate the appropriate values for AWS ID token payload.
   *
   * @param baseOidcIdTokenPayloadStructure base payload values for ID token
   * @param awsOidcTokenRequestDto GCP metadata needed for payload
   * @return OIDC ID Token Payload
   */
  private OidcIdTokenPayloadStructure parseOidcIdTokenPayload(
      OidcIdTokenPayloadStructure baseOidcIdTokenPayloadStructure, AwsOidcTokenRequestDto awsOidcTokenRequestDto) {
    String sub = updateBaseClaims(baseOidcIdTokenPayloadStructure.getSub(), awsOidcTokenRequestDto);

    String aud = updateBaseClaims(baseOidcIdTokenPayloadStructure.getAud(), awsOidcTokenRequestDto);

    String iss = updateBaseClaims(baseOidcIdTokenPayloadStructure.getIss(), awsOidcTokenRequestDto);

    Long iat = currentTimeMillis() / CONVERSION_FACTOR_FOR_MS;
    Long expiryDuration = baseOidcIdTokenPayloadStructure.getExp();
    Long exp = iat + expiryDuration;

    String accountId = null;
    if (!StringUtils.isEmpty(baseOidcIdTokenPayloadStructure.getAccountId())) {
      accountId = updateBaseClaims(baseOidcIdTokenPayloadStructure.getAccountId(), awsOidcTokenRequestDto);
    }

    return OidcIdTokenPayloadStructure.builder()
        .sub(sub)
        .aud(aud)
        .iss(iss)
        .iat(iat)
        .exp(exp)
        .accountId(accountId)
        .build();
  }

  private String updateBaseClaims(String claim, AwsOidcTokenRequestDto awsOidcTokenRequestDto) {
    return updateClaim(claim, ACCOUNT_ID, awsOidcTokenRequestDto.getAccountId());
  }
}
