/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.repositories.EnforcementResultRepo;
import io.harness.repositories.SBOMComponentRepo;
import io.harness.spec.server.ssca.v1.model.Artifact;
import io.harness.spec.server.ssca.v1.model.EnforceSbomRequestBody;
import io.harness.spec.server.ssca.v1.model.EnforceSbomResponseBody;
import io.harness.spec.server.ssca.v1.model.EnforcementSummaryResponse;
import io.harness.spec.server.ssca.v1.model.PolicyViolation;
import io.harness.ssca.beans.OpaPolicyEvaluationResult;
import io.harness.ssca.beans.RuleDTO;
import io.harness.ssca.beans.Violation;
import io.harness.ssca.enforcement.ExecutorRegistry;
import io.harness.ssca.enforcement.constants.RuleExecutorType;
import io.harness.ssca.enforcement.rule.Engine;
import io.harness.ssca.entities.ArtifactEntity;
import io.harness.ssca.entities.ArtifactEntity.ArtifactEntityKeys;
import io.harness.ssca.entities.EnforcementResultEntity;
import io.harness.ssca.entities.EnforcementSummaryEntity;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class EnforcementStepServiceImpl implements EnforcementStepService {
  @Inject ArtifactService artifactService;
  @Inject ExecutorRegistry executorRegistry;
  @Inject RuleEngineService ruleEngineService;
  @Inject EnforcementSummaryService enforcementSummaryService;
  @Inject EnforcementResultService enforcementResultService;
  @Inject PolicyMgmtService policyMgmtService;
  @Inject SBOMComponentRepo sbomComponentRepo;
  @Inject FeatureFlagService featureFlagService;
  @Inject EnforcementResultRepo enforcementResultRepo;

  @Override
  public EnforceSbomResponseBody enforceSbom(
      String accountId, String orgIdentifier, String projectIdentifier, EnforceSbomRequestBody body) {
    String artifactId =
        artifactService.generateArtifactId(body.getArtifact().getRegistryUrl(), body.getArtifact().getName());
    ArtifactEntity artifactEntity =
        artifactService
            .getArtifact(accountId, orgIdentifier, projectIdentifier, artifactId,
                Sort.by(ArtifactEntityKeys.createdOn).descending())
            .orElseThrow(()
                             -> new NotFoundException(
                                 String.format("Artifact with image name [%s] and registry Url [%s] is not found",
                                     body.getArtifact().getName(), body.getArtifact().getRegistryUrl())));
    Pair<List<EnforcementResultEntity>, List<EnforcementResultEntity>> policyEvaluationResult;
    if (featureFlagService.isEnabled(
            FeatureName.SSCA_ENFORCEMENT_WITH_BOTH_NATIVE_AND_OPA_POLICIES_ENABLED, accountId)) {
      if (StringUtils.isNotBlank(body.getPolicySetRef())) {
        policyEvaluationResult =
            evaluatePolicyViolationsUsingOpaPolicies(accountId, orgIdentifier, projectIdentifier, body, artifactEntity);
      } else {
        policyEvaluationResult = evaluatePolicyViolationsUsingSscaPolicies(
            accountId, orgIdentifier, projectIdentifier, body, artifactEntity);
      }
    } else {
      policyEvaluationResult =
          evaluatePolicyViolationsUsingOpaPolicies(accountId, orgIdentifier, projectIdentifier, body, artifactEntity);
    }

    String status =
        enforcementSummaryService.persistEnforcementSummary(body.getEnforcementId(), policyEvaluationResult.getLeft(),
            policyEvaluationResult.getRight(), artifactEntity, body.getPipelineExecutionId());

    EnforceSbomResponseBody responseBody = new EnforceSbomResponseBody();
    responseBody.setEnforcementId(body.getEnforcementId());
    responseBody.setStatus(status);

    return responseBody;
  }

  private Pair<List<EnforcementResultEntity>, List<EnforcementResultEntity>> evaluatePolicyViolationsUsingOpaPolicies(
      String accountId, String orgIdentifier, String projectIdentifier, EnforceSbomRequestBody body,
      ArtifactEntity artifactEntity) {
    // TODO: Fetch records from the db in batches and use projection.
    Page<NormalizedSBOMComponentEntity> entities =
        sbomComponentRepo.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndOrchestrationId(accountId,
            orgIdentifier, projectIdentifier, artifactEntity.getOrchestrationId(),
            PageRequest.of(0, Integer.MAX_VALUE));
    List<NormalizedSBOMComponentEntity> normalizedSBOMComponentEntities = entities.toList();
    OpaPolicyEvaluationResult opaPolicyEvaluationResult = policyMgmtService.evaluate(
        accountId, orgIdentifier, projectIdentifier, body.getPolicySetRef(), normalizedSBOMComponentEntities);
    Map<String, NormalizedSBOMComponentEntity> components = normalizedSBOMComponentEntities.stream().collect(
        Collectors.toMap(NormalizedSBOMComponentEntity::getUuid, entity -> entity, (u, v) -> v));
    List<EnforcementResultEntity> allowListViolations =
        getEnforcementResultEntitiesFromOpaViolations(accountId, orgIdentifier, projectIdentifier,
            body.getEnforcementId(), components, artifactEntity, opaPolicyEvaluationResult.getAllowListViolations());
    List<EnforcementResultEntity> denyListViolations =
        getEnforcementResultEntitiesFromOpaViolations(accountId, orgIdentifier, projectIdentifier,
            body.getEnforcementId(), components, artifactEntity, opaPolicyEvaluationResult.getDenyListViolations());
    enforcementResultRepo.saveAll(denyListViolations);
    enforcementResultRepo.saveAll(allowListViolations);
    return Pair.of(denyListViolations, allowListViolations);
  }

  private static List<EnforcementResultEntity> getEnforcementResultEntitiesFromOpaViolations(String accountId,
      String orgIdentifier, String projectIdentifier, String enforcementId,
      Map<String, NormalizedSBOMComponentEntity> components, ArtifactEntity artifactEntity,
      List<Violation> violations) {
    List<EnforcementResultEntity> enforcementResultEntities = new ArrayList<>();
    violations.forEach(opaViolation
        -> opaViolation.getArtifactUuids().forEach(uuid
            -> enforcementResultEntities.add(getEnforcementResultEntity(accountId, orgIdentifier, projectIdentifier,
                enforcementId, components.get(uuid), artifactEntity, opaViolation))));
    return enforcementResultEntities;
  }

  private static EnforcementResultEntity getEnforcementResultEntity(String accountId, String orgIdentifier,
      String projectIdentifier, String enforcementId, NormalizedSBOMComponentEntity component,
      ArtifactEntity artifactEntity, Violation opaViolation) {
    return EnforcementResultEntity.builder()
        .artifactId(component.getArtifactId())
        .enforcementID(enforcementId)
        .accountId(accountId)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .supplierType(component.getOriginatorType())
        .supplier(component.getPackageOriginatorName())
        .name(component.getPackageName())
        .packageManager(component.getPackageManager())
        .purl(component.getPurl())
        .license(component.getPackageLicense())
        .violationType(opaViolation.getType())
        .violationDetails(opaViolation.getViolationDetail())
        .tag(artifactEntity.getTag())
        .imageName(artifactEntity.getName())
        .orchestrationID(artifactEntity.getOrchestrationId())
        .version(component.getPackageVersion())
        .build();
  }

  private Pair<List<EnforcementResultEntity>, List<EnforcementResultEntity>> evaluatePolicyViolationsUsingSscaPolicies(
      String accountId, String orgIdentifier, String projectIdentifier, EnforceSbomRequestBody body,
      ArtifactEntity artifactEntity) {
    RuleDTO ruleDTO = ruleEngineService.getRules(accountId, orgIdentifier, projectIdentifier, body.getPolicyFileId());

    Engine engine = Engine.builder()
                        .artifact(artifactEntity)
                        .enforcementId(body.getEnforcementId())
                        .executorRegistry(executorRegistry)
                        .executorType(RuleExecutorType.MONGO_EXECUTOR)
                        .rules(ruleDTO.getDenyList())
                        .build();

    List<EnforcementResultEntity> denyListResult = engine.executeRules();

    engine.setRules(ruleDTO.getAllowList());
    List<EnforcementResultEntity> allowListResult = engine.executeRules();
    return Pair.of(denyListResult, allowListResult);
  }

  @Override
  public EnforcementSummaryResponse getEnforcementSummary(
      String accountId, String orgIdentifier, String projectIdentifier, String enforcementId) {
    EnforcementSummaryEntity enforcementSummary =
        enforcementSummaryService.getEnforcementSummary(accountId, orgIdentifier, projectIdentifier, enforcementId)
            .orElseThrow(()
                             -> new NotFoundException(String.format(
                                 "Enforcement with enforcementIdentifier [%s] is not found", enforcementId)));

    return new EnforcementSummaryResponse()
        .enforcementId(enforcementSummary.getEnforcementId())
        .artifact(new Artifact()
                      .id(enforcementSummary.getArtifact().getArtifactId())
                      .name(enforcementSummary.getArtifact().getName())
                      .type(enforcementSummary.getArtifact().getType())
                      .registryUrl(enforcementSummary.getArtifact().getUrl())
                      .tag(enforcementSummary.getArtifact().getTag())

                )
        .allowListViolationCount(enforcementSummary.getAllowListViolationCount())
        .denyListViolationCount(enforcementSummary.getDenyListViolationCount())
        .status(enforcementSummary.getStatus());
  }

  @Override
  public Page<PolicyViolation> getPolicyViolations(String accountId, String orgIdentifier, String projectIdentifier,
      String enforcementId, String searchText, Pageable pageable) {
    return enforcementResultService
        .getPolicyViolations(accountId, orgIdentifier, projectIdentifier, enforcementId, searchText, pageable)
        .map(enforcementResultEntity
            -> new PolicyViolation()
                   .enforcementId(enforcementResultEntity.getEnforcementID())
                   .account(enforcementResultEntity.getAccountId())
                   .org(enforcementResultEntity.getOrgIdentifier())
                   .project(enforcementResultEntity.getProjectIdentifier())
                   .artifactId(enforcementResultEntity.getArtifactId())
                   .imageName(enforcementResultEntity.getImageName())
                   .purl(enforcementResultEntity.getPurl())
                   .orchestrationId(enforcementResultEntity.getOrchestrationID())
                   .license(enforcementResultEntity.getLicense())
                   .tag(enforcementResultEntity.getTag())
                   .supplier(enforcementResultEntity.getSupplier())
                   .supplierType(enforcementResultEntity.getSupplierType())
                   .name(enforcementResultEntity.getName())
                   .version(enforcementResultEntity.getVersion())
                   .packageManager(enforcementResultEntity.getPackageManager())
                   .violationType(enforcementResultEntity.getViolationType())
                   .violationDetails(enforcementResultEntity.getViolationDetails()));
  }
}
