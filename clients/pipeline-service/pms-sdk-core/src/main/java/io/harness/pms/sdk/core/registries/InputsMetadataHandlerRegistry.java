/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.registries;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataHandler;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@OwnedBy(PIPELINE)
public class InputsMetadataHandlerRegistry implements Registry<String, InputsMetadataHandler> {
  Map<String, InputsMetadataHandler> registry = new ConcurrentHashMap<>();

  @Override
  public void register(String registryKey, InputsMetadataHandler inputsMetadataHandler) {
    if (registry.containsKey(registryKey)) {
      throw new DuplicateRegistryException(
          getType(), "Json Expansion Handler already registered for key " + registryKey);
    }
    registry.put(registryKey, inputsMetadataHandler);
  }

  @Override
  public InputsMetadataHandler obtain(String registryKey) {
    if (registry.containsKey(registryKey)) {
      return registry.get(registryKey);
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Inputs Metadata Handler registered for key: " + registryKey);
  }

  @Override
  public String getType() {
    return RegistryType.RUNTIME_INPUTS_METADATA_HANDLERS.name();
  }
}
