/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.stepDetail;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stepDetail.NodeExecutionsInfo;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PIPELINE)
@HarnessRepo
public interface NodeExecutionsInfoRepository extends PagingAndSortingRepository<NodeExecutionsInfo, String> {
  Optional<NodeExecutionsInfo> findByNodeExecutionId(String nodeExecutionId);

  /**
   * Delete all nodeExecutionsInfo for given nodeExecutionIds
   * Uses - nodeExecutionId_unique_idx index
   * @param nodeExecutionIds
   */
  void deleteAllByNodeExecutionIdIn(Set<String> nodeExecutionIds);
}
