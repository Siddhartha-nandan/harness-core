/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.resources;

import static io.harness.idp.common.RbacConstants.IDP_SCORECARD;
import static io.harness.idp.common.RbacConstants.IDP_SCORECARD_EDIT;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.scorecard.scores.service.AsyncScoreComputationService;
import io.harness.idp.scorecard.scores.service.ScoreService;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.idp.v1.ScoresApi;
import io.harness.spec.server.idp.v1.model.EntityScores;
import io.harness.spec.server.idp.v1.model.EntityScoresResponse;
import io.harness.spec.server.idp.v1.model.ScorecardFilter;
import io.harness.spec.server.idp.v1.model.ScorecardGraphSummaryInfo;
import io.harness.spec.server.idp.v1.model.ScorecardGraphSummaryInfoResponse;
import io.harness.spec.server.idp.v1.model.ScorecardRecalibrateIdentifiers;
import io.harness.spec.server.idp.v1.model.ScorecardRecalibrateInfo;
import io.harness.spec.server.idp.v1.model.ScorecardRecalibrateRequest;
import io.harness.spec.server.idp.v1.model.ScorecardRecalibrateResponse;
import io.harness.spec.server.idp.v1.model.ScorecardRecalibrateResponseV2;
import io.harness.spec.server.idp.v1.model.ScorecardScore;
import io.harness.spec.server.idp.v1.model.ScorecardScoreResponse;
import io.harness.spec.server.idp.v1.model.ScorecardSummaryInfo;
import io.harness.spec.server.idp.v1.model.ScorecardSummaryResponse;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @com.google.inject.Inject }))
@NextGenManagerAuth
@Slf4j
public class ScoreApiImpl implements ScoresApi {
  private ScoreService scoreService;
  private AsyncScoreComputationService asyncScoreComputationService;

  @Override
  public Response getAllScorecardSummary(String entityIdentifier, String harnessAccount) {
    try {
      List<ScorecardSummaryInfo> scorecardSummaryInfoList =
          scoreService.getScoresSummaryForAnEntity(harnessAccount, entityIdentifier);
      ScorecardSummaryResponse scorecardSummaryResponse = new ScorecardSummaryResponse();
      scorecardSummaryResponse.setScorecardsSummary(scorecardSummaryInfoList);
      return Response.status(Response.Status.OK).entity(scorecardSummaryResponse).build();
    } catch (Exception e) {
      log.error("Error in getting score summary for scorecards details for account - {} and entity - {},  error = {}",
          harnessAccount, entityIdentifier, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  @NGAccessControlCheck(resourceType = IDP_SCORECARD, permission = IDP_SCORECARD_EDIT)
  public Response scorecardRecalibrate(
      @Valid ScorecardRecalibrateRequest scorecardRecalibrateRequest, @AccountIdentifier String harnessAccount) {
    try {
      ScorecardSummaryInfo scorecardSummaryInfo = scoreService.getScorecardRecalibratedScoreInfoForAnEntityAndScorecard(
          harnessAccount, scorecardRecalibrateRequest.getIdentifiers().getEntityIdentifier(),
          scorecardRecalibrateRequest.getIdentifiers().getScorecardIdentifier());
      ScorecardRecalibrateResponse scorecardRecalibrateResponse = new ScorecardRecalibrateResponse();
      scorecardRecalibrateResponse.setRecalibratedScores(scorecardSummaryInfo);
      return Response.status(Response.Status.OK).entity(scorecardRecalibrateResponse).build();
    } catch (Exception e) {
      log.error(
          "Error in getting recalibrated score for scorecards details for account - {},  entity - {} and scorecard - {}, error = {}",
          harnessAccount, scorecardRecalibrateRequest.getIdentifiers().getEntityIdentifier(),
          scorecardRecalibrateRequest.getIdentifiers().getScorecardIdentifier(), e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response getScorecardsGraphsScoreSummary(
      String entityIdentifier, String harnessAccount, String scorecardIdentifier) {
    try {
      List<ScorecardGraphSummaryInfo> scorecardGraphSummaryInfos =
          scoreService.getScoresGraphSummaryForAnEntityAndScorecard(
              harnessAccount, entityIdentifier, scorecardIdentifier);
      ScorecardGraphSummaryInfoResponse scorecardGraphSummaryInfoResponse = new ScorecardGraphSummaryInfoResponse();
      scorecardGraphSummaryInfoResponse.setScorecardGraphSummary(scorecardGraphSummaryInfos);
      return Response.status(Response.Status.OK).entity(scorecardGraphSummaryInfoResponse).build();
    } catch (Exception e) {
      log.error(
          "Error in getting score graph summary for scorecards details for account - {},  entity - {} and scorecard - {},  error = {}",
          harnessAccount, entityIdentifier, scorecardIdentifier, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response getScorecardsScoresOverview(String entityIdentifier, String harnessAccount) {
    try {
      List<ScorecardScore> scorecardScores =
          scoreService.getScorecardScoreOverviewForAnEntity(harnessAccount, entityIdentifier);
      ScorecardScoreResponse scorecardScoreResponse = new ScorecardScoreResponse();
      int overallScore = 0;
      for (ScorecardScore scorecardScore : scorecardScores) {
        overallScore = overallScore + scorecardScore.getScore();
      }
      scorecardScoreResponse.setScorecardScores(scorecardScores);
      if (!scorecardScores.isEmpty()) {
        scorecardScoreResponse.setOverallScore(overallScore / scorecardScores.size());
      }
      return Response.status(Response.Status.OK).entity(scorecardScoreResponse).build();
    } catch (Exception e) {
      log.error("Error in getting scores overview for scorecards details for account - {},  entity - {} ,  error = {}",
          harnessAccount, entityIdentifier, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response getScoresRecalibrateStatus(@Valid ScorecardRecalibrateRequest body, String harnessAccount) {
    ScorecardRecalibrateIdentifiers identifiers = body.getIdentifiers();
    String scorecardIdentifier = identifiers.getScorecardIdentifier();
    String entityIdentifier = identifiers.getEntityIdentifier();
    try {
      ScorecardRecalibrateInfo recalibrateInfo =
          asyncScoreComputationService.getRecalibrateInfo(harnessAccount, scorecardIdentifier, entityIdentifier);
      ScorecardRecalibrateResponseV2 responseV2 = new ScorecardRecalibrateResponseV2();
      responseV2.setInfo(recalibrateInfo);
      return Response.status(Response.Status.OK).entity(responseV2).build();
    } catch (Exception e) {
      log.error("Error in getting recalibrate status for account - {}, scorecard - {}, entity - {},  error = {}",
          harnessAccount, scorecardIdentifier, entityIdentifier, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }

  @Override
  public Response getAggregatedScores(@Valid ScorecardFilter body, String harnessAccount) {
    try {
      List<EntityScores> entityScores = scoreService.getEntityScores(harnessAccount, body);
      List<EntityScoresResponse> entityScoresResponse = new ArrayList<>();
      entityScores.forEach(entityScore -> entityScoresResponse.add(new EntityScoresResponse().entity(entityScore)));
      return Response.status(Response.Status.OK).entity(entityScoresResponse).build();
    } catch (Exception e) {
      log.error("Error in getting entity scores for account - {},  error = {}", harnessAccount, e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(ResponseMessage.builder().message(e.getMessage()).build())
          .build();
    }
  }
}