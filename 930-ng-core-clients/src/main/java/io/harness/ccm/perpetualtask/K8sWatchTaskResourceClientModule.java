/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

@OwnedBy(CE)
public class K8sWatchTaskResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig httpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public K8sWatchTaskResourceClientModule(
      ServiceHttpClientConfig httpClientConfig, String serviceSecret, String clientId) {
    this.httpClientConfig = httpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Singleton
  private K8sWatchTaskResourceHttpClientFactory secretManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new K8sWatchTaskResourceHttpClientFactory(
        this.httpClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(K8sWatchTaskResourceClient.class)
        .toProvider(K8sWatchTaskResourceHttpClientFactory.class)
        .in(Scopes.SINGLETON);
  }
}
