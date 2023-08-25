/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.freeze.service;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.freeze.beans.response.FreezeSummaryResponseDTO;
import io.harness.freeze.entity.FrozenExecution;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.List;
import java.util.Optional;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_DEPLOYMENT_FREEZE})
public interface FrozenExecutionService {
  void createFrozenExecution(Ambiance ambiance, List<FreezeSummaryResponseDTO> manualFreezeConfigs,
      List<FreezeSummaryResponseDTO> globalFreezeConfigs);
  Optional<FrozenExecution> getFrozenExecution(
      String accountId, String orgId, String projectId, String planExecutionId);
}
