/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.AbstractPmsStagePlanCreator;
import io.harness.plancreator.stages.PmsAbstractStageNode;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.strategy.StrategyUtils;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.approval.stage.ApprovalStageSpecParameters;
import io.harness.steps.approval.stage.ApprovalStageStep;
import io.harness.utils.CommonPlanCreatorUtils;
import io.harness.utils.PlanCreatorUtilsCommon;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ApprovalStagePlanCreatorV2 extends AbstractPmsStagePlanCreator<PmsAbstractStageNode> {
  @Inject KryoSerializer kryoSerializer;

  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Approval");
  }

  @Override
  public StepType getStepType(PmsAbstractStageNode stageElementConfig) {
    return ApprovalStageStep.STEP_TYPE;
  }

  @Override
  public SpecParameters getSpecParameters(
      String childNodeId, PlanCreationContext ctx, PmsAbstractStageNode stageElementConfig) {
    return ApprovalStageSpecParameters.getStepParameters(childNodeId);
  }

  @Override
  public Class<PmsAbstractStageNode> getFieldClass() {
    return PmsAbstractStageNode.class;
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, PmsAbstractStageNode field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();
    Map<String, ByteString> metadataMap = new HashMap<>();

    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

    // Add dependency for execution
    YamlField executionField = specField.getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (executionField == null) {
      throw new InvalidRequestException("Execution section is required in Approval stage");
    }
    dependenciesNodeMap.put(executionField.getNode().getUuid(), executionField);

    // Adding Spec node
    PlanNode specPlanNode =
        CommonPlanCreatorUtils.getSpecPlanNode(specField.getNode().getUuid(), executionField.getNode().getUuid());
    planCreationResponseMap.put(
        specPlanNode.getUuid(), PlanCreationResponse.builder().node(specPlanNode.getUuid(), specPlanNode).build());

    addStrategyFieldDependencyIfPresent(ctx, field, dependenciesNodeMap, metadataMap);

    planCreationResponseMap.put(executionField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(
                DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                    .toBuilder()
                    .putDependencyMetadata(field.getUuid(), Dependency.newBuilder().putAllMetadata(metadataMap).build())
                    .putDependencyMetadata(executionField.getNode().getUuid(),
                        Dependency.newBuilder().setParentInfo(generateParentInfo(ctx, field)).build())
                    .build())
            .build());
    return planCreationResponseMap;
  }

  private HarnessStruct generateParentInfo(PlanCreationContext ctx, PmsAbstractStageNode stageNode) {
    YamlField field = ctx.getCurrentField();
    HarnessStruct.Builder parentInfo = HarnessStruct.newBuilder();
    parentInfo.putData(PlanCreatorConstants.STAGE_ID,
        HarnessValue.newBuilder().setStringValue(getFinalPlanNodeId(ctx, stageNode)).build());
    if (StrategyUtils.isWrappedUnderStrategy(field)) {
      String strategyId = stageNode.getUuid();
      parentInfo.putData(
          PlanCreatorConstants.NEAREST_STRATEGY_ID, HarnessValue.newBuilder().setStringValue(strategyId).build());
      parentInfo.putData(PlanCreatorConstants.ALL_STRATEGY_IDS,
          PlanCreatorUtilsCommon.appendToParentInfoList(PlanCreatorConstants.ALL_STRATEGY_IDS, strategyId, ctx));
      parentInfo.putData(PlanCreatorConstants.STRATEGY_NODE_TYPE,
          HarnessValue.newBuilder().setStringValue(YAMLFieldNameConstants.STAGE).build());
    }
    return parentInfo.build();
  }
}
