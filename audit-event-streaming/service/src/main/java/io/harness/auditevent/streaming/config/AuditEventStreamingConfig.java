/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.auditevent.streaming.config;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditEventStreamingConfig {
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty("serviceSecrets") @ConfigSecret ServiceSecrets serviceSecrets;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
}
