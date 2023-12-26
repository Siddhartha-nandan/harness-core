/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.aws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AwsOidcCredentialsRequest", description = "This contains Aws OIDC Credentials request details")
public class AwsOidcCredentialRequestDto {
  @Schema(description = "The OIDC ID Token") private String oidcIdToken;
  @NotEmpty @Schema(description = "IAM Role ARN") private String iamRoleArn;
  @Schema(description = "Retry policy for aws sdk calls") private AwsSdkCallRetryPolicy retryPolicy;
  @NotNull private AwsOidcTokenRequestDto awsOidcTokenRequestDto;
}
