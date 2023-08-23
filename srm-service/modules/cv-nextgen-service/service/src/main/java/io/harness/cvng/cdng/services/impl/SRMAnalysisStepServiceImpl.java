/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import static io.harness.cvng.notification.utils.NotificationRuleConstants.ANALYSIS_DURATION;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.ANALYSIS_ENDED_AT;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.ANALYSIS_PIPELINE_NAME;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.ANALYSIS_STARTED_AT;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.ENTITY_IDENTIFIER;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.ENTITY_NAME;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.MS_HEALTH_REPORT;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.PIPELINE_ID;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.PIPELINE_URL_FORMAT;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.PLAN_EXECUTION_ID;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.SERVICE_IDENTIFIER;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.STAGE_STEP_ID;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.artifact.outcome.ArtifactsOutcome;
import io.harness.cvng.analysis.entities.SRMAnalysisStepDetailDTO;
import io.harness.cvng.analysis.entities.SRMAnalysisStepExecutionDetail;
import io.harness.cvng.analysis.entities.SRMAnalysisStepExecutionDetail.SRMAnalysisStepExecutionDetailsKeys;
import io.harness.cvng.analysis.entities.SRMAnalysisStepInstanceDetails;
import io.harness.cvng.beans.change.SRMAnalysisStatus;
import io.harness.cvng.cdng.services.api.SRMAnalysisStepService;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.change.MSHealthReport;
import io.harness.cvng.core.beans.params.MonitoredServiceParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ResourceParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.monitoredService.MSHealthReportService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.notification.beans.NotificationRuleConditionType;
import io.harness.cvng.notification.beans.NotificationRuleType;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceNotificationRuleCondition;
import io.harness.cvng.servicelevelobjective.beans.secondaryevents.SecondaryEventDetailsResponse;
import io.harness.cvng.servicelevelobjective.beans.secondaryevents.SecondaryEventsResponse;
import io.harness.cvng.servicelevelobjective.beans.secondaryevents.SecondaryEventsType;
import io.harness.cvng.servicelevelobjective.services.api.SecondaryEventDetailsService;
import io.harness.cvng.utils.ScopedInformation;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.remote.client.NGRestUtils;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRMAnalysisStepServiceImpl implements SRMAnalysisStepService, SecondaryEventDetailsService {
  @Inject HPersistence hPersistence;

  @Inject PipelineServiceClient pipelineServiceClient;

  @Inject NextGenService nextGenService;

  @Inject Clock clock;

  @Inject MonitoredServiceService monitoredServiceService;

  @Inject MSHealthReportService msHealthReportService;

  @Override
  public String createSRMAnalysisStepExecution(Ambiance ambiance, String monitoredServiceIdentifier, String stepName,
      ServiceEnvironmentParams serviceEnvironmentParams, Duration duration,
      Optional<ArtifactsOutcome> optionalArtifactsOutcome) {
    String pipelineName = ambiance.getMetadata().getPipelineIdentifier();
    try {
      Object pmsExecutionSummary = NGRestUtils.getResponse(pipelineServiceClient.getExecutionDetailV2(
          ambiance.getPlanExecutionId(), serviceEnvironmentParams.getAccountIdentifier(),
          serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier()));
      JsonNode rootNode = JsonUtils.asTree(pmsExecutionSummary);

      // Fetch the value of the "name" field
      pipelineName = rootNode.get("pipelineExecutionSummary").get("name").asText();
    } catch (Exception exception) {
      log.error("Failed to fetch the pipeline name", exception);
    }
    Instant instant = clock.instant();
    SRMAnalysisStepExecutionDetail executionDetails =
        SRMAnalysisStepExecutionDetail.builder()
            .stageId(AmbianceUtils.getStageLevelFromAmbiance(ambiance).get().getIdentifier())
            .stageStepId(AmbianceUtils.getStageLevelFromAmbiance(ambiance).get().getSetupId())
            .pipelineId(ambiance.getMetadata().getPipelineIdentifier())
            .planExecutionId(ambiance.getPlanExecutionId())
            .stepName(stepName)
            .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
            .envIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
            .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
            .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
            .accountId(serviceEnvironmentParams.getAccountIdentifier())
            .monitoredServiceIdentifier(monitoredServiceIdentifier)
            .analysisStartTime(instant.toEpochMilli())
            .analysisStatus(SRMAnalysisStatus.RUNNING)
            .analysisEndTime(instant.plus(duration).toEpochMilli())
            .analysisDuration(duration)
            .pipelineName(pipelineName)
            .build();
    if (optionalArtifactsOutcome.isPresent()) {
      executionDetails.setArtifactType(optionalArtifactsOutcome.get().getPrimary().getArtifactType());
      executionDetails.setArtifactTag(optionalArtifactsOutcome.get().getPrimary().getTag());
    }
    return hPersistence.save(executionDetails);
  }

  @Nullable
  @Override
  public SRMAnalysisStepExecutionDetail getSRMAnalysisStepExecutionDetail(String analysisStepExecutionDetailId) {
    return hPersistence.get(SRMAnalysisStepExecutionDetail.class, analysisStepExecutionDetailId);
  }

  @Override
  public void abortRunningStepsForMonitoredService(ProjectParams projectParams, String monitoredServiceIdentifier) {
    Query<SRMAnalysisStepExecutionDetail> updateQuery =
        getSRMAnalysisStepExecutionDetailQuery(projectParams, monitoredServiceIdentifier)
            .filter(SRMAnalysisStepExecutionDetailsKeys.analysisStatus, SRMAnalysisStatus.RUNNING);
    UpdateOperations<SRMAnalysisStepExecutionDetail> updateOperations =
        hPersistence.createUpdateOperations(SRMAnalysisStepExecutionDetail.class)
            .set(SRMAnalysisStepExecutionDetailsKeys.analysisStatus, SRMAnalysisStatus.ABORTED)
            .set(SRMAnalysisStepExecutionDetailsKeys.analysisEndTime, clock.millis());
    hPersistence.update(updateQuery, updateOperations);
  }

  @Override
  public SRMAnalysisStepDetailDTO abortRunningSrmAnalysisStep(String executionDetailId) {
    SRMAnalysisStepExecutionDetail stepExecutionDetail = verifyAndGetSRMAnalysisStepExecutionDetail(executionDetailId);

    UpdateOperations<SRMAnalysisStepExecutionDetail> updateOperations =
        hPersistence.createUpdateOperations(SRMAnalysisStepExecutionDetail.class)
            .set(SRMAnalysisStepExecutionDetailsKeys.analysisStatus, SRMAnalysisStatus.ABORTED)
            .set(SRMAnalysisStepExecutionDetailsKeys.analysisEndTime, clock.millis());
    hPersistence.update(stepExecutionDetail, updateOperations);
    stepExecutionDetail = getSRMAnalysisStepExecutionDetail(executionDetailId);
    try {
      handleReportNotification(stepExecutionDetail);
    } catch (Exception exception) {
      log.error("Sending deployment analysis report failed", exception);
    }
    return SRMAnalysisStepDetailDTO.getDTOFromEntity(stepExecutionDetail);
  }

  @Override
  public void completeSrmAnalysisStep(SRMAnalysisStepExecutionDetail stepExecutionDetail) {
    UpdateOperations<SRMAnalysisStepExecutionDetail> updateOperations =
        hPersistence.createUpdateOperations(SRMAnalysisStepExecutionDetail.class)
            .set(SRMAnalysisStepExecutionDetailsKeys.analysisStatus, SRMAnalysisStatus.COMPLETED);
    hPersistence.update(stepExecutionDetail, updateOperations);
    try {
      handleReportNotification(stepExecutionDetail);
    } catch (Exception exception) {
      log.error("Sending deployment analysis report failed", exception);
    }
  }

  private SRMAnalysisStepExecutionDetail verifyAndGetSRMAnalysisStepExecutionDetail(String executionDetailId) {
    SRMAnalysisStepExecutionDetail stepExecutionDetail = getSRMAnalysisStepExecutionDetail(executionDetailId);

    Preconditions.checkArgument(
        !stepExecutionDetail.equals(null), String.format("Step Execution Id %s is not present.", executionDetailId));
    Preconditions.checkArgument(stepExecutionDetail.getAnalysisStatus().equals(SRMAnalysisStatus.RUNNING),
        String.format("Step Execution Id %s is not RUNNING, the current status is %s", executionDetailId,
            stepExecutionDetail.getAnalysisStatus()));
    return stepExecutionDetail;
  }

  @Override
  public SRMAnalysisStepDetailDTO getSRMAnalysisSummary(String executionDetailId) {
    SRMAnalysisStepExecutionDetail stepExecutionDetail = getSRMAnalysisStepExecutionDetail(executionDetailId);
    Preconditions.checkArgument(!stepExecutionDetail.equals(null),
        String.format("Step Execution Details %s is not present.", executionDetailId));
    ServiceResponseDTO serviceResponseDTO =
        nextGenService.getService(stepExecutionDetail.getAccountId(), stepExecutionDetail.getOrgIdentifier(),
            stepExecutionDetail.getProjectIdentifier(), stepExecutionDetail.getServiceIdentifier());
    EnvironmentResponseDTO environmentResponseDTO =
        nextGenService.getEnvironment(stepExecutionDetail.getAccountId(), stepExecutionDetail.getOrgIdentifier(),
            stepExecutionDetail.getProjectIdentifier(), stepExecutionDetail.getEnvIdentifier());
    SRMAnalysisStepDetailDTO srmAnalysisStepDetailDTO = SRMAnalysisStepDetailDTO.getDTOFromEntity(stepExecutionDetail);
    srmAnalysisStepDetailDTO.setServiceName(serviceResponseDTO != null ? serviceResponseDTO.getName() : null);
    srmAnalysisStepDetailDTO.setEnvironmentName(
        environmentResponseDTO != null ? environmentResponseDTO.getName() : null);
    return srmAnalysisStepDetailDTO;
  }

  @Override
  public List<SecondaryEventsResponse> getSRMAnalysisStepExecutions(
      ProjectParams projectParams, String monitoredServiceIdentifier, long startTime, long endTime) {
    List<SRMAnalysisStepExecutionDetail> srmAnalysisStepExecutionDetails =
        getSRMAnalysisStepExecutionDetailQuery(projectParams, monitoredServiceIdentifier)
            .field(SRMAnalysisStepExecutionDetailsKeys.analysisStartTime)
            .lessThanOrEq(endTime)
            .field(SRMAnalysisStepExecutionDetailsKeys.analysisEndTime)
            .greaterThanOrEq(startTime)
            .asList();
    return srmAnalysisStepExecutionDetails.stream()
        .map(executionDetail
            -> SecondaryEventsResponse.builder()
                   .type(SecondaryEventsType.SRM_ANALYSIS_IMPACT)
                   .startTime(TimeUnit.MILLISECONDS.toSeconds(executionDetail.getAnalysisStartTime()))
                   .endTime(TimeUnit.MILLISECONDS.toSeconds(executionDetail.getAnalysisEndTime()))
                   .identifiers(List.of(executionDetail.getUuid()))
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public SecondaryEventDetailsResponse getInstanceByUuids(List<String> uuids, SecondaryEventsType eventType) {
    SRMAnalysisStepExecutionDetail analysisStepExecutionDetail = getSRMAnalysisStepExecutionDetail(uuids.get(0));
    return SecondaryEventDetailsResponse.builder()
        .type(eventType)
        .startTime(TimeUnit.MILLISECONDS.toSeconds(analysisStepExecutionDetail.getAnalysisStartTime()))
        .endTime(TimeUnit.MILLISECONDS.toSeconds(analysisStepExecutionDetail.getAnalysisEndTime()))
        .details(SRMAnalysisStepInstanceDetails.builder()
                     .analysisDuration(analysisStepExecutionDetail.getAnalysisDuration())
                     .analysisStatus(analysisStepExecutionDetail.getAnalysisStatus())
                     .build())
        .build();
  }

  @Override
  public void handleReportNotification(SRMAnalysisStepExecutionDetail stepExecutionDetail) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(stepExecutionDetail.getAccountId())
                                      .orgIdentifier(stepExecutionDetail.getOrgIdentifier())
                                      .projectIdentifier(stepExecutionDetail.getProjectIdentifier())
                                      .build();
    List<MonitoredServiceNotificationRule> notificationRules =
        monitoredServiceService.getNotificationRules(projectParams, stepExecutionDetail.getMonitoredServiceIdentifier(),
            Collections.singletonList(NotificationRuleConditionType.DEPLOYMENT_IMPACT_REPORT));
    for (MonitoredServiceNotificationRule notificationRule : notificationRules) {
      List<MonitoredServiceNotificationRuleCondition> notificationRuleConditions = notificationRule.getConditions();
      for (MonitoredServiceNotificationRuleCondition condition : notificationRuleConditions) {
        MSHealthReport msHealthReport =
            msHealthReportService.getMSHealthReport(projectParams, stepExecutionDetail.getMonitoredServiceIdentifier(),
                Instant.ofEpochMilli(stepExecutionDetail.getAnalysisStartTime()));
        MonitoredService monitoredService = monitoredServiceService.getMonitoredService(
            MonitoredServiceParams.builderWithProjectParams(projectParams)
                .monitoredServiceIdentifier(stepExecutionDetail.getMonitoredServiceIdentifier())
                .build());
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM 'at' h:mm a z", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone(clock.getZone().getId())); // Set the desired time zone
        Date startDateTime = new Date(stepExecutionDetail.getAnalysisStartTime());
        String formattedStartDate = dateFormat.format(startDateTime);
        Date endDateTime = new Date(stepExecutionDetail.getAnalysisEndTime());
        String formattedEndDate = dateFormat.format(endDateTime);
        Map<String, Object> entityDetails = new HashMap<>();
        entityDetails.put(ENTITY_IDENTIFIER, stepExecutionDetail.getMonitoredServiceIdentifier());
        entityDetails.put(ENTITY_NAME, monitoredService.getName());
        entityDetails.put(SERVICE_IDENTIFIER, monitoredService.getServiceIdentifier());
        entityDetails.put(MS_HEALTH_REPORT, msHealthReport);
        entityDetails.put(PIPELINE_ID, stepExecutionDetail.getPipelineId());
        entityDetails.put(PLAN_EXECUTION_ID, stepExecutionDetail.getPlanExecutionId());
        entityDetails.put(STAGE_STEP_ID, stepExecutionDetail.getStageStepId());
        entityDetails.put(ANALYSIS_STARTED_AT, formattedStartDate);
        entityDetails.put(ANALYSIS_ENDED_AT, formattedEndDate);
        entityDetails.put(
            ANALYSIS_DURATION, String.valueOf(stepExecutionDetail.getAnalysisDuration().toDays()) + " days");
        entityDetails.put(ANALYSIS_PIPELINE_NAME,
            stepExecutionDetail.getPipelineName().equals(null) ? "" : stepExecutionDetail.getPipelineName());
        msHealthReportService.sendReportNotification(projectParams, entityDetails,
            NotificationRuleType.MONITORED_SERVICE,
            MonitoredServiceNotificationRule.MonitoredServiceDeploymentImpactReportCondition.builder().build(),
            notificationRule.getNotificationMethod(), stepExecutionDetail.getMonitoredServiceIdentifier());
      }
    }
  }

  private Query<SRMAnalysisStepExecutionDetail> getSRMAnalysisStepExecutionDetailQuery(
      ProjectParams projectParams, String monitoredServiceIdentifier) {
    return hPersistence.createQuery(SRMAnalysisStepExecutionDetail.class)
        .filter(SRMAnalysisStepExecutionDetailsKeys.accountId, projectParams.getAccountIdentifier())
        .filter(SRMAnalysisStepExecutionDetailsKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(SRMAnalysisStepExecutionDetailsKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(SRMAnalysisStepExecutionDetailsKeys.monitoredServiceIdentifier, monitoredServiceIdentifier);
  }

  private String getPipelineUrl(
      String baseUrl, ProjectParams projectParams, SRMAnalysisStepExecutionDetail stepExecutionDetail) {
    return String.format(PIPELINE_URL_FORMAT, baseUrl, projectParams.getAccountIdentifier(),
        projectParams.getOrgIdentifier(), projectParams.getProjectIdentifier(), stepExecutionDetail.getPipelineId(),
        stepExecutionDetail.getPlanExecutionId(), stepExecutionDetail.getStageStepId());
  }

  @Override
  public PageResponse<SRMAnalysisStepDetailDTO> getReportList(ProjectParams projectParams,
      List<String> serviceIdentifiers, List<String> environmentIdentifiers, List<String> monitoredServiceIdentifiers,
      boolean isMonitoredServiceIdentifierScoped, Instant startTime, Instant endTime, PageRequest pageRequest) {
    if (isNotEmpty(monitoredServiceIdentifiers)) {
      Preconditions.checkState(isEmpty(serviceIdentifiers) && isEmpty(environmentIdentifiers),
          "serviceIdentifier, envIdentifier filter can not be used with monitoredServiceIdentifier filter");
      return getReportList(projectParams, monitoredServiceIdentifiers, startTime, endTime, pageRequest,
          isMonitoredServiceIdentifierScoped);
    } else {
      return getReportList(projectParams, serviceIdentifiers, environmentIdentifiers, startTime, endTime, pageRequest);
    }
  }

  public PageResponse<SRMAnalysisStepDetailDTO> getReportList(ProjectParams projectParams,
      List<String> serviceIdentifiers, List<String> environmentIdentifier, Instant startTime, Instant endTime,
      PageRequest pageRequest) {
    List<String> monitoredServiceIdentifiers = monitoredServiceService.getMonitoredServiceIdentifiers(
        projectParams, serviceIdentifiers, environmentIdentifier);
    return getReportList(projectParams, monitoredServiceIdentifiers, startTime, endTime, pageRequest, false);
  }

  private PageResponse<SRMAnalysisStepDetailDTO> getReportList(ProjectParams projectParams,
      List<String> monitoredServiceIdentifiers, Instant startTime, Instant endTime, PageRequest pageRequest,
      boolean isMonitoredServiceIdentifierScoped) {
    List<SRMAnalysisStepExecutionDetail> list =
        getList(startTime, endTime, projectParams, monitoredServiceIdentifiers, isMonitoredServiceIdentifierScoped)
            .asList();

    List<SRMAnalysisStepDetailDTO> srmAnalysisStepDetailDTOList =
        list.stream()
            .map(stepExecutionDetail -> {
              ServiceResponseDTO serviceResponseDTO = null;
              EnvironmentResponseDTO environmentResponseDTO = null;
              if (stepExecutionDetail.getOrgIdentifier() != null
                  && stepExecutionDetail.getProjectIdentifier() != null) {
                if (stepExecutionDetail.getServiceIdentifier() != null) {
                  serviceResponseDTO = nextGenService.getService(stepExecutionDetail.getAccountId(),
                      stepExecutionDetail.getOrgIdentifier(), stepExecutionDetail.getProjectIdentifier(),
                      stepExecutionDetail.getServiceIdentifier());
                }
                if (stepExecutionDetail.getEnvIdentifier() != null) {
                  environmentResponseDTO = nextGenService.getEnvironment(stepExecutionDetail.getAccountId(),
                      stepExecutionDetail.getOrgIdentifier(), stepExecutionDetail.getProjectIdentifier(),
                      stepExecutionDetail.getEnvIdentifier());
                }
              }
              SRMAnalysisStepDetailDTO srmAnalysisStepDetailDTO =
                  SRMAnalysisStepDetailDTO.getDTOFromEntity(stepExecutionDetail);
              srmAnalysisStepDetailDTO.setServiceName(serviceResponseDTO != null ? serviceResponseDTO.getName() : null);
              srmAnalysisStepDetailDTO.setEnvironmentName(
                  environmentResponseDTO != null ? environmentResponseDTO.getName() : null);
              srmAnalysisStepDetailDTO.setServiceName(serviceResponseDTO != null ? serviceResponseDTO.getName() : null);
              return srmAnalysisStepDetailDTO;
            })
            .collect(Collectors.toList());

    return PageUtils.offsetAndLimit(
        srmAnalysisStepDetailDTOList, pageRequest.getPageIndex(), pageRequest.getPageSize());
  }

  private Query<SRMAnalysisStepExecutionDetail> getList(Instant startTime, Instant endTime, ProjectParams projectParams,
      List<String> monitoredServiceIdentifiers, boolean isMonitoredServiceIdentifierScoped) {
    Query<SRMAnalysisStepExecutionDetail> query = hPersistence.createQuery(SRMAnalysisStepExecutionDetail.class);
    if (isMonitoredServiceIdentifierScoped) {
      List<ResourceParams> monitoredServiceIdentifiersWithParams =
          ScopedInformation.getResourceParamsFromScopedIdentifiers(monitoredServiceIdentifiers);

      for (int i = 0; i < monitoredServiceIdentifiersWithParams.size(); i++) {
        query.or(query.and(query.criteria(SRMAnalysisStepExecutionDetailsKeys.accountId)
                               .equal(monitoredServiceIdentifiersWithParams.get(i).getAccountIdentifier()),
            query.criteria(SRMAnalysisStepExecutionDetailsKeys.orgIdentifier)
                .equal(monitoredServiceIdentifiersWithParams.get(i).getOrgIdentifier()),
            query.criteria(SRMAnalysisStepExecutionDetailsKeys.projectIdentifier)
                .equal(monitoredServiceIdentifiersWithParams.get(i).getProjectIdentifier()),
            query.criteria(SRMAnalysisStepExecutionDetailsKeys.monitoredServiceIdentifier)
                .equal(monitoredServiceIdentifiersWithParams.get(i).getIdentifier())));
        query.criteria(SRMAnalysisStepExecutionDetailsKeys.analysisEndTime).greaterThanOrEq(startTime.toEpochMilli());
        query.criteria(SRMAnalysisStepExecutionDetailsKeys.analysisStartTime).lessThanOrEq(endTime.toEpochMilli());
      }

    } else {
      query = query.filter(SRMAnalysisStepExecutionDetailsKeys.accountId, projectParams.getAccountIdentifier())
                  .filter(SRMAnalysisStepExecutionDetailsKeys.orgIdentifier, projectParams.getOrgIdentifier())
                  .filter(SRMAnalysisStepExecutionDetailsKeys.projectIdentifier, projectParams.getProjectIdentifier())
                  .field(SRMAnalysisStepExecutionDetailsKeys.monitoredServiceIdentifier)
                  .in(monitoredServiceIdentifiers)
                  .field(SRMAnalysisStepExecutionDetailsKeys.analysisEndTime)
                  .greaterThanOrEq(startTime.toEpochMilli())
                  .field(SRMAnalysisStepExecutionDetailsKeys.analysisStartTime)
                  .lessThanOrEq(endTime.toEpochMilli());
    }
    return query;
  }
}
