/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import io.harness.accesscontrol.clients.RoleClientFactory;
import io.harness.accesscontrol.clients.RolesClient;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class RolesClientModule extends AbstractModule {
  private final ServiceHttpClientConfig resourceGroupClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public RolesClientModule(ServiceHttpClientConfig resourceGroupClientConfig, String serviceSecret, String clientId) {
    this.resourceGroupClientConfig = resourceGroupClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Singleton
  private RoleClientFactory resourceGroupHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new RoleClientFactory(resourceGroupClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Provides
  @Named("PRIVILEGED")
  @Singleton
  private RoleClientFactory resourceGroupAdminHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new RoleClientFactory(resourceGroupClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(RolesClient.class).toProvider(RoleClientFactory.class).in(Scopes.SINGLETON);
    bind(RolesClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(RoleClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
