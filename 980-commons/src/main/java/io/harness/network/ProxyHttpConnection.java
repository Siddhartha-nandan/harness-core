/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.network;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.http.HttpConnection;
import org.eclipse.jgit.transport.http.apache.TemporaryBufferEntity;
import org.eclipse.jgit.transport.http.apache.internal.HttpApacheText;
import org.eclipse.jgit.util.HttpSupport;
import org.eclipse.jgit.util.TemporaryBuffer;

public class ProxyHttpConnection implements HttpConnection {
  HttpClient client;
  URL url;
  HttpUriRequest req;
  HttpResponse resp;
  String method;
  private TemporaryBufferEntity entity;
  private boolean isUsingProxy;
  private Proxy proxy;
  private Integer timeout;
  private Integer readTimeout;
  private Boolean followRedirects;
  private HostnameVerifier hostnameverifier;
  private SSLContext ctx;
  private SSLConnectionSocketFactory socketFactory;
  private boolean usePooling;
  private String proxyHost;
  private int proxyPort;
  private String proxyUsername;
  private String proxyPassword;

  public HttpClient getClient() {
    if (this.client == null) {
      HttpClientBuilder clientBuilder = HttpClients.custom();
      RequestConfig.Builder configBuilder = RequestConfig.custom();
      if (this.proxy != null && !Proxy.NO_PROXY.equals(this.proxy)) {
        this.isUsingProxy = true;
        InetSocketAddress adr = (InetSocketAddress) this.proxy.address();
        clientBuilder.setProxy(new HttpHost(adr.getHostName(), adr.getPort()));
      }

      if (this.timeout != null) {
        configBuilder.setConnectTimeout(this.timeout);
      }

      if (this.readTimeout != null) {
        configBuilder.setSocketTimeout(this.readTimeout);
      }

      if (this.followRedirects != null) {
        configBuilder.setRedirectsEnabled(this.followRedirects);
      }

      boolean pooled = true;
      SSLConnectionSocketFactory sslConnectionFactory;
      if (this.socketFactory != null) {
        pooled = this.usePooling;
        sslConnectionFactory = this.socketFactory;
      } else {
        pooled = this.hostnameverifier == null;
        sslConnectionFactory = this.getSSLSocketFactory();
      }

      clientBuilder.setSSLSocketFactory(sslConnectionFactory);
      if (!pooled) {
        RegistryBuilder<ConnectionSocketFactory> registryBulder = RegistryBuilder.create();
        Registry<ConnectionSocketFactory> registry = registryBulder.register("https", sslConnectionFactory)
                                                         .register("http", PlainConnectionSocketFactory.INSTANCE)
                                                         .build();

        clientBuilder.setConnectionManager(new BasicHttpClientConnectionManager(registry));
      }

      clientBuilder.setDefaultRequestConfig(configBuilder.build());

      if (isNotBlank(this.proxyUsername) && isNotBlank(this.proxyPassword)) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(this.proxyHost, this.proxyPort),
            new UsernamePasswordCredentials(this.proxyUsername, this.proxyPassword));
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
      } else {
        clientBuilder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider());
      }
      this.client = clientBuilder.build();
    }

    return this.client;
  }

  void setSSLSocketFactory(@NonNull SSLConnectionSocketFactory factory, boolean isDefault) {
    this.socketFactory = factory;
    this.usePooling = isDefault;
  }

  private SSLConnectionSocketFactory getSSLSocketFactory() {
    HostnameVerifier verifier = this.hostnameverifier;
    SSLContext context;
    if (verifier == null) {
      context = SSLContexts.createSystemDefault();
      verifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
    } else {
      context = this.getSSLContext();
    }

    return new SSLConnectionSocketFactory(context, verifier) {
      protected void prepareSocket(SSLSocket socket) throws IOException {
        super.prepareSocket(socket);
        HttpSupport.configureTLS(socket);
      }
    };
  }

  private SSLContext getSSLContext() {
    if (this.ctx == null) {
      try {
        this.ctx = SSLContext.getInstance("TLS");
      } catch (NoSuchAlgorithmException var2) {
        throw new IllegalStateException(HttpApacheText.get().unexpectedSSLContextException, var2);
      }
    }

    return this.ctx;
  }

  public void setBuffer(TemporaryBuffer buffer) {
    this.entity = new TemporaryBufferEntity(buffer);
  }

  public ProxyHttpConnection(String urlStr, Proxy proxy, String proxyHost, int proxyPort, String proxyUsername,
      String proxyPassword) throws MalformedURLException {
    this(urlStr, proxy, (HttpClient) null);
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
  }

  public ProxyHttpConnection(String urlStr, Proxy proxy, HttpClient cl) throws MalformedURLException {
    this.resp = null;
    this.method = "GET";
    this.isUsingProxy = false;
    this.timeout = null;
    this.usePooling = true;
    this.client = cl;
    this.url = new URL(urlStr);
    this.proxy = proxy;
  }

  public int getResponseCode() throws IOException {
    this.execute();
    return this.resp.getStatusLine().getStatusCode();
  }

  public URL getURL() {
    return this.url;
  }

  public String getResponseMessage() throws IOException {
    this.execute();
    return this.resp.getStatusLine().getReasonPhrase();
  }

  private void execute() throws IOException, ClientProtocolException {
    if (this.resp == null) {
      if (this.entity == null) {
        this.resp = this.getClient().execute(this.req);
      } else {
        try {
          if (this.req instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest eReq = (HttpEntityEnclosingRequest) this.req;
            eReq.setEntity(this.entity);
          }

          this.resp = this.getClient().execute(this.req);
        } finally {
          this.entity.close();
          this.entity = null;
        }
      }
    }
  }

  public Map<String, List<String>> getHeaderFields() {
    Map<String, List<String>> ret = new HashMap();
    Header[] var5;
    int var4 = (var5 = this.resp.getAllHeaders()).length;

    for (int var3 = 0; var3 < var4; ++var3) {
      Header hdr = var5[var3];
      List<String> list = (List) ret.get(hdr.getName());
      if (list == null) {
        list = new LinkedList();
        ret.put(hdr.getName(), list);
      }

      HeaderElement[] var10;
      int var9 = (var10 = hdr.getElements()).length;

      for (int var8 = 0; var8 < var9; ++var8) {
        HeaderElement hdrElem = var10[var8];
        ((List) list).add(hdrElem.toString());
      }
    }

    return ret;
  }

  public void setRequestProperty(String name, String value) {
    this.req.addHeader(name, value);
  }

  public void setRequestMethod(String method) throws ProtocolException {
    this.method = method;
    if ("GET".equalsIgnoreCase(method)) {
      this.req = new HttpGet(this.url.toString());
    } else if ("HEAD".equalsIgnoreCase(method)) {
      this.req = new HttpHead(this.url.toString());
    } else if ("PUT".equalsIgnoreCase(method)) {
      this.req = new HttpPut(this.url.toString());
    } else {
      if (!"POST".equalsIgnoreCase(method)) {
        this.method = null;
        throw new UnsupportedOperationException();
      }

      this.req = new HttpPost(this.url.toString());
    }
  }

  public void setUseCaches(boolean usecaches) {}

  public void setConnectTimeout(int timeout) {
    this.timeout = timeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  public String getContentType() {
    HttpEntity responseEntity = this.resp.getEntity();
    if (responseEntity != null) {
      Header contentType = responseEntity.getContentType();
      if (contentType != null) {
        return contentType.getValue();
      }
    }

    return null;
  }

  public InputStream getInputStream() throws IOException {
    this.execute();
    return this.resp.getEntity().getContent();
  }

  public String getHeaderField(@NonNull String name) {
    Header header = this.resp.getFirstHeader(name);
    return header == null ? null : header.getValue();
  }

  public List<String> getHeaderFields(@NonNull String name) {
    return Collections.unmodifiableList((List) Arrays.asList(this.resp.getHeaders(name))
                                            .stream()
                                            .map(NameValuePair::getValue)
                                            .collect(Collectors.toList()));
  }

  public int getContentLength() {
    Header contentLength = this.resp.getFirstHeader("content-length");
    if (contentLength == null) {
      return -1;
    } else {
      try {
        int l = Integer.parseInt(contentLength.getValue());
        return l < 0 ? -1 : l;
      } catch (NumberFormatException var3) {
        return -1;
      }
    }
  }

  public void setInstanceFollowRedirects(boolean followRedirects) {
    this.followRedirects = followRedirects;
  }

  public void setDoOutput(boolean dooutput) {}

  public void setFixedLengthStreamingMode(int contentLength) {
    if (this.entity != null) {
      throw new IllegalArgumentException();
    } else {
      this.entity = new TemporaryBufferEntity(new TemporaryBuffer.LocalFile((File) null));
      this.entity.setContentLength(contentLength);
    }
  }

  public OutputStream getOutputStream() throws IOException {
    if (this.entity == null) {
      this.entity = new TemporaryBufferEntity(new TemporaryBuffer.LocalFile((File) null));
    }

    return this.entity.getBuffer();
  }

  public void setChunkedStreamingMode(int chunklen) {
    if (this.entity == null) {
      this.entity = new TemporaryBufferEntity(new TemporaryBuffer.LocalFile((File) null));
    }

    this.entity.setChunked(true);
  }

  public String getRequestMethod() {
    return this.method;
  }

  public boolean usingProxy() {
    return this.isUsingProxy;
  }

  public void connect() throws IOException {
    this.execute();
  }

  public void setHostnameVerifier(HostnameVerifier hostnameverifier) {
    this.hostnameverifier = hostnameverifier;
  }

  public void configure(KeyManager[] km, TrustManager[] tm, SecureRandom random) throws KeyManagementException {
    this.getSSLContext().init(km, tm, random);
  }
}
