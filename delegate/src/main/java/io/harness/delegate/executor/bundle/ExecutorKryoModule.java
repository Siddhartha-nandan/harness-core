/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.executor.bundle;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.govern.ProviderModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.KryoSerializer;

import java.util.Set;

public class ExecutorKryoModule extends ProviderModule {
    private final Set<Class<? extends KryoRegistrar>> registars;

    public ExecutorKryoModule(Set<Class<? extends KryoRegistrar>> registars) {
        this.registars = registars;
    }

    @Provides
    @Singleton
    Set<Class<? extends KryoRegistrar>> registrars() {
        return registars;
    }

    @Provides
    @Singleton
    @Named("referenceFalseKryoSerializer")
    public KryoSerializer getKryoSerializer(final Provider<Set<Class<? extends KryoRegistrar>>> provider) {
        return new KryoSerializer(provider.get(), false, false);
    }
}
