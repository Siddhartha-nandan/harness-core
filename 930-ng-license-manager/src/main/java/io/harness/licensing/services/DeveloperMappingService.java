/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness.licensing.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.DeveloperMappingDTO;

import java.util.List;

@OwnedBy(HarnessTeam.GTM)
public interface DeveloperMappingService {
  List<DeveloperMappingDTO> getAccountLevelDeveloperMapping(String accountIdentifier);

  DeveloperMappingDTO createAccountLevelDeveloperMapping(
      String accountIdentifier, DeveloperMappingDTO developerMappingDTO);
}
