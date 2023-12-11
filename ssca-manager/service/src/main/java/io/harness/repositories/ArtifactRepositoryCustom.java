/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.ArtifactEntity;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(SSCA)
public interface ArtifactRepositoryCustom {
  void invalidateOldArtifact(ArtifactEntity artifact);
  ArtifactEntity findOne(Criteria criteria);
  ArtifactEntity findOne(Criteria criteria, Sort sort, List<String> projectionFields);
  Page<ArtifactEntity> findAll(Criteria criteria, Pageable pageable);

  List<ArtifactEntity> findAll(Aggregation aggregation);
  long getCount(Aggregation aggregation);
}
