/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.gitcaching.GitCachingConstants.BOOLEAN_FALSE_VALUE;
import static io.harness.pms.merger.helpers.InputSetMergeHelper.mergeInputSetIntoPipelineForGivenStages;
import static io.harness.pms.merger.helpers.InputSetMergeHelper.mergeInputSets;
import static io.harness.pms.merger.helpers.InputSetMergeHelper.mergeInputSetsForGivenStages;
import static io.harness.pms.merger.helpers.InputSetTemplateHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.InputSetTemplateHelper.createTemplateFromPipelineForGivenStages;

import io.harness.annotations.dev.OwnedBy;
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
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipeline.service.PipelineCRUDErrorResponse;
import io.harness.pms.plan.execution.StagesExecutionHelper;
import io.harness.pms.stages.StagesExpressionExtractor;
import io.harness.pms.yaml.PipelineVersion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ValidateAndMergeHelper {
  private final PMSPipelineService pmsPipelineService;
  private final PMSInputSetService pmsInputSetService;
  private final PMSPipelineTemplateHelper pipelineTemplateHelper;
  private final GitSyncSdkService gitSyncSdkService;

  public PipelineEntity getPipelineEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineBranch, String pipelineRepoID, boolean checkForStoreType) {
    if (gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return getPipelineEntityForOldGitSyncFlow(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);
    } else {
      return getPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, checkForStoreType);
    }
  }

  private PipelineEntity getPipelineEntityForOldGitSyncFlow(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String pipelineBranch, String pipelineRepoID) {
    GitSyncBranchContext gitSyncBranchContext =
        GitSyncBranchContext.builder()
            .gitBranchInfo(GitEntityInfo.builder().branch(pipelineBranch).yamlGitConfigId(pipelineRepoID).build())
            .build();

    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(gitSyncBranchContext, true)) {
      Optional<PipelineEntity> pipelineEntity = pmsPipelineService.getAndValidatePipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      if (pipelineEntity.isPresent()) {
        return pipelineEntity.get();
      } else {
        throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
            orgIdentifier, projectIdentifier, pipelineIdentifier));
      }
    }
  }

  private PipelineEntity getPipelineEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean checkForStoreType) {
    Optional<PipelineEntity> optionalPipelineEntity;
    if (GitContextHelper.isUpdateToNewBranch()) {
      String baseBranch = Objects.requireNonNull(GitContextHelper.getGitEntityInfo()).getBaseBranch();
      GitSyncBranchContext branchContext =
          GitSyncBranchContext.builder().gitBranchInfo(GitEntityInfo.builder().branch(baseBranch).build()).build();
      try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(branchContext, true)) {
        optionalPipelineEntity = pmsPipelineService.getAndValidatePipeline(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      }
    } else {
      long start = System.currentTimeMillis();
      optionalPipelineEntity = pmsPipelineService.getAndValidatePipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      log.info(
          "[PMS_ValidateMerger] fetching and validating pipeline when update to new branch is false, took {}ms for projectId {}, orgId {}, accountId {}",
          System.currentTimeMillis() - start, projectIdentifier, orgIdentifier, accountId);
    }
    if (optionalPipelineEntity.isPresent()) {
      StoreType storeTypeInContext = GitAwareContextHelper.getGitRequestParamsInfo().getStoreType();
      PipelineEntity pipelineEntity = optionalPipelineEntity.get();
      if (checkForStoreType && storeTypeInContext != null && pipelineEntity.getStoreType() != storeTypeInContext) {
        throw new InvalidRequestException("Input Set should have the same Store Type as the Pipeline it is for");
      }
      return pipelineEntity;
    } else {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }
  }

  public InputSetTemplateResponseDTOPMS getInputSetTemplateResponseDTO(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> stageIdentifiers) {
    Optional<PipelineEntity> optionalPipelineEntity = pmsPipelineService.getAndValidatePipeline(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (optionalPipelineEntity.isPresent()) {
      String template;
      List<String> replacedExpressions = null;

      String pipelineYaml = optionalPipelineEntity.get().getYaml();
      if (EmptyPredicate.isEmpty(stageIdentifiers)) {
        template = createTemplateFromPipeline(pipelineYaml);
      } else {
        String yaml = getYaml(accountId, orgIdentifier, projectIdentifier, pipelineYaml, optionalPipelineEntity);
        StagesExecutionHelper.throwErrorIfAllStagesAreDeleted(yaml, stageIdentifiers);
        replacedExpressions = new ArrayList<>(StagesExpressionExtractor.getNonLocalExpressions(yaml, stageIdentifiers));
        template = createTemplateFromPipelineForGivenStages(pipelineYaml, stageIdentifiers);
      }

      boolean hasInputSets = pmsInputSetService.checkForInputSetsForPipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

      return InputSetTemplateResponseDTOPMS.builder()
          .inputSetTemplateYaml(template)
          .replacedExpressions(replacedExpressions)
          .modules(optionalPipelineEntity.get().getFilters().keySet())
          .hasInputSets(hasInputSets)
          .build();
    } else {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }
  }

  private String getYaml(String accountId, String orgIdentifier, String projectIdentifier, String pipelineYaml,
      Optional<PipelineEntity> optionalPipelineEntity) {
    if (optionalPipelineEntity.isPresent()
        && Boolean.TRUE.equals(optionalPipelineEntity.get().getTemplateReference())) {
      // returning resolved yaml
      return pipelineTemplateHelper
          .resolveTemplateRefsInPipeline(accountId, orgIdentifier, projectIdentifier, pipelineYaml, BOOLEAN_FALSE_VALUE)
          .getMergedPipelineYaml();
    }
    return pipelineYaml;
  }

  public String getPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, List<String> stageIdentifiers) {
    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineService.getPipeline(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false, false);
    if (optionalPipelineEntity.isPresent()) {
      String pipelineYaml = optionalPipelineEntity.get().getYaml();
      if (EmptyPredicate.isEmpty(stageIdentifiers)) {
        return createTemplateFromPipeline(pipelineYaml);
      } else {
        return createTemplateFromPipelineForGivenStages(pipelineYaml, stageIdentifiers);
      }

    } else {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }
  }

  public String getPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineBranch, String pipelineRepoID, List<String> stageIdentifiers) {
    String pipelineYaml = getPipelineEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID, false)
                              .getYaml();
    if (EmptyPredicate.isEmpty(stageIdentifiers)) {
      return createTemplateFromPipeline(pipelineYaml);
    }
    return createTemplateFromPipelineForGivenStages(pipelineYaml, stageIdentifiers);
  }

  public String getMergeInputSetFromPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, List<String> inputSetReferences, String pipelineBranch, String pipelineRepoID,
      List<String> stageIdentifiers) {
    Set<String> inputSetVersions = new HashSet<>();
    PipelineEntity pipelineEntity = getPipelineEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID, false);
    String pipelineYaml = pipelineEntity.getYaml();
    String pipelineTemplate = EmptyPredicate.isEmpty(stageIdentifiers)
        ? createTemplateFromPipeline(pipelineYaml)
        : createTemplateFromPipelineForGivenStages(pipelineYaml, stageIdentifiers);

    if (EmptyPredicate.isEmpty(pipelineTemplate)) {
      throw new InvalidRequestException(
          "Pipeline " + pipelineIdentifier + " does not have any runtime input. All existing input sets are invalid");
    }

    List<String> inputSetYamlList = new ArrayList<>();
    inputSetReferences.forEach(identifier -> {
      Optional<InputSetEntity> entity = pmsInputSetService.getWithoutValidations(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false);
      if (entity.isEmpty()) {
        return;
      }
      InputSetEntity inputSet = entity.get();
      inputSetVersions.add(inputSet.getHarnessVersion());
      checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(pipelineEntity, inputSet);
      if (inputSet.getInputSetEntityType() == InputSetEntityType.INPUT_SET) {
        inputSetYamlList.add(inputSet.getYaml());
      } else {
        List<String> overlayReferences = inputSet.getInputSetReferences();
        overlayReferences.forEach(id -> {
          Optional<InputSetEntity> entity2 = pmsInputSetService.getWithoutValidations(
              accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, id, false);
          entity2.ifPresent(inputSetEntity -> {
            checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(pipelineEntity, entity2.get());
            inputSetYamlList.add(inputSetEntity.getYaml());
          });
        });
      }
    });

    if (inputSetVersions.contains(PipelineVersion.V0) && inputSetVersions.contains(PipelineVersion.V1)) {
      throw new InvalidRequestException("Input set versions 0 and 1 are not compatible");
    }
    if (inputSetVersions.contains(PipelineVersion.V1)) {
      return InputSetMergeHelper.mergeInputSetsV1(inputSetYamlList);
    }

    if (EmptyPredicate.isEmpty(stageIdentifiers)) {
      return mergeInputSets(pipelineTemplate, inputSetYamlList, false);
    }
    return mergeInputSetsForGivenStages(pipelineTemplate, inputSetYamlList, false, stageIdentifiers);
  }

  public String mergeInputSetIntoPipeline(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String mergedRuntimeInputYaml, String pipelineBranch, String pipelineRepoID,
      List<String> stageIdentifiers) {
    String pipelineYaml = getPipelineEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID, false)
                              .getYaml();
    if (EmptyPredicate.isEmpty(stageIdentifiers)) {
      return InputSetMergeHelper.mergeInputSetIntoPipeline(pipelineYaml, mergedRuntimeInputYaml, false);
    }
    return mergeInputSetIntoPipelineForGivenStages(pipelineYaml, mergedRuntimeInputYaml, false, stageIdentifiers);
  }

  private void checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(
      PipelineEntity pipelineEntity, InputSetEntity inputSetEntity) {
    if (pipelineEntity.getStoreType() == null || inputSetEntity.getStoreType() == null) {
      return;
    }
    if (!pipelineEntity.getStoreType().equals(inputSetEntity.getStoreType())) {
      throw NestedExceptionUtils.hintWithExplanationException("Please move the input-set from inline to remote.",
          "The pipeline is remote and input-set is inline",
          new InvalidRequestException(String.format(
              "Remote Pipeline %s cannot be used with inline input-set %s, please move input-set to from inline to remote to use them",
              pipelineEntity.getIdentifier(), inputSetEntity.getIdentifier())));
    }
  }
}
