/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipelinestage.helper;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipelinestage.PipelineStageStepParameters;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.pipelinestage.PipelineStageConfig;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PipelineStageHelper {
  @Inject private PMSPipelineTemplateHelper pmsPipelineTemplateHelper;

  public void validateNestedChainedPipeline(PipelineEntity entity) {
    TemplateMergeResponseDTO templateMergeResponseDTO = pmsPipelineTemplateHelper.resolveTemplateRefsInPipeline(entity);

    containsPipelineStage(templateMergeResponseDTO.getMergedPipelineYaml());
  }

  private void containsPipelineStage(String yaml) {
    try {
      YamlField pipelineYamlField = YamlUtils.readTree(yaml);
      List<YamlNode> stages = pipelineYamlField.getNode()
                                  .getField(YAMLFieldNameConstants.PIPELINE)
                                  .getNode()
                                  .getField(YAMLFieldNameConstants.STAGES)
                                  .getNode()
                                  .asArray();
      for (YamlNode yamlNode : stages) {
        if (yamlNode.getField(YAMLFieldNameConstants.STAGE) != null) {
          containsPipelineStageInStageNode(yamlNode);
        } else if (yamlNode.getField(YAMLFieldNameConstants.PARALLEL) != null) {
          containsPipelineStageInParallelNode(yamlNode);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void containsPipelineStageInParallelNode(YamlNode yamlNode) {
    List<YamlNode> stageInParallel = yamlNode.getField(YAMLFieldNameConstants.PARALLEL).getNode().asArray();
    for (YamlNode stage : stageInParallel) {
      if (stage.getField(YAMLFieldNameConstants.STAGE) != null) {
        containsPipelineStageInStageNode(stage);
      } else {
        throw new InvalidRequestException("Parallel stages contains entity other than stage");
      }
    }
  }

  private void containsPipelineStageInStageNode(YamlNode yamlNode) {
    if (yamlNode.getField(YAMLFieldNameConstants.STAGE) != null
        && yamlNode.getField(YAMLFieldNameConstants.STAGE).getNode() != null
        && yamlNode.getField(YAMLFieldNameConstants.STAGE).getNode().getType().equals("Pipeline")) {
      throw new InvalidRequestException("Nested pipeline is not supported");
    }
  }

  public void validateResource(
      AccessControlClient accessControlClient, Ambiance ambiance, PipelineStageStepParameters stepParameters) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(ambiance.getSetupAbstractions().get("accountId"),
                                                  stepParameters.getOrg(), stepParameters.getProject()),
        Resource.of("PIPELINE", stepParameters.getPipeline()), PipelineRbacPermissions.PIPELINE_EXECUTE);
  }

  public String getInputSetYaml(PipelineStageConfig config) {
    String inputSetYaml = "";
    if (config.getPipelineInputs() != null && config.getPipelineInputs().getValue() != null) {
      Map<String, Object> inputYaml = new HashMap<>();
      inputYaml.put(YAMLFieldNameConstants.PIPELINE, config.getPipelineInputs().getValue());
      try {
        YamlField inputYamlField = YamlUtils.readTree(YamlPipelineUtils.writeYamlString(inputYaml));
        JsonNode inputJsonNode = inputYamlField.getNode().getCurrJsonNode();
        YamlUtils.removeUuid(inputJsonNode);
        inputSetYaml = inputJsonNode.toString();
      } catch (IOException e) {
        throw new InvalidRequestException(
            String.format("Pipeline Inputs is invalid for child % s ", config.getPipeline()));
      }
    }
    return inputSetYaml;
  }
}
