/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitxwebhooks.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eventsframework.EventsFrameworkConstants.GITX_WEBHOOK_EVENT;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.exception.InternalServerErrorException;
import io.harness.gitsync.common.beans.GitXWebhookEventStatus;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventUpdateRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventsListResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXWebhookEventUpdateInfo;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ListGitXWebhookResponseDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookCriteriaDTO;
import io.harness.gitsync.gitxwebhooks.dtos.UpdateGitXWebhookRequestDTO;
import io.harness.gitsync.gitxwebhooks.entity.Author;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhook;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhookEvent;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhookEvent.GitXWebhookEventKeys;
import io.harness.gitsync.gitxwebhooks.helper.GitXWebhookHelper;
import io.harness.gitsync.gitxwebhooks.helper.GitXWebhookTriggerHelper;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookEventLogContext;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookLogContext;
import io.harness.gitsync.gitxwebhooks.observer.GitXWebhookEventUpdateObserver;
import io.harness.gitsync.gitxwebhooks.utils.GitXWebhookUtils;
import io.harness.hsqs.client.api.HsqsClientService;
import io.harness.hsqs.client.model.EnqueueRequest;
import io.harness.hsqs.client.model.EnqueueResponse;
import io.harness.observer.Subject;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.repositories.gitxwebhook.GitXWebhookEventsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class GitXWebhookEventServiceImpl implements GitXWebhookEventService {
  @Inject GitXWebhookEventsRepository gitXWebhookEventsRepository;
  @Inject GitXWebhookService gitXWebhookService;
  @Inject HsqsClientService hsqsClientService;
  @Inject GitXWebhookTriggerHelper gitXWebhookTriggerHelper;
  @Inject GitXWebhookHelper gitXWebhookHelper;

  private static final String QUEUE_TOPIC_PREFIX = "ng";
  private static final String WEBHOOK_FAILURE_ERROR_MESSAGE =
      "Unexpected error occurred while [%s] git webhook. Please contact Harness Support.";
  private static final String LISTING_EVENTS = "listing events";

  @Getter private final Subject<GitXWebhookEventUpdateObserver> gitXWebhookEventUpdateSubject = new Subject<>();

  @Override
  public void processEvent(WebhookDTO webhookDTO) {
    try (GitXWebhookEventLogContext context = new GitXWebhookEventLogContext(webhookDTO)) {
      try {
        if (!gitXWebhookHelper.isBiDirectionalSyncEnabledInSettings(webhookDTO.getAccountId())) {
          gitXWebhookTriggerHelper.startTriggerExecution(webhookDTO);
          return;
        }
        List<GitXWebhook> gitXWebhookList =
            fetchGitXWebhook(webhookDTO.getAccountId(), webhookDTO.getParsedResponse().getPush().getRepo().getName());
        if (isEmpty(gitXWebhookList)) {
          log.info(
              String.format("Skipping processing of event [%s] as no GitX Webhook found.", webhookDTO.getEventId()));
          gitXWebhookTriggerHelper.startTriggerExecution(webhookDTO);
          return;
        }
        GitXWebhookEvent gitXWebhookEvent = buildGitXWebhookEvent(webhookDTO, gitXWebhookList);
        if (GitXWebhookUtils.isNullCommitId(gitXWebhookEvent.getAfterCommitId())
            || GitXWebhookUtils.isNullCommitId(gitXWebhookEvent.getBeforeCommitId())) {
          log.info(String.format(
              "Skipping processing of event [%s] as it as NULL commit id for either before or after commit id",
              webhookDTO.getEventId()));
          return;
        }
        GitXWebhookEvent createdGitXWebhookEvent = gitXWebhookEventsRepository.create(gitXWebhookEvent);
        updateGitXWebhook(gitXWebhookList, webhookDTO.getTime());
        enqueueWebhookEvents(webhookDTO);
        log.info(
            String.format("Successfully created the webhook event %s", createdGitXWebhookEvent.getEventIdentifier()));
      } catch (Exception exception) {
        log.error("Failed to process the webhook event {}", webhookDTO.getEventId(), exception);
        gitXWebhookTriggerHelper.startTriggerExecution(webhookDTO);
        throw new InternalServerErrorException(
            String.format("Failed to process the webhook event [%s].", webhookDTO.getEventId()));
      }
    }
  }

  @Override
  public GitXEventsListResponseDTO listEvents(GitXEventsListRequestDTO gitXEventsListRequestDTO) {
    try (GitXWebhookLogContext context = new GitXWebhookLogContext(gitXEventsListRequestDTO)) {
      try {
        if (isNotEmpty(gitXEventsListRequestDTO.getRepoName())) {
          GitXWebhook gitXWebhook = fetchGitXWebhookForGivenScope(
              gitXEventsListRequestDTO.getScope(), gitXEventsListRequestDTO.getRepoName());
          if (gitXWebhook != null) {
            gitXEventsListRequestDTO.setWebhookIdentifier(gitXWebhook.getIdentifier());
          }
        }
        List<String> gitxWebhookIdentifiers = new ArrayList<>();
        if (isEmpty(gitXEventsListRequestDTO.getWebhookIdentifier())) {
          gitxWebhookIdentifiers = getGitXWebhookIdentifiers(gitXEventsListRequestDTO);
          if (isEmpty(gitxWebhookIdentifiers)) {
            return GitXEventsListResponseDTO.builder().build();
          }
        }
        Query query = buildEventsListQuery(gitXEventsListRequestDTO, gitxWebhookIdentifiers);
        List<GitXWebhookEvent> gitXWebhookEventList = gitXWebhookEventsRepository.list(query);
        return GitXEventsListResponseDTO.builder()
            .gitXEventDTOS(prepareGitXWebhookEvents(gitXEventsListRequestDTO, gitXWebhookEventList))
            .build();
      } catch (Exception exception) {
        log.error(String.format("Error occurred while GitX listing events in account %s",
            gitXEventsListRequestDTO.getScope().getAccountIdentifier()));
        throw new InternalServerErrorException(String.format(WEBHOOK_FAILURE_ERROR_MESSAGE, LISTING_EVENTS));
      }
    }
  }

  @Override
  public void updateEvent(
      String accountIdentifier, String eventIdentifier, GitXEventUpdateRequestDTO gitXEventUpdateRequestDTO) {
    Criteria criteria = buildCriteria(accountIdentifier, eventIdentifier);
    Query query = new Query(criteria);
    Update update = buildGitXWebhookEventUpdate(gitXEventUpdateRequestDTO);
    gitXWebhookEventsRepository.update(query, update);
    gitXWebhookEventUpdateSubject.fireInform(GitXWebhookEventUpdateObserver::onGitXWebhookEventUpdate,
        GitXWebhookEventUpdateInfo.builder()
            .eventStatus(gitXEventUpdateRequestDTO.getGitXWebhookEventStatus().name())
            .webhookDTO(gitXEventUpdateRequestDTO.getWebhookDTO())
            .build());
  }

  private Criteria buildCriteria(String accountIdentifier, String eventIdentifier) {
    return Criteria.where(GitXWebhookEventKeys.accountIdentifier)
        .is(accountIdentifier)
        .and(GitXWebhookEventKeys.eventIdentifier)
        .is(eventIdentifier);
  }

  private Update buildGitXWebhookEventUpdate(GitXEventUpdateRequestDTO gitXEventUpdateRequestDTO) {
    Update update = new Update();
    update.set(GitXWebhookEventKeys.eventStatus, gitXEventUpdateRequestDTO.getGitXWebhookEventStatus().name());
    if (isNotEmpty(gitXEventUpdateRequestDTO.getProcessedFilePaths())) {
      update.set(GitXWebhookEventKeys.processedFilePaths, gitXEventUpdateRequestDTO.getProcessedFilePaths());
    }
    return update;
  }

  private List<GitXEventDTO> prepareGitXWebhookEvents(
      GitXEventsListRequestDTO gitXEventsListRequestDTO, List<GitXWebhookEvent> gitXWebhookEventList) {
    List<GitXEventDTO> gitXEventList = new ArrayList<>();
    for (GitXWebhookEvent gitXWebhookEvent : gitXWebhookEventList) {
      if (isEmpty(gitXEventsListRequestDTO.getFilePath())) {
        gitXEventList.add(buildGitXEventDTO(gitXWebhookEvent, gitXEventsListRequestDTO.getWebhookIdentifier()));
      } else if (isNotEmpty(gitXEventsListRequestDTO.getFilePath())
          && isFilePathMatching(gitXEventsListRequestDTO.getFilePath(), gitXWebhookEvent.getProcessedFilePaths())) {
        gitXEventList.add(buildGitXEventDTO(gitXWebhookEvent, gitXEventsListRequestDTO.getWebhookIdentifier()));
      }
    }
    return gitXEventList;
  }

  private GitXEventDTO buildGitXEventDTO(GitXWebhookEvent gitXWebhookEvent, String webhookIdentifier) {
    return GitXEventDTO.builder()
        .webhookIdentifier(getWebhookIdentifier(webhookIdentifier, gitXWebhookEvent))
        .authorName(gitXWebhookEvent.getAuthor().getName())
        .eventTriggerTime(gitXWebhookEvent.getEventTriggeredTime())
        .payload(gitXWebhookEvent.getPayload())
        .eventIdentifier(gitXWebhookEvent.getEventIdentifier())
        .eventStatus(gitXWebhookEvent.getEventStatus())
        .build();
  }

  private String getWebhookIdentifier(String webhookIdentifier, GitXWebhookEvent gitXWebhookEvent) {
    return isNotEmpty(webhookIdentifier)                          ? webhookIdentifier
        : isNotEmpty(gitXWebhookEvent.getWebhookIdentifierList()) ? gitXWebhookEvent.getWebhookIdentifierList().get(0)
                                                                  : null;
  }

  private boolean isFilePathMatching(String entityFilePath, List<String> modifiedFilePaths) {
    return isNotEmpty(
        GitXWebhookUtils.compareFolderPaths(Collections.singletonList(entityFilePath), modifiedFilePaths));
  }

  private Criteria buildEventsListCriteria(
      GitXEventsListRequestDTO gitXEventsListRequestDTO, List<String> gitxWebhookIdentifiers) {
    Criteria criteria = new Criteria();
    criteria.and(GitXWebhookEventKeys.accountIdentifier).is(gitXEventsListRequestDTO.getScope().getAccountIdentifier());
    if (isNotEmpty(gitXEventsListRequestDTO.getWebhookIdentifier())) {
      criteria.and(GitXWebhookEventKeys.webhookIdentifierList).is(gitXEventsListRequestDTO.getWebhookIdentifier());
    } else {
      if (isNotEmpty(gitxWebhookIdentifiers)) {
        criteria.and(GitXWebhookEventKeys.webhookIdentifierList).in(gitxWebhookIdentifiers);
      }
    }
    if (gitXEventsListRequestDTO.getEventStartTime() != null && gitXEventsListRequestDTO.getEventEndTime() != null) {
      criteria.and(GitXWebhookEventKeys.eventTriggeredTime)
          .gte(gitXEventsListRequestDTO.getEventStartTime())
          .lte(gitXEventsListRequestDTO.getEventEndTime());
    }
    if (isNotEmpty(gitXEventsListRequestDTO.getRepoName())) {
      criteria.and(GitXWebhookEventKeys.repo).is(gitXEventsListRequestDTO.getRepoName());
    }
    if (isNotEmpty(gitXEventsListRequestDTO.getEventIdentifier())) {
      criteria.and(GitXWebhookEventKeys.eventIdentifier).is(gitXEventsListRequestDTO.getEventIdentifier());
    }
    if (isNotEmpty(gitXEventsListRequestDTO.getEventStatus())) {
      criteria.and(GitXWebhookEventKeys.eventStatus).in(gitXEventsListRequestDTO.getEventStatus());
    }
    return criteria;
  }

  private List<String> getGitXWebhookIdentifiers(GitXEventsListRequestDTO gitXEventsListRequestDTO) {
    ListGitXWebhookResponseDTO listGitXWebhookResponseDTO = gitXWebhookService.listGitXWebhooks(
        ListGitXWebhookRequestDTO.builder().scope(gitXEventsListRequestDTO.getScope()).build());
    List<String> gitxWebhookIdentifiers = new ArrayList<>();
    if (listGitXWebhookResponseDTO != null && isNotEmpty(listGitXWebhookResponseDTO.getGitXWebhooksList())) {
      listGitXWebhookResponseDTO.getGitXWebhooksList().forEach(
          gitXWebhookResponseDTO -> { gitxWebhookIdentifiers.add(gitXWebhookResponseDTO.getWebhookIdentifier()); });
    }
    return gitxWebhookIdentifiers;
  }

  private Query buildEventsListQuery(
      GitXEventsListRequestDTO gitXEventsListRequestDTO, List<String> gitxWebhookIdentifiers) {
    Criteria criteria = buildEventsListCriteria(gitXEventsListRequestDTO, gitxWebhookIdentifiers);
    Query query = new Query(criteria);
    query.addCriteria(Criteria.where(GitXWebhookEventKeys.createdAt).exists(true))
        .with(Sort.by(Sort.Direction.DESC, GitXWebhookEventKeys.createdAt));
    return query;
  }

  private List<GitXWebhook> fetchGitXWebhook(String accountIdentifier, String repoName) {
    return gitXWebhookService.getGitXWebhook(accountIdentifier, repoName);
  }

  private GitXWebhook fetchGitXWebhookForGivenScope(Scope scope, String repoName) {
    Optional<GitXWebhook> optionalGitXWebhook = gitXWebhookService.getGitXWebhookForGivenScopes(scope, repoName);
    if (optionalGitXWebhook.isEmpty()) {
      return null;
    }
    return optionalGitXWebhook.get();
  }

  private GitXWebhookEvent buildGitXWebhookEvent(WebhookDTO webhookDTO, List<GitXWebhook> gitXWebhookList) {
    return GitXWebhookEvent.builder()
        .accountIdentifier(webhookDTO.getAccountId())
        .eventIdentifier(webhookDTO.getEventId())
        .author(buildAuthor(webhookDTO))
        .eventTriggeredTime(webhookDTO.getTime())
        .eventStatus(GitXWebhookEventStatus.QUEUED.name())
        .payload(webhookDTO.getJsonPayload())
        .afterCommitId(webhookDTO.getParsedResponse().getPush().getAfter())
        .beforeCommitId(webhookDTO.getParsedResponse().getPush().getBefore())
        .branch(getBranch(webhookDTO))
        .repo(webhookDTO.getParsedResponse().getPush().getRepo().getName())
        .webhookIdentifierList(getWebhookIdentifiers(gitXWebhookList))
        .build();
  }

  private List<String> getWebhookIdentifiers(List<GitXWebhook> gitXWebhookList) {
    List<String> webhookIdentifiers = new ArrayList<>();
    gitXWebhookList.forEach(gitXWebhook -> { webhookIdentifiers.add(gitXWebhook.getIdentifier()); });
    return webhookIdentifiers;
  }

  private String getBranch(WebhookDTO webhookDTO) {
    String ref = webhookDTO.getParsedResponse().getPush().getRef();
    return ref.replaceFirst("^refs/heads/", "");
  }

  private Author buildAuthor(WebhookDTO webhookDTO) {
    return Author.builder().name(webhookDTO.getParsedResponse().getPush().getCommit().getAuthor().getName()).build();
  }

  private void updateGitXWebhook(List<GitXWebhook> gitXWebhookList, long triggerEventTime) {
    gitXWebhookList.forEach(gitXWebhook -> {
      gitXWebhookService.updateGitXWebhook(UpdateGitXWebhookCriteriaDTO.builder()
                                               .webhookIdentifier(gitXWebhook.getIdentifier())
                                               .scope(Scope.of(gitXWebhook.getAccountIdentifier(),
                                                   gitXWebhook.getOrgIdentifier(), gitXWebhook.getProjectIdentifier()))
                                               .build(),
          UpdateGitXWebhookRequestDTO.builder()
              .lastEventTriggerTime(triggerEventTime)
              .folderPaths(gitXWebhook.getFolderPaths())
              .build());
    });
  }

  private void enqueueWebhookEvents(WebhookDTO webhookDTO) {
    EnqueueRequest enqueueRequest = EnqueueRequest.builder()
                                        .topic(QUEUE_TOPIC_PREFIX + GITX_WEBHOOK_EVENT)
                                        .subTopic(webhookDTO.getAccountId())
                                        .producerName(QUEUE_TOPIC_PREFIX + GITX_WEBHOOK_EVENT)
                                        .payload(RecastOrchestrationUtils.toJson(webhookDTO))
                                        .build();
    EnqueueResponse execute = hsqsClientService.enqueue(enqueueRequest);
    log.info("GitXWebhook event queued message id: {} for eventIdentifier: {}", execute.getItemId(),
        webhookDTO.getEventId());
  }
}
