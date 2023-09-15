/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage;

import static io.harness.pms.yaml.YAMLFieldNameConstants.CUSTOM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.environment.helper.EnvironmentPlanCreatorHelper;
import io.harness.cdng.environment.yaml.EnvironmentYamlV2;
import io.harness.cdng.pipeline.beans.CustomStageSpecParams;
import io.harness.cdng.pipeline.steps.CustomStageStep;
import io.harness.cdng.service.steps.helpers.beans.ServiceStepV3Parameters;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.AbstractStagePlanCreator;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.plancreator.strategy.StrategyUtils;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.SdkCoreStepUtils;
import io.harness.when.utils.RunInfoUtils;
import io.harness.yaml.utils.NGVariablesUtils;

<<<<<<< HEAD
<<<<<<< HEAD
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
=======
>>>>>>> 5234b436ce9 ([feat]: [CDS-78377] variables & variable overrides support for custom stage env)
=======
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
>>>>>>> 26a6a7185ba ([feat]: [CDS-78377] add variables overrides for custom stage env. step)
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class CustomStagePlanCreator extends AbstractStagePlanCreator<CustomStageNode> {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StagePlanCreatorHelper stagePlanCreatorHelper;

  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton(CUSTOM);
  }

  @Override
  public StepType getStepType(CustomStageNode stageElementConfig) {
    return CustomStageStep.STEP_TYPE;
  }

  @Override
  public SpecParameters getSpecParameters(
      String childNodeId, PlanCreationContext ctx, CustomStageNode stageElementConfig) {
    return CustomStageSpecParams.getStepParameters(childNodeId);
  }

  @Override
  public Class<CustomStageNode> getFieldClass() {
    return CustomStageNode.class;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, CustomStageNode stageNode, List<String> childrenNodeIds) {
    stageNode.setIdentifier(StrategyUtils.getIdentifierWithExpression(ctx, stageNode.getIdentifier()));
    stageNode.setName(StrategyUtils.getIdentifierWithExpression(ctx, stageNode.getName()));
    StageElementParametersBuilder stageParameters = getStageParameters(stageNode);
    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));
    stageParameters.specConfig(getSpecParameters(specField.getNode().getUuid(), ctx, stageNode));
    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(StrategyUtils.getSwappedPlanNodeId(ctx, stageNode.getUuid()))
            .name(stageNode.getName())
            .identifier(stageNode.getIdentifier())
            .group(StepOutcomeGroup.STAGE.name())
            .stepParameters(stageParameters.build())
            .stepType(getStepType(stageNode))
            .skipCondition(SkipInfoUtils.getSkipCondition(stageNode.getSkipCondition()))
            .whenCondition(RunInfoUtils.getRunConditionForStage(stageNode.getWhen()))
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField(), ctx.getDependency()));
    if (!EmptyPredicate.isEmpty(ctx.getExecutionInputTemplate())) {
      builder.executionInputTemplate(ctx.getExecutionInputTemplate());
    }
    return builder.build();
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, CustomStageNode field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();
    Map<String, ByteString> metadataMap = new HashMap<>();

    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

    // Add dependency for execution
    YamlField executionField = specField.getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (executionField == null) {
      throw new InvalidRequestException("Execution section is required in Custom stage");
    }
    dependenciesNodeMap.put(executionField.getNode().getUuid(), executionField);
    addStrategyFieldDependencyIfPresent(ctx, field, dependenciesNodeMap, metadataMap);

    planCreationResponseMap.put(executionField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(
                DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                    .toBuilder()
                    .putDependencyMetadata(field.getUuid(), Dependency.newBuilder().putAllMetadata(metadataMap).build())
                    .build())
            .build());

    String executionFieldUuid = executionField.getNode().getUuid();
    String specNextNodeUuid = executionFieldUuid;

    EnvironmentYamlV2 finalEnvironmentYamlV2 = field.getCustomStageConfig().getEnvironment();
    boolean envNodeExists = finalEnvironmentYamlV2 != null && finalEnvironmentYamlV2.getEnvironmentRef() != null;

    String envNodeUuid;
    // Adding Env & Infra nodes
    if (envNodeExists) {
      String infraNodeUuid = null;
      if (finalEnvironmentYamlV2.getInfrastructureDefinition() != null) {
        infraNodeUuid = addInfraNode(planCreationResponseMap, finalEnvironmentYamlV2, specField, ctx);
      }
      String envNextNodeUuid = EmptyPredicate.isNotEmpty(infraNodeUuid) ? infraNodeUuid : executionFieldUuid;
      envNodeUuid = addEnvNode(planCreationResponseMap, envNextNodeUuid);
      specNextNodeUuid = envNodeUuid;
    }

    // Adding Spec node
    PlanCreationResponse specPlanCreationResponse = prepareDependencyForSpecNode(specField, specNextNodeUuid);
    planCreationResponseMap.put(specField.getNode().getUuid(), specPlanCreationResponse);

