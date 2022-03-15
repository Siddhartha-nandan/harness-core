/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.repositories.envGroup.EnvironmentGroupRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class EnvironmentGroupServiceImpl implements EnvironmentGroupService {
  private final EnvironmentGroupRepository environmentRepository;

  @Inject
  public EnvironmentGroupServiceImpl(EnvironmentGroupRepository environmentRepository) {
    this.environmentRepository = environmentRepository;
  }

  @Override
  public Optional<EnvironmentGroupEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, boolean deleted) {
    return environmentRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, envGroupId, !deleted);
  }
}
