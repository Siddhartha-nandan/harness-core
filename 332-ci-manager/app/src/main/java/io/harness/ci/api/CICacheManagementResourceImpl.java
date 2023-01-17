/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.resources;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildFailureInfo;
import io.harness.app.beans.entities.CICacheManagementResource;
import io.harness.app.beans.entities.CIUsageResult;
import io.harness.app.beans.entities.CacheMetadataInfo;
import io.harness.app.beans.entities.DashboardBuildExecutionInfo;
import io.harness.app.beans.entities.DashboardBuildRepositoryInfo;
import io.harness.app.beans.entities.DashboardBuildsActiveAndFailedInfo;
import io.harness.app.beans.entities.DashboardBuildsHealthInfo;
import io.harness.cache.CICacheManagementService;
import io.harness.cimanager.dashboard.api.CIDashboardOverviewResource;
import io.harness.core.ci.services.CIOverviewDashboardService;
import io.harness.ng.core.dto.ResponseDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@OwnedBy(HarnessTeam.CI)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CICacheManagementResourceImpl implements CICacheManagementResource {
  private final CICacheManagementService ciCacheManagementService;

  public ResponseDTO<CacheMetadataInfo> getCacheInfo(String accountIdentifier) {
    log.info("Getting cache information");

    return ResponseDTO.newResponse(ciCacheManagementService.getCacheMetadata(accountIdentifier));
  }
}
