/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftStatus;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftServiceImpl implements SbomDriftService {
  @Override
  public void calculateDrift(
      String accountId, String orgId, String projectId, String artifactId, String baseTag, String tag) {
    // TODO: calculate component and license drift and save the results in database
  }

  @Override
  public List<ComponentDrift> getComponentDriftsByArtifactId(String accountId, String orgId, String projectId,
      String artifactId, String baseTag, String tag, ComponentDriftStatus status, Pageable pageable) {
    return new ArrayList<>();
  }
}
