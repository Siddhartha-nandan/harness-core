/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryResponse;
import io.harness.spec.server.ssca.v1.model.OrchestrationSummaryResponse;
import io.harness.spec.server.ssca.v1.model.TokenIssueResponseBody;
import io.harness.ssca.client.beans.SBOMArtifactResponse;
import io.harness.ssca.client.beans.SscaAuthToken;
import io.harness.ssca.client.beans.enforcement.SscaEnforcementSummary;
import io.harness.ssca.utils.SSCACommonEndpointConstants;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.SSCA)
public interface SSCAServiceClient {
  @GET(SSCACommonEndpointConstants.SSCA_SERVICE_ARTIFACT_ENDPOINT + "stepExecutions/{stepExecutionId}")
  Call<SBOMArtifactResponse> getArtifactInfoV2(@Path("stepExecutionId") String stepExecutionId,
      @Query("accountIdentifier") String accountIdentifier, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);

  @GET(SSCACommonEndpointConstants.SSCA_SERVICE_TOKEN_ENDPOINT)
  Call<SscaAuthToken> generateAuthToken(@Query("accountIdentifier") String accountIdentifier,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier);

  @GET(SSCACommonEndpointConstants.SSCA_MANAGER_ORCHESTRATION_SUMMARY_ENDPOINT)
  Call<OrchestrationSummaryResponse> getOrchestrationSummary(@Path("org") String org, @Path("project") String project,
      @Path("orchestration-id") String orchestrationId, @Header("Harness-Account") String accountIdentifier);
  @GET(SSCACommonEndpointConstants.SSCA_MANAGER_ENFORCEMENT_ENDPOINT)
  Call<EnforcementSummaryResponse> getEnforcementSummary(@Path("org") String org, @Path("project") String project,
      @Path("enforcement-id") String enforcementId, @Header("Harness-Account") String accountIdentifier);

  @GET(SSCACommonEndpointConstants.SSCA_SERVICE_ENFORCEMENT_ENDPOINT + "{enforcementId}/summary")
  Call<SscaEnforcementSummary> getEnforcementSummary(@Path("enforcementId") String enforcementId);

  @GET(SSCACommonEndpointConstants.SSCA_MANAGER_TOKEN_ENDPOINT)
  Call<TokenIssueResponseBody> generateSSCAAuthToken(@Header("Harness-Account") String accountIdentifier);
}
