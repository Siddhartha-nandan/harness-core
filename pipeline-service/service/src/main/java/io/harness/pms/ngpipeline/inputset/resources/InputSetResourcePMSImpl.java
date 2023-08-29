/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.InputSetTemplateHelper.removeRuntimeInputFromYaml;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitaware.helper.GitImportInfoDTO;
import io.harness.gitsync.GitMetadataUpdateRequestInfoDTO;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitx.USER_FLOW;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.inputset.InputSetMoveConfigOperationDTO;
import io.harness.pms.inputset.MergeInputSetForRerunRequestDTO;
import io.harness.pms.inputset.MergeInputSetRequestDTOPMS;
import io.harness.pms.inputset.MergeInputSetResponseDTOPMS;
import io.harness.pms.inputset.MergeInputSetTemplateRequestDTO;
import io.harness.pms.ngpipeline.inputset.api.InputSetsApiUtils;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetGitUpdateResponseDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetImportRequestDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetImportResponseDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetMoveConfigRequestDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetMoveConfigResponseDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetSanitiseResponseDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateRequestDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetYamlDiffDTO;
import io.harness.pms.ngpipeline.inputset.exceptions.InvalidInputSetException;
import io.harness.pms.ngpipeline.inputset.helpers.InputSetSanitizer;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.pms.ngpipeline.inputset.service.InputSetValidationHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;
import io.harness.pms.pipeline.PMSInputSetListRepoResponse;
import io.harness.pms.pipeline.ResolveInputYamlType;
import io.harness.pms.pipeline.gitsync.PMSUpdateGitDetailsParams;
import io.harness.pms.pipeline.mappers.GitXCacheMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.utils.PageUtils;
import io.harness.utils.PmsFeatureFlagService;
import io.harness.utils.ThreadOperationContextHelper;

import com.google.inject.Inject;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_FIRST_GEN, HarnessModuleComponent.CDS_TEMPLATE_LIBRARY,
        HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(PIPELINE)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@PipelineServiceAuth
