/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.k8s;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import lombok.NonNull;

public class K8SSecret extends V1Secret {
  public K8SSecret(@NonNull final String name, @NonNull final String namespace) {
    metadata(new V1ObjectMeta().name(name).namespace(namespace)).type("Opaque").apiVersion("v1").kind("Secret");
  }
}
