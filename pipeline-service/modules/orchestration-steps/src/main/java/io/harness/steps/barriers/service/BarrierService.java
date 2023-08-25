/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.barriers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.service.visitor.BarrierVisitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
public interface BarrierService {
  BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance);
  List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances);
  BarrierExecutionInstance get(String barrierUuid);
  BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance);
  BarrierExecutionInstance updateState(String uuid, State state);
  List<BarrierExecutionInstance> updatePosition(String planExecutionId,
      BarrierPositionInfo.BarrierPosition.BarrierPositionType positionType, String positionSetupId,
      String positionExecutionId, String stageExecutionId, String stepGroupExecutionId);
  BarrierExecutionInstance findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
  BarrierExecutionInstance findByPlanNodeIdAndPlanExecutionId(String planNodeId, String planExecutionId);
  List<BarrierExecutionInstance> findByStageIdentifierAndPlanExecutionIdAnsStateIn(
      String stageIdentifier, String planExecutionId, Set<State> stateSet);
  BarrierExecutionInstance findByIdentifierAndPlanExecutionIdAndStrategyExecutionId(String identifier, String planExecutionId, String strategyExecutionId);
  BarrierVisitor getBarrierInfo(String yaml);
//  Map<String, List<BarrierPositionInfo.BarrierPosition>> getBarrierPositionInfoList(String yaml);

  /**
   * Deletes barrierInstances for given planExecutionIds
   * Uses - planExecutionId_barrierState_stagesIdentifier_idx
   * @param planExecutionIds
   */
  void deleteAllForGivenPlanExecutionId(Set<String> planExecutionIds);
  void upsert(BarrierExecutionInstance barrierExecutionInstance);
  void updateBarrierWithinStrategy(String barrierIdentifier, String planExecutionId, String strategySetupId, List<BarrierPositionInfo.BarrierPosition> barrierPositions, String strategyExecutionId);
  List<BarrierExecutionInstance> findManyByPlanExecutionIdAndSetupInfo_StrategySetupId(String planExecutionId, String strategySetupId);
  void updateManyPlanExecutionId(String setupId, String executionId);
}
