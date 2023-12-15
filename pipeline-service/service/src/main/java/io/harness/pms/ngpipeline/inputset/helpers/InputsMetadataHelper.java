/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.InputsMetadata;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.merger.helpers.InputSetTemplateHelper;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.ngpipeline.inputset.beans.dto.InputSetMetadataDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.service.InputSetValidationHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipeline.service.PipelineCRUDErrorResponse;
import io.harness.pms.plan.execution.StagesExecutionHelper;
import io.harness.pms.stages.StagesExpressionExtractor;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.utils.PipelineGitXHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.gitcaching.GitCachingConstants.BOOLEAN_FALSE_VALUE;
import static io.harness.pms.merger.helpers.InputSetMergeHelper.mergeInputSetIntoPipelineForGivenStages;
import static io.harness.pms.merger.helpers.InputSetMergeHelper.mergeInputSetsForGivenStages;
import static io.harness.pms.merger.helpers.InputSetTemplateHelper.*;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InputsMetadataHelper {
  private static final Map<String, String> entityKeyToRefKeyMap = Map.of(YAMLFieldNameConstants.SERVICE, YAMLFieldNameConstants.SERVICE_REF);


  public void getRuntimeInputsMetadata(String accountId, String orgIdentifier, String projectIdentifier, String pipelineYaml, String runtimeInputFormYaml) {
    try {
      Map<String, Map<String, String>> entityTypeToData = new HashMap<>();
      Set<FQN> fqnsWithRawInputValue = RuntimeInputFormHelper.fetchFQNsWithRawInputFieldValue(pipelineYaml);
      for (FQN fqn : fqnsWithRawInputValue) {
        for (String entityKey : entityKeyToRefKeyMap.keySet()) {
          FQN baseFQN = fqn.getBaseFQNTillOneOfGivenFields(Set.of(entityKey));
          if (baseFQN != null) {
            if (!entityTypeToData.containsKey(entityKey)) {
              entityTypeToData.put(entityKey, new HashMap<>());
            }
            String entityRef = YamlUtils.readTree(YamlUtils.getInnerYamlFromFQN(pipelineYaml, baseFQN)).getNode().getCurrJsonNode().get(entityKeyToRefKeyMap.get(entityKey)).toString();
            String innerInputFormYaml = YamlUtils.getInnerYamlFromFQNExpression(runtimeInputFormYaml, baseFQN.getExpressionFqn());
            entityTypeToData.get(entityKey).put(entityRef, innerInputFormYaml);
            break;
          }
        }
      }
      log.info("ok");
    } catch (Exception ex) {
      log.error("PAU", ex);
    }
  }
}
