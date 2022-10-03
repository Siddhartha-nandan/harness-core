/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.beans.refresh.v2;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.template.beans.refresh.NodeInfo;

import java.util.List;
import lombok.Builder;

@OwnedBy(HarnessTeam.CDC)
public class ServiceNodeErrorSummary extends NodeErrorSummary {
  ServiceResponseDTO serviceResponse;

  @Builder
  public ServiceNodeErrorSummary(
      NodeInfo nodeInfo, ServiceResponseDTO serviceResponse, List<NodeErrorSummary> childrenErrorNodes) {
    super(nodeInfo, childrenErrorNodes);
    this.serviceResponse = serviceResponse;
  }

  @Override
  public ErrorNodeType getType() {
    return ErrorNodeType.SERVICE;
  }
}
