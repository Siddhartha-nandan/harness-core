/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.dashboard;

import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.pms.dashboards.ExecutionsCount;
import io.harness.pms.dashboards.PipelinesCount;

import java.util.List;

public interface PMSLandingDashboardService {
  PipelinesCount getPipelinesCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);
  ExecutionsCount getExecutionsCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);
}
