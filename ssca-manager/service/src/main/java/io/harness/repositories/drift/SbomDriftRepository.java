/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.drift;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.drift.DriftEntity;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(SSCA)
public interface SbomDriftRepository extends CrudRepository<DriftEntity, String>, SbomDriftRepositoryCustom {
  Optional<DriftEntity>
  findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndOrchestrationIdAndBaseOrchestrationId(
      String accountId, String orgId, String projectId, String orchestrationId, String baseOrchestrationId);
}
