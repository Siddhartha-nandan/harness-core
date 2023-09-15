/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.idp.v1.model.EntityScores;
import io.harness.spec.server.idp.v1.model.ScorecardFilter;
import io.harness.spec.server.idp.v1.model.ScorecardGraphSummaryInfo;
import io.harness.spec.server.idp.v1.model.ScorecardScore;
import io.harness.spec.server.idp.v1.model.ScorecardSummaryInfo;

import java.util.List;

@OwnedBy(HarnessTeam.IDP)
public interface ScoreService {
  /**
   * This function populates data into scorecard related collections against the global account identifier
   * @param checkEntities check entities represented as json string
   * @param datapointEntities datapoint entities represented as json string
   * @param datasourceEntities datasource entities represented as json string
   * @param datasourceLocationEntities datasourceLocation entities represented as json string
   */
  void populateData(
      String checkEntities, String datapointEntities, String datasourceEntities, String datasourceLocationEntities);

  List<ScorecardSummaryInfo> getScoresSummaryForAnEntity(String accountIdentifier, String entityIdentifier);

  List<ScorecardGraphSummaryInfo> getScoresGraphSummaryForAnEntityAndScorecard(
      String accountIdentifier, String entityIdentifier, String scoreIdentifier);

  List<ScorecardScore> getScorecardScoreOverviewForAnEntity(String accountIdentifier, String entityIdentifier);

  ScorecardSummaryInfo getScorecardRecalibratedScoreInfoForAnEntityAndScorecard(
      String accountIdentifier, String entityIdentifier, String scorecardIdentifier);
  List<EntityScores> getEntityScores(String harnessAccount, ScorecardFilter filter);
}
