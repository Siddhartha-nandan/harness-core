/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.PipelineVersion;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.harness.steps.barriers.service.visitor.BarrierVisitor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierInitializer implements OrchestrationStartObserver {
  @Inject private BarrierService barrierService;

  @Override
  public void onStart(OrchestrationStartInfo orchestrationStartInfo) {
//    String pipelineSetupId = orchestrationStartInfo.getAmbiance().getLevelsList().get(0).getSetupId();
//    barrierService.updateManyPlanExecutionId(pipelineSetupId, orchestrationStartInfo.getPlanExecutionId());
    boolean a = true;
    if (a) {
      return;
    }

    String version = AmbianceUtils.getPipelineVersion(orchestrationStartInfo.getAmbiance());
    String planExecutionId = orchestrationStartInfo.getPlanExecutionId();
    PlanExecutionMetadata planExecutionMetadata = orchestrationStartInfo.getPlanExecutionMetadata();
    try {
      switch (version) {
        case PipelineVersion.V1:
          // TODO: Barrier support
          break;
        case PipelineVersion.V0:
          BarrierVisitor barriersInfo = barrierService.getBarrierInfo(planExecutionMetadata.getProcessedYaml());
          Map<String, BarrierSetupInfo> barrierIdentifierSetupInfoMap = new ArrayList<>(barriersInfo.getBarrierIdentifierMap().values())
                  .stream()
                  .collect(Collectors.toMap(BarrierSetupInfo::getIdentifier, Function.identity()));

          Map<String, List<BarrierPositionInfo.BarrierPosition>> barrierPositionInfoMap = barriersInfo.getBarrierPositionInfoMap();

          List<BarrierExecutionInstance> barriers =
              barrierPositionInfoMap.entrySet()
                  .stream()
                  .filter(entry -> !entry.getValue().isEmpty())
                  // Filter out barriers that are within a "strategy" node.
                  .filter(entry -> barrierIdentifierSetupInfoMap.get(entry.getKey()).getStrategySetupId() == null)
                  .map(entry
                      -> BarrierExecutionInstance.builder()
                             .uuid(generateUuid())
                             .setupInfo(barrierIdentifierSetupInfoMap.get(entry.getKey()))
                             .positionInfo(BarrierPositionInfo.builder()
                                               .planExecutionId(planExecutionId)
                                               .barrierPositionList(entry.getValue())
                                               .build())
                             .name(barrierIdentifierSetupInfoMap.get(entry.getKey()).getName())
                             .barrierState(Barrier.State.STANDING)
                             .identifier(entry.getKey())
                             .planExecutionId(planExecutionId)
                             .build())
                  .collect(Collectors.toList());

          barrierService.saveAll(barriers);
          break;
        default:
          throw new IllegalStateException("version not supported");
      }
    } catch (Exception e) {
      log.error("Barrier initialization failed for planExecutionId: [{}]", planExecutionId);
      throw e;
    }
  }
}
