/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.utility;

import static io.harness.oidc.idtoken.OidcIdTokenConstants.ACCOUNT_ID;
import static io.harness.oidc.idtoken.OidcIdTokenUtility.capturePlaceholderContents;
import static io.harness.oidc.idtoken.OidcIdTokenUtility.generateOidcIdToken;

import static java.lang.System.currentTimeMillis;

import io.harness.oidc.aws.credential.AwsOidcCredentialUtility;
import io.harness.oidc.aws.dto.AwsOidcTokenRequestDto;
import io.harness.oidc.config.OidcConfigurationUtility;
import io.harness.oidc.entities.OidcJwks;
import io.harness.oidc.idtoken.OidcIdTokenHeaderStructure;
import io.harness.oidc.idtoken.OidcIdTokenPayloadStructure;
import io.harness.oidc.jwks.OidcJwksUtility;
import io.harness.oidc.rsa.OidcRsaKeyService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class AwsOidcTokenUtility {
  @Inject private OidcConfigurationUtility oidcConfigurationUtility;
  @Inject private OidcJwksUtility oidcJwksUtility;
  @Inject private OidcRsaKeyService oidcRsaKeyService;
  AwsOidcCredentialUtility awsOidcCredentialUtility;

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
    // First parse all the mandatory claims.
    String baseSub = baseOidcIdTokenPayloadStructure.getSub();
    String finalSub = updateBaseClaims(baseSub, awsOidcTokenRequestDto);

    String baseAud = baseOidcIdTokenPayloadStructure.getAud();
    String finalAud = updateBaseClaims(baseAud, awsOidcTokenRequestDto);

    String baseIss = baseOidcIdTokenPayloadStructure.getIss();
    String finalIss = updateBaseClaims(baseIss, awsOidcTokenRequestDto);

    Long iat = currentTimeMillis() / 1000;
    Long exp = baseOidcIdTokenPayloadStructure.getExp();
    exp = iat + exp;

    // Now parse the optional claims.
    String accountId = null;
    if (!StringUtils.isEmpty(baseOidcIdTokenPayloadStructure.getAccountId())) {
      accountId = updateBaseClaims(baseOidcIdTokenPayloadStructure.getAccountId(), awsOidcTokenRequestDto);
    }

    return OidcIdTokenPayloadStructure.builder()
        .sub(finalSub)
        .aud(finalAud)
        .iss(finalIss)
        .iat(iat)
        .exp(exp)
        .accountId(accountId)
        .build();
  }

  /**
   * Utility function to update the given base claim
   * by replacing the placeholders with the given values.
   *
   * @param claim base claim to be updated
   * @param awsOidcTokenRequestDto provides values for updating the base claims
   * @return fully resolved final claim
   */
  private String updateBaseClaims(String claim, AwsOidcTokenRequestDto awsOidcTokenRequestDto) {
    List<String> placeHolders = capturePlaceholderContents(claim);
    for (String placeholder : placeHolders) {
      String replaceValue = "";
      switch (placeholder) {
        case ACCOUNT_ID:
          replaceValue = awsOidcTokenRequestDto.getAccountId();
          break;
      }
      // Include {} in the captured placeholder while replacing values.
      claim = claim.replace("{" + placeholder + "}", replaceValue);
    }
    return claim;
  }
}
