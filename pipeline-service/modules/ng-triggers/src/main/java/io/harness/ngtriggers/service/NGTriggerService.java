/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.WebhookEventProcessingDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.validations.ValidationResult;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface NGTriggerService {
  NGTriggerEntity create(NGTriggerEntity ngTriggerEntity, boolean serviceV1);

  Optional<NGTriggerEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, String identifier, boolean deleted);

  NGTriggerEntity update(NGTriggerEntity ngTriggerEntity, boolean serviceV2);

  boolean updateTriggerStatus(NGTriggerEntity ngTriggerEntity, boolean status, boolean serviceV2);

  Page<NGTriggerEntity> list(Criteria criteria, Pageable pageable);

  List<NGTriggerEntity> listEnabledTriggersForCurrentProject(
      String accountId, String orgIdentifier, String projectIdentifier);

  List<NGTriggerEntity> listEnabledTriggersForAccount(String accountId);

  List<NGTriggerEntity> findTriggersForCustomWehbook(
      TriggerWebhookEvent triggerWebhookEvent, boolean isDeleted, boolean enabled);
  List<NGTriggerEntity> findTriggersForWehbookBySourceRepoType(
      TriggerWebhookEvent triggerWebhookEvent, boolean isDeleted, boolean enabled);
  List<NGTriggerEntity> findBuildTriggersByAccountIdAndSignature(String accountId, List<String> signatures);
  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String targetIdentifier,
      String identifier, Long version);

  TriggerWebhookEvent addEventToQueue(TriggerWebhookEvent webhookEventQueueRecord);
  TriggerWebhookEvent updateTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
  void deleteTriggerWebhookEvent(TriggerWebhookEvent webhookEventQueueRecord);
  List<ConnectorResponseDTO> fetchConnectorsByFQN(String accountId, List<String> fqns);

  void validateTriggerConfig(TriggerDetails triggerDetails, boolean serviceV2);
  void validateInputSets(TriggerDetails triggerDetails);
  boolean deleteAllForPipeline(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);

  WebhookEventProcessingDetails fetchTriggerEventHistory(String accountId, String eventId);
  NGTriggerEntity updateTriggerWithValidationStatus(NGTriggerEntity ngTriggerEntity, ValidationResult validationResult);
  Map<String, Map<String, String>> generateErrorMap(InputSetErrorWrapperDTOPMS inputSetErrorWrapperDTOPMS);
  TriggerDetails fetchTriggerEntity(
      String accountId, String orgId, String projectId, String pipelineId, String triggerId, String newYaml, boolean serviceV2);
  Object fetchExecutionSummaryV2(String planExecutionId, String accountId, String orgId, String projectId);
}
