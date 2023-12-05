/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.services;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.TunnelRegisterRequestDTO;
import io.harness.ng.core.dto.TunnelResponseDTO;

@OwnedBy(CI)
public interface TunnelService {
  Boolean registerTunnel(String accountId, TunnelRegisterRequestDTO tunnelRegisterRequestDTO);
  Boolean deleteTunnel(String accountId);
  TunnelResponseDTO getTunnel(String accountId);
}
