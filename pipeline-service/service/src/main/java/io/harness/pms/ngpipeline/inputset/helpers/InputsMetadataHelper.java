/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.InputsMetadata;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.pms.contracts.inputmetadata.InputsMetadataProto;
import io.harness.pms.contracts.inputmetadata.InputsMetadataRequestMetadata;
import io.harness.pms.contracts.inputmetadata.InputsMetadataResponseProto;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.governance.JsonExpander;
import io.harness.pms.inputset.inputmetadata.InputsMetadataGenerator;
import io.harness.pms.inputset.inputmetadata.InputsMetadataRequest;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.merger.helpers.InputSetTemplateHelper;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.merger.helpers.YamlSubMapExtractor;
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
import io.harness.pms.sdk.core.inputmetadata.InputsMetadataResponse;
import io.harness.pms.stages.StagesExpressionExtractor;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.utils.PipelineGitXHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InputsMetadataHelper {
  private final InputsMetadataGenerator inputsMetadataGenerator;
  private static final Map<String, ModuleType> entityKeyToModule =
      Map.of(YAMLFieldNameConstants.SERVICE, ModuleType.CD);
  private static final Map<String, String> entityKeyToRefKeyMap =
      Map.of(YAMLFieldNameConstants.SERVICE, YAMLFieldNameConstants.SERVICE_REF);

  public Map<String, InputsMetadata> getRuntimeInputsMetadata(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String pipelineYaml, String runtimeInputFormYaml) {
    Map<String, InputsMetadata> inputsMetadata = new HashMap<>();
    try {
      YamlConfig pipelineYamlConfig = new YamlConfig(pipelineYaml);
      YamlConfig runtimeInputFormYamlConfig = new YamlConfig(runtimeInputFormYaml);
      Set<InputsMetadataRequest> inputsMetadataRequests = new HashSet<>();
      Set<FQN> fqnsWithRawInputValue = RuntimeInputFormHelper.fetchFQNsWithRawInputFieldValue(pipelineYaml);
      for (FQN fqn : fqnsWithRawInputValue) {
        for (String entityKey : entityKeyToRefKeyMap.keySet()) {
          FQN baseFQN = fqn.getBaseFQNTillOneOfGivenFields(Set.of(entityKey));
          if (baseFQN != null) {
            String entityRef = getEntityRef(pipelineYamlConfig, baseFQN, entityKeyToRefKeyMap.get(entityKey));
            String innerInputFormYaml = getInnerYamlFromFQN(runtimeInputFormYamlConfig, baseFQN);
            inputsMetadataRequests.add(InputsMetadataRequest.builder()
                                           .module(entityKeyToModule.get(entityKey))
                                           .entityId(entityRef)
                                           .entityType(entityKey)
                                           .inputFormYaml(innerInputFormYaml)
                                           .fqn(baseFQN.getExpressionFqn())
                                           .build());
            break;
          }
        }
      }
      if (!inputsMetadataRequests.isEmpty()) {
        InputsMetadataRequestMetadata requestMetadata = InputsMetadataRequestMetadata.newBuilder()
                                                            .setAccountId(accountId)
                                                            .setOrgId(orgIdentifier)
                                                            .setProjectId(projectIdentifier)
                                                            .build();
        Set<InputsMetadataResponseProto> inputsMetadataResponses =
            inputsMetadataGenerator.fetchInputsMetadata(inputsMetadataRequests, requestMetadata);
        for (InputsMetadataResponseProto inputsMetadataResponse : inputsMetadataResponses) {
          String baseFqn = inputsMetadataResponse.getFqn();
          for (String key : inputsMetadataResponse.getResultMap().keySet()) {
            InputsMetadataProto inputsMetadataProto = inputsMetadataResponse.getResultMap().get(key);
            inputsMetadata.put(baseFqn + "." + key,
                InputsMetadata.builder()
                    .description(inputsMetadataProto.getDescription())
                    .required(Boolean.valueOf(inputsMetadataProto.getRequired()))
                    .build());
          }
        }
      }
      return inputsMetadata;
    } catch (Exception ex) {
      log.error(String.format(
                    "Failed to generate runtime inputs metadata for pipeline with identifier %s", pipelineIdentifier),
          ex);
      return inputsMetadata;
    }
  }

  private String getEntityRef(YamlConfig pipelineYamlConfig, FQN entityFQN, String entityRefKey) {
    String entityRef = YamlSubMapExtractor.getNodeForFQN(pipelineYamlConfig, entityFQN).get(entityRefKey).toString();
    return HarnessStringUtils.removeLeadingAndTrailingQuotesBothOrNone(entityRef);
  }

  private String getInnerYamlFromFQN(YamlConfig yaml, FQN fqn) {
    return YamlUtils.writeYamlString(YamlSubMapExtractor.getNodeForFQN(yaml, fqn));
  }
}
