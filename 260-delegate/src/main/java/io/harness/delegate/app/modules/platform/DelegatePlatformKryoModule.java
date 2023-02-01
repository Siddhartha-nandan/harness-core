/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules.platform;

import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.kryo.ConnectorBeansKryoRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.SMDelegateKryoRegistrar;
import io.harness.serializer.kryo.SecretConfigKryoRegistrar;
import io.harness.serializer.kryo.SecretManagerClientKryoRegistrar;
import io.harness.serializer.platform.DelegatePlatformRegistrar;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Set;

public class DelegatePlatformKryoModule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> registrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .add(DelegatePlatformRegistrar.class)
        .add(SecretConfigKryoRegistrar.class)
        .add(ConnectorBeansKryoRegistrar.class)
        .add(DelegateTasksBeansKryoRegister.class)
        .add(SecretManagerClientKryoRegistrar.class)
        .add(SMDelegateKryoRegistrar.class)
        .build();
  }

  @Provides
  @Singleton
  @Named("referenceFalseKryoSerializer")
  public KryoSerializer getKryoSerializer(final Provider<Set<Class<? extends KryoRegistrar>>> provider) {
    return new KryoSerializer(provider.get(), true, false);
  }
}