@Slf4j
public class InputSetResourcePMSImpl implements InputSetResourcePMS {
  private final PMSInputSetService pmsInputSetService;
  private final PMSPipelineService pipelineService;
  private final GitSyncSdkService gitSyncSdkService;
  private final ValidateAndMergeHelper validateAndMergeHelper;
  private final InputSetsApiUtils inputSetsApiUtils;
  private final PMSExecutionService executionService;
  private final PmsFeatureFlagService pmsFeatureFlagService;
  private ResourceScope resourceScope;
  private Resource resource;
  private AccessControlClient accessControlClient;
  private static final List<String> permissionsList =
      Arrays.asList(PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT, PipelineRbacPermissions.PIPELINE_EXECUTE);
  private static final String PIPELINE_RESOURCE_TYPE = "PIPELINE";
  private static final String ACCESS_DENIED_ERROR_MESSAGE = "Access Denied: You don't have necessary permission(s)";

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetResponseDTOPMS> getInputSet(String inputSetIdentifier,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      String pipelineBranch, String pipelineRepoId, boolean loadFromFallbackBranch,
      GitEntityFindInfoDTO gitEntityBasicInfo, String loadFromCache) {
    log.info(String.format("Retrieving input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Optional<InputSetEntity> optionalInputSetEntity = pmsInputSetService.get(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetIdentifier, false, pipelineBranch, pipelineRepoId, false,
        loadFromFallbackBranch, GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));

    if (optionalInputSetEntity.isEmpty()) {
      throw new InvalidRequestException(
          String.format("InputSet with the given ID: %s does not exist or has been deleted", inputSetIdentifier));
    }
    InputSetEntity inputSetEntity = optionalInputSetEntity.get();
    InputSetResponseDTOPMS inputSet = PMSInputSetElementMapper.toInputSetResponseDTOPMS(inputSetEntity);

    return ResponseDTO.newResponse(inputSetEntity.getVersion().toString(), inputSet);
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<OverlayInputSetResponseDTOPMS> getOverlayInputSet(String inputSetIdentifier,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      String pipelineBranch, String pipelineRepoId, boolean loadFromFallbackBranch,
      GitEntityFindInfoDTO gitEntityBasicInfo, String loadFromCache) {
    log.info(String.format(
        "Retrieving overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Optional<InputSetEntity> optionalInputSetEntity = pmsInputSetService.get(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetIdentifier, false, pipelineBranch, pipelineRepoId, false,
        loadFromFallbackBranch, GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));

    if (optionalInputSetEntity.isEmpty()) {
      throw new InvalidRequestException(
          String.format("InputSet with the given ID: %s does not exist or has been deleted", inputSetIdentifier));
    }
    InputSetEntity inputSetEntity = optionalInputSetEntity.get();
    OverlayInputSetResponseDTOPMS overlayInputSet =
        PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(inputSetEntity);

    return ResponseDTO.newResponse(inputSetEntity.getVersion().toString(), overlayInputSet);
  }

  public ResponseDTO<InputSetResponseDTOPMS> createInputSet(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, String pipelineBranch, String pipelineRepoID,
      GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull String yaml, @NotNull String permission)
      throws AccessDeniedException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    String inputSetVersion = inputSetsApiUtils.inputSetVersion(accountId, yaml);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityFromVersion(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, yaml, inputSetVersion, InputSetEntityType.INPUT_SET);
    log.info(String.format("Create input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    InputSetEntity createdEntity = pmsInputSetService.create(entity, false);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(createdEntity));
  }

  public ResponseDTO<OverlayInputSetResponseDTOPMS> createOverlayInputSet(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, GitEntityCreateInfoDTO gitEntityCreateInfo,
      @NotNull String yaml, @NotNull String permission) throws AccessDeniedException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    log.info(
        String.format("Create overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    // overlay input set validation does not require pipeline branch and repo, hence sending null here
    InputSetEntity createdEntity = pmsInputSetService.create(entity, false);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(createdEntity));
  }

  public ResponseDTO<InputSetResponseDTOPMS> updateInputSet(String ifMatch, String inputSetIdentifier,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      String pipelineBranch, String pipelineRepoID, GitEntityUpdateInfoDTO gitEntityInfo, @NotNull String yaml,
      @NotNull String permission) throws AccessDeniedException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    log.info(String.format("Updating input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    String inputSetVersion = inputSetsApiUtils.inputSetVersion(accountId, yaml);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityFromVersion(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, yaml, inputSetVersion, InputSetEntityType.INPUT_SET);
    InputSetEntity entityWithVersion = entity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    InputSetEntity updatedEntity = pmsInputSetService.update(ChangeType.MODIFY, entityWithVersion, false);
    return ResponseDTO.newResponse(
        updatedEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(updatedEntity));
  }

  public ResponseDTO<OverlayInputSetResponseDTOPMS> updateOverlayInputSet(String ifMatch, String inputSetIdentifier,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      GitEntityUpdateInfoDTO gitEntityInfo, @NotNull String yaml, @NotNull String permission)
      throws AccessDeniedException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    log.info(
        String.format("Updating overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    InputSetEntity entityWithVersion = entity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    // overlay input set validation does not require pipeline branch and repo, hence sending null here
    InputSetEntity updatedEntity = pmsInputSetService.update(ChangeType.MODIFY, entityWithVersion, false);
    return ResponseDTO.newResponse(
        updatedEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(updatedEntity));
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_DELETE)
  public ResponseDTO<Boolean> delete(String ifMatch, String inputSetIdentifier,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      GitEntityDeleteInfoDTO entityDeleteInfo) {
    log.info(String.format("Deleting input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    return ResponseDTO.newResponse(pmsInputSetService.delete(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, inputSetIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<PageResponse<InputSetSummaryResponseDTOPMS>> listInputSetsForPipeline(int page, int size,
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      InputSetListTypePMS inputSetListType, String searchTerm, List<String> sort,
      GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format("Get List of input sets for pipeline %s in project %s, org %s, account %s",
        pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Criteria criteria = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetListType, searchTerm, false);
    Pageable pageRequest =
        PageUtils.getPageRequest(page, size, sort, Sort.by(Sort.Direction.DESC, InputSetEntityKeys.lastUpdatedAt));
    Page<InputSetEntity> inputSetEntities =
        pmsInputSetService.list(criteria, pageRequest, accountId, orgIdentifier, projectIdentifier);

    Page<InputSetSummaryResponseDTOPMS> inputSetList =
        inputSetEntities.map(PMSInputSetElementMapper::toInputSetSummaryResponseDTOPMS);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetTemplateResponseDTOPMS> getTemplateFromPipeline(
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      GitEntityFindInfoDTO gitEntityBasicInfo, InputSetTemplateRequestDTO inputSetTemplateRequestDTO,
      String loadFromCache) {
    log.info(String.format("Get template for pipeline %s in project %s, org %s, account %s", pipelineIdentifier,
        projectIdentifier, orgIdentifier, accountId));
    List<String> stageIdentifiers =
        inputSetTemplateRequestDTO == null ? Collections.emptyList() : inputSetTemplateRequestDTO.getStageIdentifiers();
    InputSetTemplateResponseDTOPMS response =
        validateAndMergeHelper.getInputSetTemplateResponseDTO(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, stageIdentifiers, GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));
    return ResponseDTO.newResponse(response);
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<MergeInputSetResponseDTOPMS> getMergeInputSetFromPipelineTemplate(
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      String pipelineBranch, String pipelineRepoID, GitEntityFindInfoDTO gitEntityBasicInfo,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO, String loadFromCache) {
    if (pipelineBranch == null && gitEntityBasicInfo != null) {
      pipelineBranch = gitEntityBasicInfo.getBranch();
    }
    if (mergeInputSetRequestDTO.isGetOnlyFileContent()
        && pmsFeatureFlagService.isEnabled(accountId, FeatureName.PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    List<String> inputSetReferences = mergeInputSetRequestDTO.getInputSetReferences();
    String mergedYaml;
    try {
      mergedYaml = validateAndMergeHelper.getMergedYamlFromInputSetReferencesAndRuntimeInputYamlWithDefaultValues(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch,
          pipelineRepoID, mergeInputSetRequestDTO.getStageIdentifiers(), mergeInputSetRequestDTO.getLastYamlToMerge(),
          GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));
    } catch (InvalidInputSetException e) {
      InputSetErrorWrapperDTOPMS errorWrapperDTO = (InputSetErrorWrapperDTOPMS) e.getMetadata();
      return ResponseDTO.newResponse(
          MergeInputSetResponseDTOPMS.builder().isErrorResponse(true).inputSetErrorWrapper(errorWrapperDTO).build());
    }
    String fullYaml = "";
    if (mergeInputSetRequestDTO.isWithMergedPipelineYaml()) {
      fullYaml = validateAndMergeHelper.mergeInputSetIntoPipeline(accountId, orgIdentifier, projectIdentifier,
          pipelineIdentifier, mergedYaml, pipelineBranch, pipelineRepoID, mergeInputSetRequestDTO.getStageIdentifiers(),
          GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));
    }
    return ResponseDTO.newResponse(MergeInputSetResponseDTOPMS.builder()
                                       .isErrorResponse(false)
                                       .pipelineYaml(mergedYaml)
                                       .completePipelineYaml(fullYaml)
                                       .build());
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<MergeInputSetResponseDTOPMS> getMergeInputSetForRerun(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, String pipelineBranch, String pipelineRepoID,
      GitEntityFindInfoDTO gitEntityBasicInfo,
      @NotNull @Valid MergeInputSetForRerunRequestDTO mergeInputSetRequestDTO) {
    String planExecutionId = mergeInputSetRequestDTO.getPlanExecutionId();
    String mergedYaml;
    try {
      mergedYaml = executionService.mergeRuntimeInputIntoPipelineForRerun(accountId, orgIdentifier, projectIdentifier,
          pipelineIdentifier, planExecutionId, pipelineBranch, pipelineRepoID,
          mergeInputSetRequestDTO.getStageIdentifiers());
    } catch (InvalidInputSetException e) {
      InputSetErrorWrapperDTOPMS errorWrapperDTO = (InputSetErrorWrapperDTOPMS) e.getMetadata();
      return ResponseDTO.newResponse(
          MergeInputSetResponseDTOPMS.builder().isErrorResponse(true).inputSetErrorWrapper(errorWrapperDTO).build());
    }
    String fullYaml = "";
    if (mergeInputSetRequestDTO.isGetResponseWithMergedPipelineYaml()) {
      fullYaml = validateAndMergeHelper.mergeInputSetIntoPipeline(accountId, orgIdentifier, projectIdentifier,
          pipelineIdentifier, mergedYaml, pipelineBranch, pipelineRepoID, mergeInputSetRequestDTO.getStageIdentifiers(),
          false);
    }
    return ResponseDTO.newResponse(MergeInputSetResponseDTOPMS.builder()
                                       .isErrorResponse(false)
                                       .pipelineYaml(mergedYaml)
                                       .completePipelineYaml(fullYaml)
                                       .build());
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<MergeInputSetResponseDTOPMS> getMergeInputForExecution(
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, boolean resolveExpressions,
      ResolveInputYamlType resolveExpressionsType, @NotNull String planExecutionId) {
    String mergedYaml;
    try {
      mergedYaml = executionService.mergeRuntimeInputIntoPipeline(
          accountId, orgIdentifier, projectIdentifier, planExecutionId, resolveExpressions, resolveExpressionsType);
    } catch (InvalidInputSetException e) {
      InputSetErrorWrapperDTOPMS errorWrapperDTO = (InputSetErrorWrapperDTOPMS) e.getMetadata();
      return ResponseDTO.newResponse(
          MergeInputSetResponseDTOPMS.builder().isErrorResponse(true).inputSetErrorWrapper(errorWrapperDTO).build());
    }
    return ResponseDTO.newResponse(
        MergeInputSetResponseDTOPMS.builder().isErrorResponse(false).pipelineYaml(mergedYaml).build());
  }

  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  // TODO(Naman): Correct PipelineServiceClient when modifying this api
  public ResponseDTO<MergeInputSetResponseDTOPMS> getMergeInputSetFromPipelineTemplate(
      @NotNull @AccountIdentifier String accountId, @NotNull @OrgIdentifier String orgIdentifier,
      @NotNull @ProjectIdentifier String projectIdentifier, @NotNull @ResourceIdentifier String pipelineIdentifier,
      String pipelineBranch, String pipelineRepoID, GitEntityFindInfoDTO gitEntityBasicInfo,
      @NotNull @Valid MergeInputSetTemplateRequestDTO mergeInputSetTemplateRequestDTO) {
    String fullYaml = validateAndMergeHelper.mergeInputSetIntoPipeline(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, mergeInputSetTemplateRequestDTO.getRuntimeInputYaml(), pipelineBranch, pipelineRepoID, null,
        false);
    return ResponseDTO.newResponse(MergeInputSetResponseDTOPMS.builder()
                                       .isErrorResponse(false)
                                       .pipelineYaml(mergeInputSetTemplateRequestDTO.getRuntimeInputYaml())
                                       .completePipelineYaml(fullYaml)
                                       .build());
  }

  public ResponseDTO<InputSetSanitiseResponseDTO> sanitiseInputSet(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, String inputSetIdentifier, String pipelineBranch,
      String pipelineRepoID, GitEntityUpdateInfoDTO gitEntityInfo, @NotNull String invalidInputSetYaml,
      @NotNull String permission) throws AccessDeniedException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    String pipelineYaml = validateAndMergeHelper
                              .getPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
                                  pipelineBranch, pipelineRepoID, false, false)
                              .getYaml();
    String newInputSetYaml = InputSetSanitizer.sanitizeInputSetAndUpdateInputSetYAML(pipelineYaml, invalidInputSetYaml);
    if (EmptyPredicate.isEmpty(newInputSetYaml)) {
      return ResponseDTO.newResponse(InputSetSanitiseResponseDTO.builder().shouldDeleteInputSet(true).build());
    }

    log.info(String.format("Updating input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    newInputSetYaml = removeRuntimeInputFromYaml(pipelineYaml, newInputSetYaml);

    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, newInputSetYaml);
    InputSetEntity updatedEntity = pmsInputSetService.update(ChangeType.MODIFY, entity, false);
    return ResponseDTO.newResponse(
        InputSetSanitiseResponseDTO.builder()
            .shouldDeleteInputSet(false)
            .inputSetUpdateResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(updatedEntity))
            .build());
  }

  // Pipeline Branch is mandatory for this Api
  @NGAccessControlCheck(resourceType = PIPELINE_RESOURCE_TYPE, permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetYamlDiffDTO> getInputSetYAMLDiff(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, String inputSetIdentifier, String pipelineBranch,
      String pipelineRepoID, GitEntityUpdateInfoDTO gitEntityInfo) {
    return ResponseDTO.newResponse(InputSetValidationHelper.getYAMLDiff(gitSyncSdkService, pmsInputSetService,
        pipelineService, validateAndMergeHelper, accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        inputSetIdentifier, pipelineBranch, pipelineRepoID, inputSetsApiUtils));
  }

  public ResponseDTO<InputSetImportResponseDTO> importInputSetFromGit(@NotNull @AccountIdentifier String accountId,
      @NotNull @OrgIdentifier String orgIdentifier, @NotNull @ProjectIdentifier String projectIdentifier,
      @NotNull @ResourceIdentifier String pipelineIdentifier, String inputSetIdentifier,
      GitImportInfoDTO gitImportInfoDTO, InputSetImportRequestDTO inputSetImportRequestDTO, @NotNull String permission)
      throws AccessControlException {
    if (permissionsList.contains(permission)) {
      resourceScope = ResourceScope.builder()
                          .accountIdentifier(accountId)
                          .orgIdentifier(orgIdentifier)
                          .projectIdentifier(projectIdentifier)
                          .build();
      resource = Resource.builder().resourceType(PIPELINE_RESOURCE_TYPE).build();
    }
    if (!accessControlClient.hasAccess(resourceScope, resource, permission)) {
      throw new AccessDeniedException(ACCESS_DENIED_ERROR_MESSAGE, ErrorCode.ACCESS_DENIED, null);
    }
    InputSetEntity inputSetEntity =
        pmsInputSetService.importInputSetFromRemote(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
            inputSetIdentifier, inputSetImportRequestDTO, gitImportInfoDTO.getIsForceImport());
    return ResponseDTO.newResponse(
        InputSetImportResponseDTO.builder().identifier(inputSetEntity.getIdentifier()).build());
  }

  @Override
  public ResponseDTO<InputSetMoveConfigResponseDTO> moveConfig(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String inputSetIdentifier, InputSetMoveConfigRequestDTO inputSetMoveConfigRequestDTO,
      @NotNull String permission) {
    if (!inputSetIdentifier.equals(inputSetMoveConfigRequestDTO.getInputSetIdentifier())) {
      throw new InvalidRequestException("Identifiers given in path param and request body don't match.");
    }
    InputSetEntity movedInputSet =
        pmsInputSetService.moveConfig(accountIdentifier, orgIdentifier, projectIdentifier, inputSetIdentifier,
            InputSetMoveConfigOperationDTO.builder()
                .connectorRef(inputSetMoveConfigRequestDTO.getConnectorRef())
                .repoName(inputSetMoveConfigRequestDTO.getRepoName())
                .branch(inputSetMoveConfigRequestDTO.getBranch())
                .filePath(inputSetMoveConfigRequestDTO.getFilePath())
                .baseBranch(inputSetMoveConfigRequestDTO.getBaseBranch())
                .commitMessage(inputSetMoveConfigRequestDTO.getCommitMsg())
                .isNewBranch(inputSetMoveConfigRequestDTO.getIsNewBranch())
                .pipelineIdentifier(inputSetMoveConfigRequestDTO.getPipelineIdentifier())
                .moveConfigOperationType(io.harness.gitaware.helper.MoveConfigOperationType.getMoveConfigType(
                    inputSetMoveConfigRequestDTO.getMoveConfigOperationType()))
                .build());
    return ResponseDTO.newResponse(
        InputSetMoveConfigResponseDTO.builder().identifier(movedInputSet.getIdentifier()).build());
  }

  @Override
  public ResponseDTO<PMSInputSetListRepoResponse> getListRepos(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return ResponseDTO.newResponse(
        pmsInputSetService.getListOfRepos(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier));
  }

  @Override
  public ResponseDTO<InputSetGitUpdateResponseDTO> updateGitMetadataForInputSet(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String inputSetIdentifier,
      GitMetadataUpdateRequestInfoDTO gitMetadataUpdateRequestInfo) {
    String inputSetAfterUpdate = pmsInputSetService.updateGitMetadata(accountIdentifier, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetIdentifier,
        PMSUpdateGitDetailsParams.builder()
            .connectorRef(gitMetadataUpdateRequestInfo.getConnectorRef())
            .repoName(gitMetadataUpdateRequestInfo.getRepoName())
            .filePath(gitMetadataUpdateRequestInfo.getFilePath())
            .build());
    return ResponseDTO.newResponse(InputSetGitUpdateResponseDTO.builder().identifier(inputSetAfterUpdate).build());
  }
}
