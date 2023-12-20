/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.gcp.accesstoken;

import io.harness.network.Http;
import io.harness.oidc.exception.OidcException;
import io.harness.oidc.gcp.constants.GcpOidcServiceAccountAccessTokenRequest;
import io.harness.oidc.gcp.constants.GcpOidcServiceAccountAccessTokenResponse;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenResponse;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.IamCredentialsSettings;
// import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequestOrBuilder;
import com.google.inject.Singleton;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Slf4j
@Singleton
public class GcpOidcAccessTokenUtility {
  /**
   * Utility function to exchange for the OIDC GCP Service Account Access Token.
   *
   * @param gcpOidcIamSaApiEndpoint The GCP IAM endpoint to make the token exchange with.
   * @param gcpOidcServiceAccountAccessTokenRequest The Token exchange request body.
   * @param workloadAccessToken The OIDC Workload Access Token which will be used as Authorization bearer.
   * @return Service Account Access Token
   */
  public static GcpOidcServiceAccountAccessTokenResponse getOidcServiceAccountAccessToken(
      String gcpOidcIamSaApiEndpoint, GcpOidcServiceAccountAccessTokenRequest gcpOidcServiceAccountAccessTokenRequest,
      String workloadAccessToken) {
    // Create an OkHttpClient with any desired configurations (e.g., timeouts, interceptors)
    OkHttpClient httpClient = new OkHttpClient.Builder()
                                  .connectionPool(Http.connectionPool)
                                  .readTimeout(60, TimeUnit.SECONDS)
                                  .retryOnConnectionFailure(true)
                                  .addInterceptor(new GcpOidcAccessTokenIamSaApiInterceptor(workloadAccessToken))
                                  .build();

    // Create a Retrofit client with the base URL
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(String.valueOf(gcpOidcIamSaApiEndpoint))
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(httpClient)
                            .build();

    // Create an instance of the API interface
    GcpOidcAccessTokenIamSaApi gcpOidcAccessTokenIamSaAPI = retrofit.create(GcpOidcAccessTokenIamSaApi.class);

    // Make the POST request and handle the response

    Call<GcpOidcServiceAccountAccessTokenResponse> call = gcpOidcAccessTokenIamSaAPI.exchangeServiceAccountAccessToken(
        "harness-pl-automation@pl-play.iam.gserviceaccount.com", gcpOidcServiceAccountAccessTokenRequest);

    try {
      Response<GcpOidcServiceAccountAccessTokenResponse> response = call.execute();
      if (response.isSuccessful()) {
        GcpOidcServiceAccountAccessTokenResponse gcpOidcServiceAccountAccessTokenResponse = response.body();
        return gcpOidcServiceAccountAccessTokenResponse;
      } else {
        String errorMsg = String.format("Error encountered while obtaining OIDC Access Token from STS for %s",
            gcpOidcServiceAccountAccessTokenRequest);
        log.error(errorMsg);
        throw new OidcException(errorMsg);
      }
    } catch (IOException e) {
      String errorMsg = String.format("Exception encountered while exchanging OIDC Service Account Access Token %s", e);
      log.error(errorMsg);
      throw new OidcException(errorMsg);
    }
  }

  public static GenerateAccessTokenResponse getOidcServiceAccountAccessTokenV2(String workloadAccessToken)
      throws IOException {
    // GCP service account email
    String serviceAccountEmail = "harness-pl-automation@pl-play.iam.gserviceaccount.com";

    // Scopes for the access token
    String scope = "https://www.googleapis.com/auth/cloud-platform";

    // Create AccessToken from the OAuth 2.0 federated access token
    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(workloadAccessToken, null));

    // Create IAMCredentialsClient
    IamCredentialsClient iamCredentialsClient =
        IamCredentialsClient.create(IamCredentialsSettings.newBuilder()
                                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                                        .build());

    try {
      // Generate access token
      GenerateAccessTokenResponse response = iamCredentialsClient.generateAccessToken(
          GenerateAccessTokenRequest.newBuilder()
              .setName("projects/-/serviceAccounts/" + serviceAccountEmail)
              .addAllScope(Collections.singletonList(scope))
              .setLifetime(Duration.newBuilder().setSeconds(3600)) // Set token lifetime in seconds
              .build());

      // Extract the access token from the response
      String generatedAccessToken = response.getAccessToken();
      System.out.println("Generated Access Token: " + generatedAccessToken);
      return response;

    } finally {
      // Close the IAMCredentialsClient
      iamCredentialsClient.close();
    }
  }
}
