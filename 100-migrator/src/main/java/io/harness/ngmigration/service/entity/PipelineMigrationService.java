/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.entity;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngmigration.utils.NGMigrationConstants.RUNTIME_FIELD;
import static io.harness.ngmigration.utils.NGMigrationConstants.RUNTIME_INPUT;

import static software.wings.ngmigration.NGMigrationEntityType.ENVIRONMENT;
import static software.wings.ngmigration.NGMigrationEntityType.PIPELINE;
import static software.wings.ngmigration.NGMigrationEntityType.SERVICE;
import static software.wings.ngmigration.NGMigrationEntityType.WORKFLOW;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.MigratedEntityMapping;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGSkipDetail;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.beans.YamlGenerationDetails;
import io.harness.ngmigration.client.NGClient;
import io.harness.ngmigration.client.PmsClient;
import io.harness.ngmigration.client.TemplateClient;
import io.harness.ngmigration.dto.ImportError;
import io.harness.ngmigration.dto.MigrationImportSummaryDTO;
import io.harness.ngmigration.expressions.MigratorExpressionUtils;
import io.harness.ngmigration.service.MigrationTemplateUtils;
import io.harness.ngmigration.service.MigratorMappingService;
import io.harness.ngmigration.service.NgMigrationService;
import io.harness.ngmigration.service.step.ApprovalStepMapperImpl;
import io.harness.ngmigration.service.workflow.WorkflowHandler;
import io.harness.ngmigration.utils.CaseFormat;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.pms.governance.PipelineSaveResponse;
import io.harness.pms.pipeline.PMSPipelineResponseDTO;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.remote.client.NGRestUtils;
import io.harness.steps.approval.stage.ApprovalStageConfig;
import io.harness.steps.approval.stage.ApprovalStageNode;
import io.harness.steps.pipelinestage.PipelineStageConfig;
import io.harness.steps.pipelinestage.PipelineStageNode;
import io.harness.steps.template.stage.TemplateStageNode;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.template.yaml.TemplateLinkConfig;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.utils.JsonPipelineUtils;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.ngmigration.CgBasicInfo;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class PipelineMigrationService extends NgMigrationService {
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowMigrationService workflowMigrationService;
  @Inject private TemplateResourceClient templateResourceClient;
  @Inject private MigrationTemplateUtils migrationTemplateUtils;
  @Inject PipelineServiceClient pipelineServiceClient;
  @Inject ApprovalStepMapperImpl approvalStepMapper;

  @Override
  public MigratedEntityMapping generateMappingEntity(NGYamlFile yamlFile) {
    CgBasicInfo basicInfo = yamlFile.getCgBasicInfo();
    PipelineInfoConfig pipelineInfoConfig = ((PipelineConfig) yamlFile.getYaml()).getPipelineInfoConfig();
    return MigratedEntityMapping.builder()
        .appId(basicInfo.getAppId())
        .accountId(basicInfo.getAccountId())
        .cgEntityId(basicInfo.getId())
        .entityType(NGMigrationEntityType.PIPELINE.name())
        .accountIdentifier(basicInfo.getAccountId())
        .orgIdentifier(pipelineInfoConfig.getOrgIdentifier())
        .projectIdentifier(pipelineInfoConfig.getProjectIdentifier())
        .identifier(pipelineInfoConfig.getIdentifier())
        .scope(MigratorMappingService.getScope(
            pipelineInfoConfig.getOrgIdentifier(), pipelineInfoConfig.getProjectIdentifier()))
        .fullyQualifiedIdentifier(MigratorMappingService.getFullyQualifiedIdentifier(basicInfo.getAccountId(),
            pipelineInfoConfig.getOrgIdentifier(), pipelineInfoConfig.getProjectIdentifier(),
            pipelineInfoConfig.getIdentifier()))
        .build();
  }

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    if (entity == null) {
      return null;
    }
    Pipeline pipeline = (Pipeline) entity;
    String entityId = pipeline.getUuid();
    CgEntityId pipelineEntityId = CgEntityId.builder().type(NGMigrationEntityType.PIPELINE).id(entityId).build();
    CgEntityNode pipelineNode = CgEntityNode.builder()
                                    .id(entityId)
                                    .appId(pipeline.getAppId())
                                    .type(NGMigrationEntityType.PIPELINE)
                                    .entityId(pipelineEntityId)
                                    .entity(pipeline)
                                    .build();

    Set<CgEntityId> children = new HashSet<>();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      List<PipelineStage> stages = pipeline.getPipelineStages();
      stages.stream().flatMap(stage -> stage.getPipelineStageElements().stream()).forEach(stageElement -> {
        // Handle Approval State
        if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
          String workflowId = (String) stageElement.getProperties().get("workflowId");
          if (isNotEmpty(workflowId)) {
            children.add(CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(workflowId).build());
          }
        }
      });
    }

    return DiscoveryNode.builder().children(children).entityNode(pipelineNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    Pipeline pipeline = pipelineService.getPipeline(appId, entityId);
    if (pipeline == null) {
      throw new InvalidRequestException(
          format("Pipeline with id:[%s] in application with id:[%s] doesn't exist", entityId, appId));
    }
    return discover(pipeline);
  }

  @Override
  public MigrationImportSummaryDTO migrate(String auth, NGClient ngClient, PmsClient pmsClient,
      TemplateClient templateClient, MigrationInputDTO inputDTO, NGYamlFile yamlFile) throws IOException {
    try {
      String yaml = getYamlString(yamlFile);
      Response<ResponseDTO<PipelineSaveResponse>> resp =
          pmsClient
              .createPipeline(auth, inputDTO.getAccountIdentifier(), inputDTO.getOrgIdentifier(),
                  inputDTO.getProjectIdentifier(), RequestBody.create(MediaType.parse("application/yaml"), yaml))
              .execute();
      log.info("Pipeline creation Response details {} {}", resp.code(), resp.message());
      if (resp.code() >= 400) {
        log.info("Pipeline generated is \n - {}", yaml);
      }
      return handleResp(yamlFile, resp);
    } catch (Exception ex) {
      log.error("Pipeline creation failed - ", ex);
      return MigrationImportSummaryDTO.builder()
          .errors(Collections.singletonList(ImportError.builder()
                                                .message("There was an error creating the pipeline")
                                                .entity(yamlFile.getCgBasicInfo())
                                                .build()))
          .build();
    }
  }

  @Override
  public YamlGenerationDetails generateYaml(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NGYamlFile> migratedEntities) {
    if (EmptyPredicate.isNotEmpty(inputDTO.getDefaults()) && inputDTO.getDefaults().containsKey(PIPELINE)
        && inputDTO.getDefaults().get(PIPELINE).isSkipMigration()) {
      return null;
    }
    Pipeline pipeline = (Pipeline) entities.get(entityId).getEntity();
    if (EmptyPredicate.isEmpty(pipeline.getPipelineStages())) {
      // TODO: @deepakputhraya
      return null;
    }

    String name = MigratorUtility.generateName(inputDTO.getOverrides(), entityId, pipeline.getName());
    String identifier = MigratorUtility.generateIdentifierDefaultName(
        inputDTO.getOverrides(), entityId, name, inputDTO.getIdentifierCaseFormat());
    Scope scope = MigratorUtility.getDefaultScope(inputDTO, entityId, Scope.PROJECT);
    String projectIdentifier = MigratorUtility.getProjectIdentifier(scope, inputDTO);
    String orgIdentifier = MigratorUtility.getOrgIdentifier(scope, inputDTO);
    String description = StringUtils.isBlank(pipeline.getDescription()) ? "" : pipeline.getDescription();

    List<StageElementWrapperConfig> ngStages = new ArrayList<>();
    List<StageElementWrapperConfig> parallelStages = null;
    List<NGVariable> pipelineVariables = getPipelineVariables(pipeline, entities);
    // <+pipeline.variables.hello2>
    for (int i = 0; i < pipeline.getPipelineStages().size(); ++i) {
      PipelineStage pipelineStage = pipeline.getPipelineStages().get(i);
      if (!isPartOfParallelStage(pipeline.getPipelineStages(), i)) {
        if (EmptyPredicate.isNotEmpty(parallelStages)) {
          ngStages.add(StageElementWrapperConfig.builder().parallel(JsonPipelineUtils.asTree(parallelStages)).build());
        }
        parallelStages = null;
      } else if (parallelStages == null) {
        parallelStages = new ArrayList<>();
      }
      for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
        StageElementWrapperConfig stage = null;
        if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
          String workflowId = (String) stageElement.getProperties().get("workflowId");
          if (isNotEmpty(workflowId)) {
            NGSkipDetail skipDetail = getSkipDetailForWorkflowStage(pipeline, stageElement, migratedEntities);
            if (skipDetail != null) {
              return YamlGenerationDetails.builder().skipDetails(Collections.singletonList(skipDetail)).build();
            }
            stage = buildWorkflowStage(
                pipeline.getAccountId(), stageElement, entities, migratedEntities, inputDTO.getIdentifierCaseFormat());
          }
        } else {
          stage = buildApprovalStage(stageElement, inputDTO.getIdentifierCaseFormat());
        }
        // If the stage cannot be migrated then we skip building the pipeline.
        if (stage == null) {
          // TODO @Deepakputhraya
          return null;
        }
        Objects.requireNonNullElse(parallelStages, ngStages).add(stage);
      }
    }
    if (EmptyPredicate.isNotEmpty(parallelStages)) {
      ngStages.add(StageElementWrapperConfig.builder().parallel(JsonPipelineUtils.asTree(parallelStages)).build());
    }

    if (EmptyPredicate.isEmpty(ngStages)) {
      return YamlGenerationDetails.builder()
          .skipDetails(Collections.singletonList(NGSkipDetail.builder()
                                                     .reason("The constructed pipeline had no stages")
                                                     .cgBasicInfo(pipeline.getCgBasicInfo())
                                                     .type(PIPELINE)
                                                     .build()))
          .build();
    }

    List<NGYamlFile> files = new ArrayList<>();
    NGYamlFile ngYamlFile =
        NGYamlFile.builder()
            .type(PIPELINE)
            .filename("pipelines/" + name + ".yaml")
            .yaml(PipelineConfig.builder()
                      .pipelineInfoConfig(PipelineInfoConfig.builder()
                                              .identifier(identifier)
                                              .name(name)
                                              .description(ParameterField.createValueField(description))
                                              .projectIdentifier(projectIdentifier)
                                              .orgIdentifier(orgIdentifier)
                                              .stages(ngStages)
                                              .allowStageExecutions(true)
                                              .variables(pipelineVariables)
                                              .build())
                      .build())
            .ngEntityDetail(NgEntityDetail.builder()
                                .identifier(identifier)
                                .orgIdentifier(orgIdentifier)
                                .projectIdentifier(projectIdentifier)
                                .build())
            .cgBasicInfo(pipeline.getCgBasicInfo())
            .build();
    files.add(ngYamlFile);
    migratedEntities.putIfAbsent(entityId, ngYamlFile);
    return YamlGenerationDetails.builder().yamlFileList(files).build();
  }

  private boolean isPartOfParallelStage(List<PipelineStage> stages, int index) {
    PipelineStage currentStage = stages.get(index);
    if (currentStage.isParallel()) {
      return true;
    }
    if (index + 1 < stages.size()) {
      PipelineStage nextStage = stages.get(index + 1);
      return nextStage.isParallel();
    }
    return false;
  }

  private StageElementWrapperConfig buildApprovalStage(PipelineStageElement stageElement, CaseFormat caseFormat) {
    AbstractStepNode stepNode = approvalStepMapper.getSpec(stageElement, caseFormat);
    ExecutionWrapperConfig stepWrapper =
        ExecutionWrapperConfig.builder().step(JsonPipelineUtils.asTree(stepNode)).build();

    ApprovalStageNode approvalStageNode = new ApprovalStageNode();
    approvalStageNode.setName(stageElement.getName());
    approvalStageNode.setIdentifier(MigratorUtility.generateIdentifier(stageElement.getName(), caseFormat));
    approvalStageNode.setApprovalStageConfig(
        ApprovalStageConfig.builder()
            .execution(ExecutionElementConfig.builder().steps(Collections.singletonList(stepWrapper)).build())
            .build());
    approvalStageNode.setFailureStrategies(WorkflowHandler.getDefaultFailureStrategy());

    return StageElementWrapperConfig.builder().stage(JsonPipelineUtils.asTree(approvalStageNode)).build();
  }

  private NGSkipDetail getSkipDetailForWorkflowStage(
      Pipeline pipeline, PipelineStageElement stageElement, Map<CgEntityId, NGYamlFile> migratedEntities) {
    // TODO: Handle Skip condition
    String workflowId = stageElement.getProperties().get("workflowId").toString();
    // Throw error if the stage is using canary or multi WFs.
    NGYamlFile wfTemplate = migratedEntities.get(CgEntityId.builder().id(workflowId).type(WORKFLOW).build());
    if (wfTemplate == null) {
      log.warn("The workflow was not migrated, aborting pipeline migration {}", workflowId);
      return NGSkipDetail.builder()
          .reason("The workflow used as a stage was not migrated")
          .cgBasicInfo(pipeline.getCgBasicInfo())
          .type(PIPELINE)
          .build();
    }

    if (wfTemplate.getYaml() instanceof NGTemplateConfig) {
      NGTemplateConfig wfTemplateConfig = (NGTemplateConfig) wfTemplate.getYaml();
      if (TemplateEntityType.PIPELINE_TEMPLATE.equals(wfTemplateConfig.getTemplateInfoConfig().getType())) {
        log.warn("Cannot link a multi-service WFs as they are created as pipeline templates");
        return NGSkipDetail.builder()
            .reason("A multi-service workflow is linked to this pipeline.")
            .cgBasicInfo(pipeline.getCgBasicInfo())
            .type(PIPELINE)
            .build();
      }
    }
    return null;
  }

  private List<NGVariable> getPipelineVariables(Pipeline pipeline, Map<CgEntityId, CgEntityNode> entities) {
    if (EmptyPredicate.isEmpty(pipeline.getPipelineStages())) {
      return new ArrayList<>();
    }

    List<PipelineStageElement> stageElements =
        pipeline.getPipelineStages()
            .stream()
            .filter(ps -> ps != null && EmptyPredicate.isNotEmpty(ps.getPipelineStageElements()))
            .flatMap(ps -> ps.getPipelineStageElements().stream())
            .filter(ps -> EmptyPredicate.isNotEmpty(ps.getWorkflowVariables()))
            .filter(ps -> StateType.ENV_STATE.name().equals(ps.getType()))
            .collect(Collectors.toList());

    List<String> toSkip = new ArrayList<>();
    for (PipelineStageElement stageElement : stageElements) {
      String workflowId = stageElement.getProperties().get("workflowId").toString();
      CgEntityId workflowEntityId = CgEntityId.builder().id(workflowId).type(WORKFLOW).build();
      if (entities.containsKey(workflowEntityId)) {
        Workflow workflow = (Workflow) entities.get(workflowEntityId).getEntity();
        CanaryOrchestrationWorkflow orchestrationWorkflow =
            (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
        WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
        String serviceExpression = getExpression(workflowPhase, "serviceId");
        String env = workflow.getEnvId();
        String infra = getExpression(workflowPhase, "infraDefinitionId");
        if (StringUtils.isNotBlank(serviceExpression)) {
          toSkip.add(serviceExpression);
        }
        if (StringUtils.isNotBlank(env)) {
          toSkip.add(env);
        }
        if (StringUtils.isNotBlank(infra)) {
          toSkip.add(infra);
        }
      }
    }

    return stageElements.stream()
        .map(PipelineStageElement::getWorkflowVariables)
        .flatMap(variables -> variables.entrySet().stream())
        .filter(entry -> StringUtils.isNotBlank(entry.getValue()) && isExpression(entry.getValue()))
        .filter(entry -> !toSkip.contains(entry.getKey()))
        .map(entry -> entry.getValue().substring(2, entry.getValue().length() - 1))
        .distinct()
        .map(val -> StringNGVariable.builder().type(NGVariableType.STRING).name(val).value(RUNTIME_FIELD).build())
        .collect(Collectors.toList());
  }

  private StageElementWrapperConfig buildWorkflowStage(String accountId, PipelineStageElement stageElement,
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, NGYamlFile> migratedEntities, CaseFormat caseFormat) {
    // TODO: Handle Skip condition
    String workflowId = stageElement.getProperties().get("workflowId").toString();
    // Throw error if the stage is using canary or multi WFs.
    CgEntityId workflowEntityId = CgEntityId.builder().id(workflowId).type(WORKFLOW).build();
    if (!migratedEntities.containsKey(workflowEntityId) || !entities.containsKey(workflowEntityId)) {
      log.error("The workflow was not migrated, aborting pipeline migration {}", workflowId);
      return null;
    }

    NGYamlFile wfTemplate = migratedEntities.get(workflowEntityId);
    Workflow workflow = (Workflow) entities.get(workflowEntityId).getEntity();

    String stageServiceRef = RUNTIME_INPUT;
    JsonNode serviceInputs = null;
    String serviceId = getServiceId(workflow, stageElement);
    if (StringUtils.isNotBlank(serviceId)) {
      CgEntityId serviceEntityId = CgEntityId.builder().id(serviceId).type(SERVICE).build();
      if (migratedEntities.containsKey(serviceEntityId)) {
        NgEntityDetail serviceDetails = migratedEntities.get(serviceEntityId).getNgEntityDetail();
        stageServiceRef = MigratorUtility.getIdentifierWithScope(serviceDetails);
        serviceInputs = migrationTemplateUtils.getServiceInput(serviceDetails, accountId);
        if (serviceInputs != null) {
          serviceInputs = serviceInputs.get("serviceInputs");
        }
      }
    }

    String stageEnvRef = RUNTIME_INPUT;
    if (StringUtils.isNotBlank(workflow.getEnvId())) {
      CgEntityId envEntityId = CgEntityId.builder().id(workflow.getEnvId()).type(ENVIRONMENT).build();
      if (migratedEntities.containsKey(envEntityId)) {
        stageEnvRef = MigratorUtility.getIdentifierWithScope(migratedEntities.get(envEntityId).getNgEntityDetail());
      }
    }

    if (wfTemplate.getYaml() instanceof PipelineConfig) {
      PipelineInfoConfig pipelineConfig = ((PipelineConfig) wfTemplate.getYaml()).getPipelineInfoConfig();
      PipelineStageConfig pipelineStageConfig = PipelineStageConfig.builder()
                                                    .pipeline(pipelineConfig.getIdentifier())
                                                    .project(pipelineConfig.getProjectIdentifier())
                                                    .org(pipelineConfig.getOrgIdentifier())
                                                    .build();
      PipelineStageNode stageNode = new PipelineStageNode();
      stageNode.setName(stageElement.getName());
      stageNode.setIdentifier(MigratorUtility.generateIdentifier(stageElement.getName(), caseFormat));
      stageNode.setDescription(ParameterField.createValueField(""));
      stageNode.setPipelineStageConfig(pipelineStageConfig);

      return StageElementWrapperConfig.builder().stage(JsonPipelineUtils.asTree(stageNode)).build();
    }

    NGTemplateConfig wfTemplateConfig = (NGTemplateConfig) wfTemplate.getYaml();
    if (TemplateEntityType.PIPELINE_TEMPLATE.equals(wfTemplateConfig.getTemplateInfoConfig().getType())) {
      log.warn("Cannot link a multi-service WFs as they are created as pipeline templates");
      return null;
    }

    JsonNode templateInputs = migrationTemplateUtils.getTemplateInputs(wfTemplate.getNgEntityDetail(), accountId);

    Map<String, String> workflowVariables = stageElement.getWorkflowVariables();
    // Set common runtime inputs
    if (templateInputs != null) {
      String whenInput = templateInputs.at("/when/condition").asText();
      if (RUNTIME_INPUT.equals(whenInput)) {
        String when = "true";
        Map<String, Object> properties = stageElement.getProperties();
        if (EmptyPredicate.isNotEmpty(properties) && properties.containsKey("disabled")) {
          boolean disabled = (Boolean) properties.get("disabled");
          if (Boolean.TRUE.equals(disabled)) {
            when = "false";
          }
        }
        if (EmptyPredicate.isNotEmpty(properties) && properties.containsKey("disableAssertion")) {
          String assertion = (String) properties.get("disableAssertion");
          if (StringUtils.isNotBlank(assertion)) {
            assertion = (String) MigratorExpressionUtils.render(
                entities, migratedEntities, assertion, new HashMap<>(), caseFormat);
            when = WorkflowHandler.wrapNot(assertion).getValue();
          }
          ObjectNode whenNode = (ObjectNode) templateInputs.get("when");
          whenNode.put("condition", when);
        }
      }
      ArrayNode variablesArray = (ArrayNode) templateInputs.get("variables");
      if (EmptyPredicate.isNotEmpty(workflowVariables) && !EmptyPredicate.isEmpty(variablesArray)) {
        for (JsonNode node : variablesArray) {
          String key = node.get("name").asText();
          if (workflowVariables.containsKey(key) && isExpression(workflowVariables.get(key))) {
            String pipelineVar = workflowVariables.get(key).substring(2, workflowVariables.get(key).length() - 1);
            ((ObjectNode) node).put("value", "<+pipeline.variables." + pipelineVar + ">");
          }
        }
      }
    }

    // Set Deployment specific runtime inputs
    if (templateInputs != null && "Deployment".equals(templateInputs.get("type").asText())) {
      String serviceRef = templateInputs.at("/spec/service/serviceRef").asText();
      if (RUNTIME_INPUT.equals(serviceRef) && !RUNTIME_INPUT.equals(stageServiceRef)) {
        ObjectNode service = (ObjectNode) templateInputs.get("spec").get("service");
        service.put("serviceRef", stageServiceRef);
        if (serviceInputs == null) {
          service.remove("serviceInputs");
        }
      }
      String envRef = templateInputs.at("/spec/environment/environmentRef").asText();
      if (RUNTIME_INPUT.equals(envRef)) {
        ObjectNode service = (ObjectNode) templateInputs.get("spec").get("environment");
        service.put("environmentRef", stageEnvRef);
      }
    }
    TemplateLinkConfig templateLinkConfig = new TemplateLinkConfig();
    templateLinkConfig.setTemplateRef(MigratorUtility.getIdentifierWithScope(wfTemplate.getNgEntityDetail()));
    templateLinkConfig.setTemplateInputs(templateInputs);

    TemplateStageNode templateStageNode = new TemplateStageNode();
    templateStageNode.setName(stageElement.getName());
    templateStageNode.setIdentifier(MigratorUtility.generateIdentifier(stageElement.getName(), caseFormat));
    templateStageNode.setDescription("");
    templateStageNode.setTemplate(templateLinkConfig);

    return StageElementWrapperConfig.builder().stage(JsonPipelineUtils.asTree(templateStageNode)).build();
  }

  private String getServiceId(Workflow workflow, PipelineStageElement stageElement) {
    if (workflow == null || EmptyPredicate.isEmpty(workflow.getServices())) {
      return null;
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (EmptyPredicate.isEmpty(orchestrationWorkflow.getWorkflowPhases())
        || orchestrationWorkflow.getWorkflowPhases().size() > 1) {
      return null;
    }
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    String serviceExpression = getExpression(workflowPhase, "serviceId");
    Map<String, String> workflowVariables = MapUtils.emptyIfNull(stageElement.getWorkflowVariables());
    if (StringUtils.isBlank(serviceExpression)) {
      return workflowPhase.getServiceId();
    }
    String serviceId = workflowVariables.get(serviceExpression);
    if (StringUtils.isNotBlank(serviceId) && !isExpression(serviceId)) {
      return serviceId;
    }
    return null;
  }

  private String getInfra(Workflow workflow, PipelineStageElement stageElement) {
    if (workflow == null) {
      return null;
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (EmptyPredicate.isEmpty(orchestrationWorkflow.getWorkflowPhases())
        || orchestrationWorkflow.getWorkflowPhases().size() > 1) {
      return null;
    }
    WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
    String infraExpression = getExpression(workflowPhase, "infraDefinitionId");
    Map<String, String> workflowVariables = MapUtils.emptyIfNull(stageElement.getWorkflowVariables());
    if (StringUtils.isBlank(infraExpression)) {
      return workflowPhase.getInfraDefinitionId();
    }
    String infraId = workflowVariables.get(infraExpression);
    if (StringUtils.isNotBlank(infraId) && !isExpression(infraId)) {
      return infraId;
    }
    return null;
  }

  private static String getExpression(WorkflowPhase workflowPhase, String field) {
    List<TemplateExpression> templateExpressions =
        ListUtils.defaultIfNull(workflowPhase.getTemplateExpressions(), new ArrayList<>());
    return templateExpressions.stream()
        .filter(te -> StringUtils.isNoneBlank(te.getExpression(), te.getFieldName()))
        .filter(te -> field.equals(te.getFieldName()))
        .map(TemplateExpression::getExpression)
        .filter(PipelineMigrationService::isExpression)
        .map(te -> te.substring(2, te.length() - 1))
        .findFirst()
        .orElse(null);
  }

  private static boolean isExpression(String str) {
    if (StringUtils.isBlank(str)) {
      return false;
    }
    return str.startsWith("${") && str.endsWith("}");
  }

  @Override
  protected YamlDTO getNGEntity(Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, NGYamlFile> migratedEntities,
      CgEntityNode cgEntityNode, NgEntityDetail ngEntityDetail, String accountIdentifier) {
    try {
      PMSPipelineResponseDTO response = NGRestUtils.getResponse(
          pipelineServiceClient.getPipelineByIdentifier(ngEntityDetail.getIdentifier(), accountIdentifier,
              ngEntityDetail.getOrgIdentifier(), ngEntityDetail.getProjectIdentifier(), null, null, false));
      if (response == null || StringUtils.isBlank(response.getYamlPipeline())) {
        return null;
      }
      return YamlUtils.read(response.getYamlPipeline(), PipelineConfig.class);
    } catch (Exception ex) {
      log.warn("Error when getting pipeline - ", ex);
      return null;
    }
  }

  @Override
  protected boolean isNGEntityExists() {
    // To avoid migrating Pipelines to NG.
    return true;
  }
}
