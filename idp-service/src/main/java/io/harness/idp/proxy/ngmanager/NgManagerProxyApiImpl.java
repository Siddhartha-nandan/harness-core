/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.proxy.ngmanager;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.annotations.IdpServiceAuthIfHasApiKey;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OwnedBy(HarnessTeam.IDP)
@RequestScoped
@NextGenManagerAuth
@NoArgsConstructor
@Slf4j
public class NgManagerProxyApiImpl implements NgManagerProxyApi {
  private static final String FORWARDING_MESSAGE = "Forwarding request to [{}]";
  private ServiceHttpClientConfig ngManagerServiceHttpClientConfig;
  private String ngManagerServiceSecret;
  private ServiceTokenGenerator tokenGenerator;

  @Inject
  public NgManagerProxyApiImpl(
      @Named("ngManagerServiceHttpClientConfig") ServiceHttpClientConfig ngManagerServiceHttpClientConfig,
      @Named("ngManagerServiceSecret") String ngManagerServiceSecret, ServiceTokenGenerator tokenGenerator) {
    this.ngManagerServiceHttpClientConfig = ngManagerServiceHttpClientConfig;
    this.ngManagerServiceSecret = ngManagerServiceSecret;
    this.tokenGenerator = tokenGenerator;
  }

  @IdpServiceAuthIfHasApiKey
  @Override
  public Response deleteProxyNgManager(UriInfo uriInfo, HttpHeaders headers, String url, String harnessAccount) {
    OkHttpClient client = getOkHttpClient();
    HttpUrl.Builder urlBuilder =
        Objects.requireNonNull(HttpUrl.parse(ngManagerServiceHttpClientConfig.getBaseUrl())).newBuilder();
    ProxyUtils.filterAndCopyPath(uriInfo, urlBuilder);
    ProxyUtils.copyQueryParams(uriInfo, urlBuilder);

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().toString()).delete();
    ProxyUtils.copyHeaders(headers, requestBuilder);

    okhttp3.Response response;
    try {
      log.info(FORWARDING_MESSAGE, urlBuilder);
      response = client.newCall(requestBuilder.build()).execute();
      return ProxyUtils.buildResponseObject(response);
    } catch (Exception e) {
      log.error("Could not forward DELETE request to ng manager", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @IdpServiceAuthIfHasApiKey
  @Override
  public Response getProxyNgManager(UriInfo uriInfo, HttpHeaders headers, String url, String harnessAccount) {
    OkHttpClient client = getOkHttpClient();
    HttpUrl.Builder urlBuilder =
        Objects.requireNonNull(HttpUrl.parse(ngManagerServiceHttpClientConfig.getBaseUrl())).newBuilder();
    ProxyUtils.filterAndCopyPath(uriInfo, urlBuilder);
    ProxyUtils.copyQueryParams(uriInfo, urlBuilder);

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().toString()).get();
    ProxyUtils.copyHeaders(headers, requestBuilder);

    okhttp3.Response response;
    try {
      Request request = requestBuilder.build();
      log.info(FORWARDING_MESSAGE, urlBuilder);
      response = client.newCall(request).execute();
      return ProxyUtils.buildResponseObject(response);
    } catch (Exception e) {
      log.error("Could not forward request GET to ng manager", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @IdpServiceAuthIfHasApiKey
  @Override
  public Response postProxyNgManager(
      UriInfo uriInfo, HttpHeaders headers, String url, String harnessAccount, String body) {
    OkHttpClient client = getOkHttpClient();
    HttpUrl.Builder urlBuilder =
        Objects.requireNonNull(HttpUrl.parse(ngManagerServiceHttpClientConfig.getBaseUrl())).newBuilder();
    ProxyUtils.filterAndCopyPath(uriInfo, urlBuilder);
    ProxyUtils.copyQueryParams(uriInfo, urlBuilder);

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().toString()).post(new RequestBody() {
      @Nullable
      @Override
      public MediaType contentType() {
        return MediaType.parse(headers.getMediaType().toString());
      }

      @Override
      public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        bufferedSink.write(body.getBytes());
      }
    });
    ProxyUtils.copyHeaders(headers, requestBuilder);

    okhttp3.Response response;
    try {
      log.info(FORWARDING_MESSAGE, urlBuilder);
      response = client.newCall(requestBuilder.build()).execute();
      return ProxyUtils.buildResponseObject(response);
    } catch (Exception e) {
      log.error("Could not forward POST request to ng manager", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @IdpServiceAuthIfHasApiKey
  @Override
  public Response putProxyNgManager(
      UriInfo uriInfo, HttpHeaders headers, String url, String harnessAccount, String body) {
    OkHttpClient client = getOkHttpClient();
    HttpUrl.Builder urlBuilder =
        Objects.requireNonNull(HttpUrl.parse(ngManagerServiceHttpClientConfig.getBaseUrl())).newBuilder();
    ProxyUtils.filterAndCopyPath(uriInfo, urlBuilder);
    ProxyUtils.copyQueryParams(uriInfo, urlBuilder);

    Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build().toString()).put(new RequestBody() {
      @Nullable
      @Override
      public MediaType contentType() {
        return MediaType.parse(headers.getMediaType().toString());
      }

      @Override
      public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        bufferedSink.write(body.getBytes());
      }
    });
    ProxyUtils.copyHeaders(headers, requestBuilder);

    okhttp3.Response response;
    try {
      log.info(FORWARDING_MESSAGE, urlBuilder);
      response = client.newCall(requestBuilder.build()).execute();
      return ProxyUtils.buildResponseObject(response);
    } catch (Exception e) {
      log.error("Could not forward PUT request to ng manager", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @VisibleForTesting
  OkHttpClient getOkHttpClient() {
    return new OkHttpClient()
        .newBuilder()
        .connectTimeout(ngManagerServiceHttpClientConfig.getConnectTimeOutSeconds(), TimeUnit.SECONDS)
        .readTimeout(ngManagerServiceHttpClientConfig.getConnectTimeOutSeconds(), TimeUnit.SECONDS)
        .writeTimeout(ngManagerServiceHttpClientConfig.getConnectTimeOutSeconds(), TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(new IdpAuthInterceptor(tokenGenerator, ngManagerServiceSecret))
        .addInterceptor(getEncodingInterceptor())
        .build();
  }

  private static Interceptor getEncodingInterceptor() {
    return chain -> chain.proceed(chain.request().newBuilder().header("Accept-Encoding", "identity").build());
  }
}
