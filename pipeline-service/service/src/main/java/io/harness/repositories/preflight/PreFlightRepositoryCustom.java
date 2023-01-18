/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.preflight;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PreFlightRepositoryCustom {
  PreFlightEntity update(Criteria criteria, PreFlightEntity entity);

  PreFlightEntity update(Criteria criteria, Update update);

  /**
   * Deletes all matching preflight entity for given query
   * @param query
   */
  void deleteAllPreflightForGivenParams(Query query);
}