<<<<<<< HEAD
=======
    // Adding Env node
    if (envNodeExists) {
      String envNextNodeUuid = executionFieldUuid;
      final ServiceStepV3Parameters stepParameters = ServiceStepV3Parameters.builder()
                                                         .envRef(finalEnvironmentYamlV2.getEnvironmentRef())
                                                         .envInputs(finalEnvironmentYamlV2.getEnvironmentInputs())
                                                         .build();

      if (ParameterField.isNotNull(finalEnvironmentYamlV2.getInfrastructureDefinition())) {
        ParameterField<String> infraRef =
            finalEnvironmentYamlV2.getInfrastructureDefinition().getValue().getIdentifier();
        stepParameters.setInfraId(infraRef);

        PlanNode infraNode = InfrastructurePmsPlanCreator.getInfraTaskExecutableStepV2PlanNode(
            finalEnvironmentYamlV2, null, ParameterField.createValueField(false));
        planCreationResponseMap.put(infraNode.getUuid(), PlanCreationResponse.builder().planNode(infraNode).build());
        stepParameters.setChildrenNodeIds(Collections.singletonList(infraNode.getUuid()));
      }

      ByteString advisorParameters = ByteString.copyFrom(
          kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(envNextNodeUuid).build()));
      final PlanNode envNode =
          EnvironmentPlanCreatorHelper.getPlanNodeForCustomStage(envNodeUuid, stepParameters, advisorParameters);
      planCreationResponseMap.put(envNode.getUuid(), PlanCreationResponse.builder().planNode(envNode).build());
    }

>>>>>>> 5234b436ce9 ([feat]: [CDS-78377] variables & variable overrides support for custom stage env)
    return planCreationResponseMap;
  }

  private String addInfraNode(LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap,
      EnvironmentYamlV2 finalEnvironmentYamlV2, YamlField specField, PlanCreationContext ctx) {
    final boolean isProjectScopedResourceConstraintQueue =
        stagePlanCreatorHelper.isProjectScopedResourceConstraintQueueByFFOrSetting(ctx);
    List<AdviserObtainment> adviserObtainments =
        stagePlanCreatorHelper.addResourceConstraintDependencyWithWhenCondition(
            planCreationResponseMap, specField, ctx, isProjectScopedResourceConstraintQueue);

    PlanNode infraNode = InfrastructurePmsPlanCreator.getCustomStageInfraTaskExecutableStepV2PlanNode(
        finalEnvironmentYamlV2, adviserObtainments);
    String infraNodeUuid = infraNode.getUuid();

    planCreationResponseMap.put(infraNodeUuid, PlanCreationResponse.builder().planNode(infraNode).build());

    return infraNodeUuid;
  }

  private String addEnvNode(
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, String envNextNodeUuid) {
    String envNodeUuid = UUIDGenerator.generateUuid();
    ByteString advisorParameters = ByteString.copyFrom(
        kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(envNextNodeUuid).build()));

    final CustomStageEnvironmentStepParameters stepParameters =
        CustomStageEnvironmentStepParameters.builder().childrenNodeIds(new ArrayList<>()).build();

    final PlanNode envNode =
        EnvironmentPlanCreatorHelper.getEnvPlanNodeForCustomStage(envNodeUuid, stepParameters, advisorParameters);

    planCreationResponseMap.put(envNode.getUuid(), PlanCreationResponse.builder().planNode(envNode).build());
    return envNodeUuid;
  }

  private PlanCreationResponse prepareDependencyForSpecNode(YamlField specField, String uuid) {
    Map<String, YamlField> specDependencyMap = new HashMap<>();
    specDependencyMap.put(specField.getNode().getUuid(), specField);
    Map<String, ByteString> specDependencyMetadataMap = new HashMap<>();
    specDependencyMetadataMap.put(
        YAMLFieldNameConstants.CHILD_NODE_OF_SPEC, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(uuid)));
    return PlanCreationResponse.builder()
        .dependencies(DependenciesUtils.toDependenciesProto(specDependencyMap)
                          .toBuilder()
                          .putDependencyMetadata(specField.getNode().getUuid(),
                              Dependency.newBuilder().putAllMetadata(specDependencyMetadataMap).build())
                          .build())
        .build();
  }

  public StageElementParametersBuilder getStageParameters(CustomStageNode stageNode) {
    TagUtils.removeUuidFromTags(stageNode.getTags());

    StageElementParametersBuilder stageBuilder = StageElementParameters.builder();
    stageBuilder.name(stageNode.getName());
    stageBuilder.identifier(stageNode.getIdentifier());
    stageBuilder.description(SdkCoreStepUtils.getParameterFieldHandleValueNull(stageNode.getDescription()));
    stageBuilder.failureStrategies(
        stageNode.getFailureStrategies() != null ? stageNode.getFailureStrategies().getValue() : null);
    stageBuilder.skipCondition(stageNode.getSkipCondition());
    stageBuilder.when(stageNode.getWhen() != null ? stageNode.getWhen().getValue() : null);
    stageBuilder.type(stageNode.getType());
    stageBuilder.uuid(stageNode.getUuid());
    stageBuilder.variables(
        ParameterField.createValueField(NGVariablesUtils.getMapOfVariables(stageNode.getVariables())));
    stageBuilder.tags(CollectionUtils.emptyIfNull(stageNode.getTags()));

    return stageBuilder;
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V0);
  }
}
