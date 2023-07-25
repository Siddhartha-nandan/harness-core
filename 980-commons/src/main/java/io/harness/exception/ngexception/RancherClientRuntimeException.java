/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.ngexception;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

import lombok.Builder;
import lombok.Data;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
@Data
@OwnedBy(HarnessTeam.CDP)
public class RancherClientRuntimeException extends RuntimeException {
  public enum RancherActionType { GENERATE_KUBECONFIG, LIST_CLUSTERS }
  private RancherActionType actionType;
  private RancherRequestData requestData;
  public RancherClientRuntimeException(String message) {
    super(message);
  }

  public RancherClientRuntimeException(String message, RancherActionType actionType, RancherRequestData requestData) {
    super(message);
    this.actionType = actionType;
    this.requestData = requestData;
  }

  public RancherClientRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  @Data
  @Builder
  public static class RancherRequestData {
    String errorMessage;
    String errorBody;
    String endpoint;
    int code;
  }
}
