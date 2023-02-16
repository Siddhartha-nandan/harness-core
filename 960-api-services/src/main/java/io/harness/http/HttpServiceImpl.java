/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.google.common.base.Ascii.toUpperCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HttpCertificate;
import io.harness.beans.KeyValuePair;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.runtime.AuthenticationRuntimeException;
import io.harness.exception.runtime.AuthorizationRuntimeException;
import io.harness.globalcontex.ErrorHandlingGlobalContextData;
import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.manage.GlobalContextManager;
import io.harness.network.Http;
import io.harness.security.PemReader;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class HttpServiceImpl implements HttpService {
  @Override
  public HttpInternalResponse executeUrl(HttpInternalConfig config) throws IOException {
    HttpInternalResponse httpInternalResponse = new HttpInternalResponse();

    SSLContextBuilder builder = createSslContextBuilder(config);

    SSLConnectionSocketFactory sslsf = null;
    try {
      sslsf = new SSLConnectionSocketFactory(builder.build(), (s, sslSession) -> true);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      log.error("", e);
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(2000);
    requestBuilder = requestBuilder.setSocketTimeout(config.getSocketTimeoutMillis());
    HttpClientBuilder httpClientBuilder =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build());

    if (config.isUseProxy()) {
      if (Http.shouldUseNonProxy(config.getUrl())) {
        if (config.isThrowErrorIfNoProxySetWithDelegateProxy()) {
          throw new InvalidRequestException(
              "Delegate is configured not to use proxy for the given url: " + config.getUrl(), WingsException.USER);
        }
      } else {
        HttpHost proxyHost = Http.getHttpProxyHost();
        if (proxyHost != null) {
          if (isNotEmpty(Http.getProxyUserName())) {
            httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(proxyHost),
                new UsernamePasswordCredentials(Http.getProxyUserName(), Http.getProxyPassword()));
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
          }
          httpClientBuilder.setProxy(proxyHost);
        } else {
          log.warn("Task setup to use DelegateProxy but delegate setup without any proxy");
        }
      }
    }

    CloseableHttpClient httpclient = httpClientBuilder.build();

    HttpUriRequest httpUriRequest =
        getMethodSpecificHttpRequest(toUpperCase(config.getMethod()), config.getUrl(), config.getBody());

    if (isNotEmpty(config.getHeaders())) {
      for (KeyValuePair header : config.getHeaders()) {
        httpUriRequest.addHeader(header.getKey(), header.getValue());
      }
    }

    // For NG Headers
    if (config.getRequestHeaders() != null) {
      for (Map.Entry<String, String> entry : config.getRequestHeaders().entrySet()) {
        httpUriRequest.addHeader(entry.getKey(), entry.getValue());
      }
    }

    httpInternalResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    ErrorHandlingGlobalContextData globalContextData =
        GlobalContextManager.get(ErrorHandlingGlobalContextData.IS_SUPPORTED_ERROR_FRAMEWORK);
    if (globalContextData != null && globalContextData.isSupportedErrorFramework()) {
      return executeHttpStep(httpclient, httpInternalResponse, httpUriRequest, config, true);
    } else {
      try {
        executeHttpStep(httpclient, httpInternalResponse, httpUriRequest, config, false);
      } catch (SocketTimeoutException | ConnectTimeoutException | HttpHostConnectException e) {
        handleException(httpInternalResponse, e, true);
      } catch (IOException e) {
        handleException(httpInternalResponse, e, false);
      }

      return httpInternalResponse;
    }
  }

  @VisibleForTesting
  protected HttpInternalResponse executeHttpStep(CloseableHttpClient httpclient,
      HttpInternalResponse httpInternalResponse, HttpUriRequest httpUriRequest, HttpInternalConfig httpInternalConfig,
      boolean isSupportingErrorFramework) throws IOException {
    return executeHttpStepTrial(
        httpclient, httpInternalResponse, httpUriRequest, httpInternalConfig, isSupportingErrorFramework);
  }

  @VisibleForTesting
  protected HttpInternalResponse executeHttpStepTrial(CloseableHttpClient httpclient,
      HttpInternalResponse httpInternalResponse, HttpUriRequest httpUriRequest, HttpInternalConfig httpInternalConfig,
      boolean isSupportingErrorFramework) throws IOException {
    try (CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpUriRequest)) {
      HttpEntity httpEntity = closeableHttpResponse.getEntity();

      if (isSupportingErrorFramework) {
        if (closeableHttpResponse.getStatusLine().getStatusCode() == 401) {
          throw new AuthenticationRuntimeException(httpUriRequest.getURI().toString());
        }

        if (closeableHttpResponse.getStatusLine().getStatusCode() == 403) {
          throw new AuthorizationRuntimeException(httpUriRequest.getURI().toString());
        }
      }

      httpInternalResponse.setHttpResponseCode(closeableHttpResponse.getStatusLine().getStatusCode());
      httpInternalResponse.setHttpResponseBody(
          httpEntity != null ? EntityUtils.toString(httpEntity, StandardCharsets.UTF_8) : "");

      EntityUtils.consume(httpEntity);
    }

    httpInternalResponse.setHeader(httpInternalConfig.getHeader());

    return httpInternalResponse;
  }

  private void handleException(HttpInternalResponse httpInternalResponse, IOException e, boolean timedOut) {
    log.error("Exception occurred during HTTP task execution", e);
    httpInternalResponse.setHttpResponseCode(500);
    httpInternalResponse.setHttpResponseBody(getMessage(e));
    httpInternalResponse.setErrorMessage(getMessage(e));
    httpInternalResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    httpInternalResponse.setTimedOut(timedOut);
  }

  @VisibleForTesting
  protected HttpUriRequest getMethodSpecificHttpRequest(String method, String url, String body) {
    switch (method) {
      case "GET":
        return new HttpGet(url);
      case "POST":
        HttpPost post = new HttpPost(url);
        setEntity(body, post);
        return post;
      case "PATCH":
        HttpPatch patch = new HttpPatch(url);
        setEntity(body, patch);
        return patch;
      case "PUT":
        HttpPut put = new HttpPut(url);
        setEntity(body, put);
        return put;
      case "DELETE":
        return new HttpDelete(url);
      case "HEAD":
      default:
        return new HttpHead(url);
    }
  }

  private void setEntity(String body, HttpEntityEnclosingRequestBase entityEnclosingRequestBase) {
    if (body != null) {
      entityEnclosingRequestBase.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    }
  }

  private SSLContextBuilder createSslContextBuilder(final HttpInternalConfig config) {
    SSLContextBuilder builder = new SSLContextBuilder();

    final boolean insecure = isInsecure(config);
    if (config.getCertificate() == null) {
      if (insecure) {
        prepareInsecureSslContext(builder);
      }
    } else {
      log.info("Prepare ssl/tls authentication [mutual={},insecure={}]", config.getCertificate().isMutual(), insecure);
      prepareSslContext(builder, config);
    }

    return builder;
  }

  private void prepareInsecureSslContext(SSLContextBuilder builder) {
    try {
      builder.loadTrustMaterial((x509Certificates, s) -> true);
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      log.error("", e);
    }
  }

  /**
   * Should ignore the CA certificate validation? It's relate to use -k or --insecure from cURL command.
   */
  private boolean isInsecure(HttpInternalConfig config) {
    return !config.isCertValidationRequired();
  }

  private void prepareSslContext(SSLContextBuilder builder, HttpInternalConfig config) {
    io.harness.beans.HttpCertificate httpC = config.getCertificate();
    try {
      final KeyStore ks =
          KeyStore.getInstance(httpC.getKeyStoreType() == null ? KeyStore.getDefaultType() : httpC.getKeyStoreType());
      ks.load(null, null);

      final String rawCert = String.valueOf(httpC.getCert());
      final X509Certificate[] certs = readCertificates(rawCert);
      loadTrustMaterial(builder, ks, certs, isInsecure(config));

      if (httpC.isMutual()) {
        loadKeyMaterial(builder, ks, certs, httpC);
      }

    } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException | RuntimeException
        | InvalidKeySpecException | UnrecoverableKeyException e) {
      log.warn("Unable to prepare ssl/tls authentication", e);
    }
  }

  /**
   * Trusted certificates for verifying the remote endpoint's certificate
   */
  private void loadTrustMaterial(SSLContextBuilder builder, KeyStore ks, X509Certificate[] certs, boolean insecure)
      throws KeyStoreException, NoSuchAlgorithmException {
    int i = 1;
    for (X509Certificate cert : certs) {
      String alias = Integer.toString(i);
      ks.setCertificateEntry(alias, cert);
      i++;
    }

    // WHEN INSECURE WE TRUST ALL CERTIFICATE IGNORING THE CA VERIFICATION
    final TrustStrategy trustStrategy = insecure ? (x509Certificates, s) -> true : null;
    builder.loadTrustMaterial(ks, trustStrategy);
  }

  private void loadKeyMaterial(SSLContextBuilder builder, KeyStore ks, X509Certificate[] certs, HttpCertificate httpC)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException,
             UnrecoverableKeyException {
    final String rawKey = String.valueOf(httpC.getCertKey());
    final ByteArrayInputStream is = new ByteArrayInputStream(rawKey.getBytes());
    final PrivateKey key = PemReader.readPrivateKey(is);
    ks.setKeyEntry("key", key, null, certs);

    builder.loadKeyMaterial(ks, null);
  }

  private X509Certificate[] readCertificates(String rawCert) throws CertificateException {
    final ByteArrayInputStream is = new ByteArrayInputStream(rawCert.getBytes());
    return PemReader.getCertificates(is);
  }
}
