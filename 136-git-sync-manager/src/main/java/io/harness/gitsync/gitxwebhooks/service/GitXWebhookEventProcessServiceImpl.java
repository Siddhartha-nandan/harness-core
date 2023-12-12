/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitxwebhooks.service;

import static io.harness.authorization.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.gitsync.common.beans.GitXWebhookEventStatus;
import io.harness.gitsync.common.dtos.GitDiffResultFileDTO;
import io.harness.gitsync.common.dtos.GitDiffResultFileListDTO;
import io.harness.gitsync.common.helper.GitRepoHelper;
import io.harness.gitsync.common.service.GitSyncConnectorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.gitxwebhooks.dtos.GitXCacheUpdateHelperRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.GitXEventUpdateRequestDTO;
import io.harness.gitsync.gitxwebhooks.dtos.ProcessingFilePathResponseDTO;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhook;
import io.harness.gitsync.gitxwebhooks.entity.GitXWebhookEvent;
import io.harness.gitsync.gitxwebhooks.loggers.GitXWebhookEventLogContext;
import io.harness.gitsync.gitxwebhooks.runnable.GitXWebhookCacheUpdateHelper;
import io.harness.gitsync.gitxwebhooks.utils.GitXWebhookUtils;
import io.harness.repositories.gitxwebhook.GitXWebhookEventsRepository;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_GITX})
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class GitXWebhookEventProcessServiceImpl implements GitXWebhookEventProcessService {
  @Inject private GitXWebhookService gitXWebhookService;
  @Inject private GitXWebhookEventsRepository gitXWebhookEventsRepository;
  @Inject private GitXWebhookCacheUpdateHelper gitXWebhookCacheUpdateHelper;
  @Inject private GitXWebhookEventService gitXWebhookEventService;
  @Inject private ScmOrchestratorService scmOrchestratorService;
  @Inject private GitSyncConnectorService gitSyncConnectorService;
  @Inject private GitRepoHelper gitRepoHelper;

  @Override
  public void processEvent(WebhookDTO webhookDTO) {
    try (GitXWebhookEventLogContext context = new GitXWebhookEventLogContext(webhookDTO)) {
      try {
        GitXWebhookEvent gitXWebhookEvent = gitXWebhookEventsRepository.findByAccountIdentifierAndEventIdentifier(
            webhookDTO.getAccountId(), webhookDTO.getEventId());
        processQueuedEvent(gitXWebhookEvent, webhookDTO);
      } catch (Exception exception) {
        log.error("Exception occurred while processing the event {}", webhookDTO.getEventId(), exception);
        throw exception;
      }
    }
  }

  private void processQueuedEvent(GitXWebhookEvent gitXWebhookEvent, WebhookDTO webhookDTO) {
    try (GitXWebhookEventLogContext context = new GitXWebhookEventLogContext(gitXWebhookEvent)) {
      try {
        SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
        log.info(String.format("Picked the event %s from the queue.", gitXWebhookEvent.getEventIdentifier()));
        List<GitXWebhook> gitXWebhookList = getGitXWebhook(gitXWebhookEvent);
        if (isEmpty(gitXWebhookList)) {
          log.info(String.format("The webhook event %s will be SKIPPED as there is no webhook(s) configured.",
              gitXWebhookEvent.getEventIdentifier()));
          updateEventStatus(gitXWebhookEvent.getAccountIdentifier(), gitXWebhookEvent.getEventIdentifier(),
              GitXWebhookEventStatus.SKIPPED, webhookDTO);
          return;
        }
        ProcessingFilePathResponseDTO processingFilePathResponseDTO =
            getFilesToProcess(gitXWebhookList, gitXWebhookEvent);
        if (isEmpty(processingFilePathResponseDTO.getProcessingFilePaths())) {
          log.info(String.format(
              "The webhook event %s will be SKIPPED as the webhook(s) is disabled or the folder paths don't match.",
              gitXWebhookEvent.getEventIdentifier()));
          updateEventStatus(gitXWebhookEvent.getAccountIdentifier(), gitXWebhookEvent.getEventIdentifier(),
              GitXWebhookEventStatus.SKIPPED, webhookDTO);
        } else {
          log.info(String.format(
              "Submitting the task for PROCESSING the webhook event %s as the webhook(s) is enabled and the folder paths match.",
              gitXWebhookEvent.getEventIdentifier()));
          gitXWebhookCacheUpdateHelper.submitTask(gitXWebhookEvent.getEventIdentifier(),
              buildGitXWebhookRunnableRequest(processingFilePathResponseDTO.getGitXWebhook(), gitXWebhookEvent,
                  processingFilePathResponseDTO.getModifiedFilePaths(), processingFilePathResponseDTO.getScmConnector(),
                  webhookDTO));
          updateEventStatus(gitXWebhookEvent.getAccountIdentifier(), gitXWebhookEvent.getEventIdentifier(),
              GitXWebhookEventStatus.PROCESSING, processingFilePathResponseDTO.getProcessingFilePaths(), webhookDTO);
        }
      } catch (ConnectorNotFoundException connectorNotFoundException) {
        log.error(String.format("Connector not found for event %s in the account %s.",
                      gitXWebhookEvent.getEventIdentifier(), gitXWebhookEvent.getAccountIdentifier()),
            connectorNotFoundException);
        markEventFailed(gitXWebhookEvent, webhookDTO);
      } catch (Exception exception) {
        log.error(
            "Exception occurred while processing the event {} ", gitXWebhookEvent.getEventIdentifier(), exception);
        markEventFailed(gitXWebhookEvent, webhookDTO);
      }
    }
  }

  private GitXCacheUpdateHelperRequestDTO buildGitXWebhookRunnableRequest(GitXWebhook gitXWebhook,
      GitXWebhookEvent gitXWebhookEvent, List<String> modifiedFilePaths, ScmConnector scmConnector,
      WebhookDTO webhookDTO) {
    return GitXCacheUpdateHelperRequestDTO.builder()
        .scope(Scope.of(
            gitXWebhook.getAccountIdentifier(), gitXWebhook.getOrgIdentifier(), gitXWebhook.getProjectIdentifier()))
        .repoName(gitXWebhook.getRepoName())
        .branch(gitXWebhookEvent.getBranch())
        .connectorRef(gitXWebhook.getConnectorRef())
        .eventIdentifier(gitXWebhookEvent.getEventIdentifier())
        .modifiedFilePaths(modifiedFilePaths)
        .scmConnector(scmConnector)
        .webhookDTO(webhookDTO)
        .build();
  }

  private ProcessingFilePathResponseDTO getFilesToProcess(
      List<GitXWebhook> gitXWebhookList, GitXWebhookEvent gitXWebhookEvent) {
    Set<String> modifiedFilePaths = new HashSet<>();
    Set<String> processingFilePaths = new HashSet<>();
    ScmConnector scmConnector = null;
    GitXWebhook gitXWebhookFinal = null;
    for (GitXWebhook gitXWebhook : gitXWebhookList) {
      if (gitXWebhook.getIsEnabled()) {
        try {
          if (isEmpty(modifiedFilePaths)) {
            scmConnector = getScmConnector(gitXWebhook);
            modifiedFilePaths.addAll(parsePayloadAndGetModifiedFilePaths(gitXWebhook, gitXWebhookEvent, scmConnector));
            gitXWebhookFinal = gitXWebhook;
            if (isEmpty(gitXWebhook.getFolderPaths())) {
              return ProcessingFilePathResponseDTO.builder()
                  .modifiedFilePaths(new ArrayList<>(modifiedFilePaths))
                  .processingFilePaths(new ArrayList<>(modifiedFilePaths))
                  .scmConnector(scmConnector)
                  .gitXWebhook(gitXWebhookFinal)
                  .build();
            }
          }
          log.info(
              String.format("Successfully fetched the file paths with GitXWebhook %s", gitXWebhook.getIdentifier()));
          processingFilePaths.addAll(getMatchingFilePaths(new ArrayList<>(modifiedFilePaths), gitXWebhook));
        } catch (Exception exception) {
          log.error(
              String.format(
                  "Failed to fetch the modifiedFilePaths data from gitXWebhook identifier %s, will be trying with the next connector",
                  gitXWebhook.getIdentifier()),
              exception);
        }
      }
    }
    return ProcessingFilePathResponseDTO.builder()
        .modifiedFilePaths(new ArrayList<>(modifiedFilePaths))
        .processingFilePaths(new ArrayList<>(processingFilePaths))
        .scmConnector(scmConnector)
        .gitXWebhook(gitXWebhookFinal)
        .build();
  }

  private List<String> parsePayloadAndGetModifiedFilePaths(
      GitXWebhook gitXWebhook, GitXWebhookEvent gitXWebhookEvent, ScmConnector scmConnector) {
    if (gitXWebhook.getIsEnabled()) {
      log.info(String.format(
          "The webhook with identifier [%s] is enabled. Checking for the folder paths.", gitXWebhook.getIdentifier()));
      List<String> modifiedFilePaths = getDiffFilesUsingSCM(gitXWebhook.getAccountIdentifier(), scmConnector,
          gitXWebhookEvent.getBeforeCommitId(), gitXWebhookEvent.getAfterCommitId());

      log.info(String.format("Successfully fetched %d of modified file paths", modifiedFilePaths.size()));
      return modifiedFilePaths;
    }
    return new ArrayList<>();
  }

  private List<String> getMatchingFilePaths(List<String> modifiedFilePaths, GitXWebhook gitXWebhook) {
    if (isEmpty(gitXWebhook.getFolderPaths())) {
      log.info(String.format("No folderPaths mentioned in the webhook %s, parsing all the modifiedFilePaths",
          gitXWebhook.getIdentifier()));
      return modifiedFilePaths;
    }
    return GitXWebhookUtils.compareFolderPaths(gitXWebhook.getFolderPaths(), modifiedFilePaths);
  }

  private List<GitXWebhook> getGitXWebhook(GitXWebhookEvent gitXWebhookEvent) {
    return gitXWebhookService.getGitXWebhook(gitXWebhookEvent.getAccountIdentifier(), gitXWebhookEvent.getRepo());
  }

  public List<String> getDiffFilesUsingSCM(
      String accountIdentifier, ScmConnector scmConnector, String initialCommitId, String finalCommitId) {
    GitDiffResultFileListDTO gitDiffResultFileListDTO =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.listCommitsDiffFiles(
                Scope.of(accountIdentifier, "", ""), scmConnector, initialCommitId, finalCommitId),
            scmConnector);

    StringBuilder gitDiffResultFileList =
        new StringBuilder(String.format("Compare Commits Response from %s to %s :: ", initialCommitId, finalCommitId));
    gitDiffResultFileListDTO.getPrFileList().forEach(
        prFile -> gitDiffResultFileList.append(prFile.toString()).append(" :::: "));
    log.info(gitDiffResultFileList.toString());

    return emptyIfNull(gitDiffResultFileListDTO.getPrFileList())
        .stream()
        .map(GitDiffResultFileDTO::getPath)
        .distinct()
        .collect(Collectors.toList());
  }

  private ScmConnector getScmConnector(GitXWebhook gitXWebhook) {
    ScmConnector scmConnector = gitSyncConnectorService.getScmConnector(gitXWebhook.getAccountIdentifier(),
        gitXWebhook.getOrgIdentifier(), gitXWebhook.getProjectIdentifier(), gitXWebhook.getConnectorRef());
    scmConnector.setUrl(gitRepoHelper.getRepoUrl(scmConnector, gitXWebhook.getRepoName()));
    return gitSyncConnectorService.getDecryptedConnectorForNewGitX(gitXWebhook.getAccountIdentifier(),
        gitXWebhook.getOrgIdentifier(), gitXWebhook.getProjectIdentifier(), scmConnector);
  }

  private void updateEventStatus(String accountIdentifier, String eventIdentifier,
      GitXWebhookEventStatus gitXWebhookEventStatus, WebhookDTO webhookDTO) {
    gitXWebhookEventService.updateEvent(accountIdentifier, eventIdentifier,
        GitXEventUpdateRequestDTO.builder()
            .gitXWebhookEventStatus(gitXWebhookEventStatus)
            .webhookDTO(webhookDTO)
            .build());
  }

  private void updateEventStatus(String accountIdentifier, String eventIdentifier,
      GitXWebhookEventStatus gitXWebhookEventStatus, List<String> processingFilePaths, WebhookDTO webhookDTO) {
    gitXWebhookEventService.updateEvent(accountIdentifier, eventIdentifier,
        GitXEventUpdateRequestDTO.builder()
            .gitXWebhookEventStatus(gitXWebhookEventStatus)
            .processedFilePaths(processingFilePaths)
            .webhookDTO(webhookDTO)
            .build());
  }

  private void markEventFailed(GitXWebhookEvent gitXWebhookEvent, WebhookDTO webhookDTO) {
    try {
      updateEventStatus(gitXWebhookEvent.getAccountIdentifier(), gitXWebhookEvent.getEventIdentifier(),
          GitXWebhookEventStatus.FAILED, webhookDTO);
    } catch (Exception ex) {
      log.error("Exception occurred while changing the state of the event {} to Failed",
          gitXWebhookEvent.getEventIdentifier(), ex);
    }
  }
}
