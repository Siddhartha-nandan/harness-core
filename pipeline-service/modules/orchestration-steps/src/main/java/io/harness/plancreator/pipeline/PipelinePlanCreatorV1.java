/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.pipeline;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.common.pipeline.PipelineSetupStepParameters;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelinePlanCreatorV1 extends ChildrenPlanCreator<YamlField> {
  @Override
  public String getStartingNodeId(YamlField field) {
    return field.getUuid();
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, YamlField config) {
    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependencies = new HashMap<>();
    YamlField specNode = Preconditions.checkNotNull(config.getNode().getField(YAMLFieldNameConstants.SPEC));
    if (specNode.getNode() == null) {
      return responseMap;
    }
    dependencies.put(specNode.getNode().getUuid(), specNode);
    responseMap.put(specNode.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(dependencies)
                              .toBuilder()
                              .putDependencyMetadata(specNode.getUuid(),
                                  Dependency.newBuilder()
                                      .setNodeMetadata(
                                          HarnessStruct.newBuilder().putData(PlanCreatorConstants.SET_STARTING_NODE_ID,
                                              HarnessValue.newBuilder().setBoolValue(true).build()))
                                      .build())
                              .build())
            .build());
    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, YamlField config, List<String> childrenNodeIds) {
    StepParameters stepParameters =
        PipelineSetupStepParameters.newBuilder().childNodeID(childrenNodeIds.get(0)).build();

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(config.getUuid())
            .identifier(config.getId())
            .stepType(PipelineSetupStep.STEP_TYPE)
            .group(StepOutcomeGroup.PIPELINE.name())
            .name(config.getName())
            .skipUnresolvedExpressionsCheck(true)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .skipExpressionChain(false);

    return planNodeBuilder.build();
  }

  @Override
  public YamlField getFieldObject(YamlField field) {
    return field;
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V1);
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.PIPELINE, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
