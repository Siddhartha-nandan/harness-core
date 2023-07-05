/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.spec.server.ssca.v1.model.SbomProcessRequestBody;
import io.harness.ssca.beans.SbomDTO;
import io.harness.ssca.entities.ArtifactEntity;

public interface ArtifactService {
  ArtifactEntity getArtifactFromSbomPayload(
      String accountId, String orgIdentifier, String projectIdentifier, SbomProcessRequestBody body, SbomDTO sbomDTO);
}
