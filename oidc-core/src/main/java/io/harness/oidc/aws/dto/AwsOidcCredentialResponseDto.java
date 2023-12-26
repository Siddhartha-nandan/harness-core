/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.dto;

import static io.harness.oidc.aws.constants.AwsOidcConstants.ACCESS_KEY;
import static io.harness.oidc.aws.constants.AwsOidcConstants.SECRET_ACCESS_KEY;
import static io.harness.oidc.aws.constants.AwsOidcConstants.SESSION_TOKEN;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsOidcCredentialResponseDto {
  @JsonProperty(ACCESS_KEY) String accessKey;
  @JsonProperty(SECRET_ACCESS_KEY) String secretAccessKey;
  @JsonProperty(SESSION_TOKEN) String sessionToken;
}
