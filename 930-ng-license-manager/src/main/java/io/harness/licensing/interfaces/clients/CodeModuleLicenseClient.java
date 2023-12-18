/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.interfaces.clients;

import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CODEModuleLicenseDTO;

public interface CodeModuleLicenseClient extends ModuleLicenseClient<CODEModuleLicenseDTO> {
  @Override CODEModuleLicenseDTO createTrialLicense(Edition edition, String accountId);
}
