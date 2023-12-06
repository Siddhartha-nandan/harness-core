/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.mapper;

import static io.harness.NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.springdata.SpringDataMongoUtils.populateInFilter;

import static java.util.Collections.emptyList;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterDTO;
import io.harness.ngtriggers.beans.dto.BulkTriggersRequestDTO;
import io.harness.ngtriggers.beans.dto.NGTriggersFilterPropertiesDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory.TriggerEventHistoryKeys;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent.TriggerWebhookEventsKeys;
import io.harness.ngtriggers.beans.source.NGTriggerType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@UtilityClass
@OwnedBy(PIPELINE)
public class TriggerFilterHelper {
  public Criteria createCriteriaForGetList(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, NGTriggerType type, String searchTerm, boolean deleted, String filterIdentifier,
      NGTriggersFilterPropertiesDTO filterProperties, FilterDTO triggerFilterDTO) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountIdentifier)) {
      criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (isNotEmpty(targetIdentifier)) {
      criteria.and(NGTriggerEntityKeys.targetIdentifier).is(targetIdentifier);
    }
    criteria.and(NGTriggerEntityKeys.deleted).is(deleted);

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      populateFilterUsingIdentifier(criteria, filterIdentifier, searchTerm, triggerFilterDTO);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      populateFilter(criteria, filterProperties, searchTerm);
    }
    if (type != null) {
      criteria.and(NGTriggerEntityKeys.type).is(type);
    }
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  private void populateFilterUsingIdentifier(
      Criteria criteria, @NotNull String filterIdentifier, String searchTerm, FilterDTO triggerFilterDTO) {
    if (triggerFilterDTO == null) {
      throw new InvalidRequestException("Could not find a Trigger filter with the identifier " + filterIdentifier);
    } else {
      populateFilter(criteria, (NGTriggersFilterPropertiesDTO) triggerFilterDTO.getFilterProperties(), searchTerm);
    }
  }

  private static void populateFilter(
      Criteria criteria, @NotNull NGTriggersFilterPropertiesDTO triggerFilter, String searchTerm) {
    populateInFilter(criteria, NGTriggerEntityKeys.identifier, triggerFilter.getTriggerIdentifiers());
    List<Criteria> criteriaList = new ArrayList<>();

    Criteria nameFilter = getCaseInsensitiveFilter(NGTriggerEntityKeys.name, triggerFilter.getTriggerNames());
    if (nameFilter != null) {
      criteriaList.add(nameFilter);
    }
    Criteria searchTermCriteria = getSearchTermCriteria(searchTerm);
    if (searchTermCriteria != null) {
      criteriaList.add(searchTermCriteria);
    }
    if (criteriaList.size() != 0) {
      criteria.andOperator(criteriaList.toArray(new Criteria[0]));
    }
    populateInFilter(criteria, NGTriggerEntityKeys.type, triggerFilter.getTriggerTypes());
  }

  private static Criteria getSearchTermCriteria(String searchTerm) {
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      return new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS));
    }
    return null;
  }

  private static Criteria getCaseInsensitiveFilter(String fieldName, List<String> values) {
    if (isNotEmpty(values)) {
      List<Criteria> criteriaForCaseInsensitive =
          values.stream()
              .map(value -> where(fieldName).regex(value, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS))
              .collect(Collectors.toList());
      return new Criteria().orOperator(criteriaForCaseInsensitive.toArray(new Criteria[0]));
    }
    return null;
  }

  public Criteria createCriteriaForCustomWebhookTriggerGetList(
      TriggerWebhookEvent triggerWebhookEvent, String searchTerm, boolean deleted, boolean enabled) {
    Criteria criteria = createCriteriaForWebhookTriggerGetList(triggerWebhookEvent.getAccountId(),
        triggerWebhookEvent.getOrgIdentifier(), triggerWebhookEvent.getProjectIdentifier(), emptyList(), searchTerm,
        deleted, enabled);

    if (triggerWebhookEvent.getPipelineIdentifier() != null) {
      criteria.and(NGTriggerEntityKeys.targetIdentifier).is(triggerWebhookEvent.getPipelineIdentifier());
    }
    if (triggerWebhookEvent.getTriggerIdentifier() != null) {
      criteria.and(NGTriggerEntityKeys.identifier).is(triggerWebhookEvent.getTriggerIdentifier());
    }
    criteria.and("metadata.webhook.type").regex("CUSTOM", CASE_INSENSITIVE_MONGO_OPTIONS);
    return criteria;
  }

  public Criteria createCriteriaFormWebhookTriggerGetListByRepoType(
      TriggerWebhookEvent triggerWebhookEvent, String searchTerm, boolean deleted, boolean enabled) {
    Criteria criteria = createCriteriaForWebhookTriggerGetList(
        triggerWebhookEvent.getAccountId(), null, null, emptyList(), searchTerm, deleted, enabled);
    criteria.and("metadata.webhook.type")
        .regex(triggerWebhookEvent.getSourceRepoType(), CASE_INSENSITIVE_MONGO_OPTIONS);
    return criteria;
  }

  public Criteria createCriteriaFormBuildTriggerUsingAccIdAndSignature(String accountId, List<String> signatures) {
    Criteria criteria = new Criteria();
    criteria.and(NGTriggerEntityKeys.accountId).is(accountId);
    criteria.and("metadata.buildMetadata.pollingConfig.signature").in(signatures);
    criteria.and(NGTriggerEntityKeys.deleted).is(false);
    criteria.and(NGTriggerEntityKeys.enabled).is(true);

    // Criteria for multi-artifact Trigger should query in `multiBuildMetadata` instead of `buildMetadata`.
    Criteria criteriaForMultiArtifact = new Criteria();
    criteriaForMultiArtifact.and(NGTriggerEntityKeys.accountId).is(accountId);
    criteriaForMultiArtifact.and("metadata.multiBuildMetadata")
        .elemMatch(Criteria.where("pollingConfig.signature").in(signatures));
    criteriaForMultiArtifact.and(NGTriggerEntityKeys.deleted).is(false);
    criteriaForMultiArtifact.and(NGTriggerEntityKeys.enabled).is(true);
    return new Criteria().orOperator(criteria, criteriaForMultiArtifact);
  }

  public Criteria createCriteriaForWebhookTriggerGetList(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<String> repoURLs, String searchTerm, boolean deleted, boolean enabledOnly) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(accountIdentifier)) {
      criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    }
    if (isNotEmpty(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (isNotEmpty(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    criteria.and(NGTriggerEntityKeys.deleted).is(deleted);
    criteria.and(NGTriggerEntityKeys.type).is(NGTriggerType.WEBHOOK);
    if (enabledOnly) {
      criteria.and(NGTriggerEntityKeys.enabled).is(true);
    }

    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria = new Criteria().orOperator(
          where(NGTriggerEntityKeys.identifier).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS),
          where(NGTriggerEntityKeys.name).regex(searchTerm, CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  public Update getUpdateOperations(NGTriggerEntity triggerEntity) {
    Update update = new Update();
    long timeOfUpdate = System.currentTimeMillis();
    update.set(NGTriggerEntityKeys.name, triggerEntity.getName());
    update.set(NGTriggerEntityKeys.identifier, triggerEntity.getIdentifier());
    update.set(NGTriggerEntityKeys.description, triggerEntity.getDescription());
    update.set(NGTriggerEntityKeys.yaml, triggerEntity.getYaml());
    update.set(NGTriggerEntityKeys.lastModifiedAt, timeOfUpdate);

    update.set(NGTriggerEntityKeys.type, triggerEntity.getType());
    update.set(NGTriggerEntityKeys.metadata, triggerEntity.getMetadata());
    update.set(NGTriggerEntityKeys.enabled, triggerEntity.getEnabled());
    update.set(NGTriggerEntityKeys.tags, triggerEntity.getTags());
    update.set(NGTriggerEntityKeys.deleted, false);
    update.set(NGTriggerEntityKeys.triggerStatus, triggerEntity.getTriggerStatus());
    update.set(NGTriggerEntityKeys.triggerConfigWrapper, triggerEntity.getTriggerConfigWrapper());
    update.set(NGTriggerEntityKeys.harnessVersion, triggerEntity.getHarnessVersion());
    if (triggerEntity.getPollInterval() != null) {
      update.set(NGTriggerEntityKeys.pollInterval, triggerEntity.getPollInterval());
    }
    if (triggerEntity.getWebhookId() != null) {
      update.set(NGTriggerEntityKeys.webhookId, triggerEntity.getWebhookId());
    }
    if (triggerEntity.getEncryptedWebhookSecretIdentifier() != null) {
      update.set(
          NGTriggerEntityKeys.encryptedWebhookSecretIdentifier, triggerEntity.getEncryptedWebhookSecretIdentifier());
    }
    if (triggerEntity.getStagesToExecute() != null) {
      update.set(NGTriggerEntityKeys.stagesToExecute, triggerEntity.getStagesToExecute());
    }
    if (triggerEntity.getNextIterations() != null) {
      update.set(NGTriggerEntityKeys.nextIterations, triggerEntity.getNextIterations());
    }

    return update;
  }

  public Update getUpdateOperations(TriggerWebhookEvent triggerWebhookEvent) {
    Update update = new Update();
    update.set(TriggerWebhookEventsKeys.attemptCount, triggerWebhookEvent.getAttemptCount());
    update.set(TriggerWebhookEventsKeys.processing, triggerWebhookEvent.isProcessing());
    return update;
  }

  public Update getUpdateOperationsForDelete() {
    Update update = new Update();
    update.set(NGTriggerEntityKeys.deleted, true);
    update.set(NGTriggerEntityKeys.enabled, false);
    return update;
  }

  public Criteria createCriteriaForTriggerEventCountLastNDays(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String triggerIdentifier, String targetIdentifier, long startTime) {
    Criteria criteria = new Criteria();
    criteria.and(TriggerEventHistoryKeys.accountId).is(accountIdentifier);
    criteria.and(TriggerEventHistoryKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(TriggerEventHistoryKeys.projectIdentifier).is(projectIdentifier);
    criteria.and(TriggerEventHistoryKeys.triggerIdentifier).is(triggerIdentifier);
    criteria.and(TriggerEventHistoryKeys.targetIdentifier).is(targetIdentifier);
    criteria.and(TriggerEventHistoryKeys.executionNotAttempted).ne(true);
    criteria.and(TriggerEventHistoryKeys.createdAt).gte(startTime);

    return criteria;
  }

  public Criteria getCriteriaForTogglingTriggersInBulk(
      String accountIdentifier, BulkTriggersRequestDTO bulkTriggersRequestDTO) {
    String orgIdentifier = null;
    String projectIdentifier = null;
    String pipelineIdentifier = null;
    String type = null;

    if (bulkTriggersRequestDTO.getFilters() != null) {
      orgIdentifier = bulkTriggersRequestDTO.getFilters().getOrgIdentifier();
      projectIdentifier = bulkTriggersRequestDTO.getFilters().getProjectIdentifier();
      pipelineIdentifier = bulkTriggersRequestDTO.getFilters().getPipelineIdentifier();
      type = bulkTriggersRequestDTO.getFilters().getType();
    }

    if (StringUtils.isBlank(accountIdentifier)) {
      throw new InvalidRequestException(
          "accountIdentifier parameter cannot be null. Please input a valid accountIdentifier.");
    }
    if (StringUtils.isBlank(orgIdentifier) && StringUtils.isNotBlank(projectIdentifier)) {
      throw new InvalidRequestException(
          "Please input a valid orgIdentifier for the given projectIdentifier [" + projectIdentifier + "]");
    }
    if (StringUtils.isAnyBlank(projectIdentifier, orgIdentifier) && StringUtils.isNotBlank(pipelineIdentifier)) {
      throw new InvalidRequestException(
          "Please input a valid orgIdentifier and projectIdentifier for the given pipelineIdentifier ["
          + pipelineIdentifier + "]");
    }

    Criteria criteria = new Criteria();
    criteria.and(NGTriggerEntityKeys.accountId).is(accountIdentifier);
    criteria.and(NGTriggerEntityKeys.deleted).is(false);
    criteria.and(NGTriggerEntityKeys.enabled).is(!bulkTriggersRequestDTO.getData().isEnable());

    if (StringUtils.isNotBlank(orgIdentifier)) {
      criteria.and(NGTriggerEntityKeys.orgIdentifier).is(orgIdentifier);
    }
    if (StringUtils.isNotBlank(projectIdentifier)) {
      criteria.and(NGTriggerEntityKeys.projectIdentifier).is(projectIdentifier);
    }
    if (StringUtils.isNotBlank(pipelineIdentifier)) {
      criteria.and(NGTriggerEntityKeys.targetIdentifier).is(pipelineIdentifier);
    }
    if (isNotEmpty(type)) {
      criteria.and(NGTriggerEntityKeys.type).is(type);
    }

    return criteria;
  }
}
