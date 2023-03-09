/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.plan.IdentityPlanNode;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionMode;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.pipeline.service.PipelineMetadataService;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class RollbackModeExecutionHelper {
  NodeExecutionService nodeExecutionService;
  PipelineMetadataService pipelineMetadataService;
  PrincipalInfoHelper principalInfoHelper;

  public ExecutionMetadata transformExecutionMetadata(ExecutionMetadata executionMetadata, String planExecutionID,
      ExecutionTriggerInfo triggerInfo, String accountId, String orgIdentifier, String projectIdentifier) {
    return executionMetadata.toBuilder()
        .setExecutionUuid(planExecutionID)
        .setTriggerInfo(triggerInfo)
        .setRunSequence(pipelineMetadataService.incrementExecutionCounter(
            accountId, orgIdentifier, projectIdentifier, executionMetadata.getPipelineIdentifier()))
        .setPrincipalInfo(principalInfoHelper.getPrincipalInfoFromSecurityContext())
        .setExecutionMode(ExecutionMode.POST_EXECUTION_ROLLBACK)
        .build();
  }

  public PlanExecutionMetadata transformPlanExecutionMetadata(
      PlanExecutionMetadata planExecutionMetadata, String planExecutionID) {
    return planExecutionMetadata.withPlanExecutionId(planExecutionID)
        .withProcessedYaml(transformProcessedYaml(planExecutionMetadata.getProcessedYaml()))
        .withUuid(null); // this uuid is the mongo uuid. It is being set as null so that when this Plan Execution
                         // Metadata is saved later on in the execution, a new object is stored rather than replacing
                         // the Metadata for the original execution
  }

  /**
   * This is to reverse the stages in the processed yaml
   * Original->
   * pipeline:
   *   stages:
   *   - stage:
   *       identifier: s1
   *  - stage:
   *       identifier: s2
   * Transformed->
   * pipeline:
   *   stages:
   *   - stage:
   *       identifier: s2
   *   - stage:
   *       identifier: s1
   */
  private String transformProcessedYaml(String processedYaml) {
    JsonNode pipelineNode;
    try {
      pipelineNode = YamlUtils.readTree(processedYaml).getNode().getCurrJsonNode();
    } catch (IOException e) {
      throw new UnexpectedException("Unable to transform processed YAML while executing in Rollback Mode");
    }
    ObjectNode pipelineInnerNode = (ObjectNode) pipelineNode.get(YAMLFieldNameConstants.PIPELINE);
    ArrayNode stagesList = (ArrayNode) pipelineInnerNode.get(YAMLFieldNameConstants.STAGES);
    ArrayNode reversedStages = stagesList.deepCopy().removeAll();
    int numStages = stagesList.size();
    for (int i = numStages - 1; i >= 0; i--) {
      reversedStages.add(stagesList.get(i));
    }
    pipelineInnerNode.set(YAMLFieldNameConstants.STAGES, reversedStages);
    return YamlUtils.write(pipelineNode).replace("---\n", "");
  }

  /**
   * Step1: Initialise a map from planNodeIDs to Plan Nodes
   * Step2: fetch all node executions of previous execution that are the descendants of any stage
   * Step3: create identity plan nodes for all node executions that are the descendants of any stage, and add them to
   * the map
   * Step4: Go through `createdPlan`. If any Plan node has AdvisorObtainments for POST_EXECUTION_ROLLBACK Mode, add them
   * to the corresponding Identity Plan Node in the initialised map
   * Step5: From `createdPlan`, pick out all nodes that are not a descendants of some stage, and add them to the
   * initialised map.
   * Step6: For all IDs in `nodeIDsToPreserve`, remove the Identity Plan Nodes in the map, and put the
   * Plan nodes from `createdPlan`
   */
  public Plan transformPlanForRollbackMode(
      Plan createdPlan, String previousExecutionId, List<String> nodeIDsToPreserve) {
    // steps 1, 2, and 3
    Map<String, Node> planNodeIDToUpdatedPlanNodes =
        buildIdentityNodes(previousExecutionId, createdPlan.getPlanNodes());

    // step 4
    addAdvisorsToIdentityNodes(createdPlan, planNodeIDToUpdatedPlanNodes);

    // steps 5 and 6
    addPreservedPlanNodes(createdPlan, nodeIDsToPreserve, planNodeIDToUpdatedPlanNodes);

    return Plan.builder()
        .uuid(createdPlan.getUuid())
        .planNodes(planNodeIDToUpdatedPlanNodes.values())
        .startingNodeId(createdPlan.getStartingNodeId())
        .setupAbstractions(createdPlan.getSetupAbstractions())
        .graphLayoutInfo(createdPlan.getGraphLayoutInfo())
        .validUntil(createdPlan.getValidUntil())
        .valid(createdPlan.isValid())
        .errorResponse(createdPlan.getErrorResponse())
        .build();
  }

  Map<String, Node> buildIdentityNodes(String previousExecutionId, List<Node> createdPlanNodes) {
    Map<String, Node> planNodeIDToUpdatedNodes = new HashMap<>();

    List<NodeExecution> nodeExecutions = getNodeExecutionsWithOnlyRequiredFields(previousExecutionId, createdPlanNodes);
    for (NodeExecution nodeExecution : nodeExecutions) {
      Node planNode = nodeExecution.getNode();
      if (planNode.getStepType().getStepCategory() == StepCategory.STAGE) {
        continue;
      }
      IdentityPlanNode identityPlanNode = IdentityPlanNode.mapPlanNodeToIdentityNode(
          nodeExecution.getNode(), nodeExecution.getStepType(), nodeExecution.getUuid(), true);
      planNodeIDToUpdatedNodes.put(planNode.getUuid(), identityPlanNode);
    }
    return planNodeIDToUpdatedNodes;
  }

  List<NodeExecution> getNodeExecutionsWithOnlyRequiredFields(String previousExecutionId, List<Node> createdPlanNodes) {
    List<String> stageFQNs = createdPlanNodes.stream()
                                 .filter(n -> n.getStepCategory() == StepCategory.STAGE)
                                 .map(Node::getStageFqn)
                                 .collect(Collectors.toList());
    List<String> requiredFields =
        Arrays.asList(NodeExecutionKeys.planNode, NodeExecutionKeys.stepType, NodeExecutionKeys.uuid);
    return nodeExecutionService.fetchNodeExecutionsForGivenStageFQNs(previousExecutionId, stageFQNs, requiredFields);
  }

  void addAdvisorsToIdentityNodes(Plan createdPlan, Map<String, Node> planNodeIDToUpdatedPlanNodes) {
    for (Node planNode : createdPlan.getPlanNodes()) {
      if (EmptyPredicate.isEmpty(planNode.getAdvisorObtainmentsForExecutionMode())) {
        continue;
      }
      List<AdviserObtainment> adviserObtainments =
          planNode.getAdvisorObtainmentsForExecutionMode().get(ExecutionMode.POST_EXECUTION_ROLLBACK);
      if (EmptyPredicate.isNotEmpty(adviserObtainments)) {
        IdentityPlanNode updatedNode = (IdentityPlanNode) planNodeIDToUpdatedPlanNodes.get(planNode.getUuid());
        planNodeIDToUpdatedPlanNodes.put(planNode.getUuid(), updatedNode.withAdviserObtainments(adviserObtainments));
      }
    }
  }

  void addPreservedPlanNodes(
      Plan createdPlan, List<String> nodeIDsToPreserve, Map<String, Node> planNodeIDToUpdatedPlanNodes) {
    for (Node planNode : createdPlan.getPlanNodes()) {
      if (nodeIDsToPreserve.contains(planNode.getUuid()) || isStageOrAncestorOfSomeStage(planNode)) {
        planNodeIDToUpdatedPlanNodes.put(planNode.getUuid(), planNode);
      }
    }
  }

  boolean isStageOrAncestorOfSomeStage(Node planNode) {
    StepCategory stepCategory = planNode.getStepCategory();
    return Arrays.asList(StepCategory.PIPELINE, StepCategory.STAGES, StepCategory.STAGE).contains(stepCategory);
  }
}