/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.servicelevelobjective.SLORiskCountResponse;
import io.harness.cvng.servicelevelobjective.beans.MSDropdownResponse;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardDetail;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.beans.SLOErrorBudgetResetDTO;
import io.harness.cvng.servicelevelobjective.beans.SLOHealthListView;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveV2DTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveV2Response;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyDTO;
import io.harness.cvng.servicelevelobjective.beans.slospec.SimpleServiceLevelObjectiveSpec;
import io.harness.cvng.servicelevelobjective.entities.AbstractServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective.ServiceLevelObjectivesDetail;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.SimpleServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.TimePeriod;
import io.harness.cvng.servicelevelobjective.entities.UserJourney;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.cvng.servicelevelobjective.services.api.SLODashboardService;
import io.harness.cvng.servicelevelobjective.services.api.SLOErrorBudgetResetService;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveV2Service;
import io.harness.cvng.servicelevelobjective.services.api.UserJourneyService;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SLODashboardServiceImpl implements SLODashboardService {
  @Inject private ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Inject private ServiceLevelObjectiveV2Service serviceLevelObjectiveV2Service;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private SLIRecordService sliRecordService;
  @Inject private ServiceLevelIndicatorService serviceLevelIndicatorService;
  @Inject private SLOHealthIndicatorService sloHealthIndicatorService;
  @Inject private Clock clock;
  @Inject private NextGenService nextGenService;
  @Inject private SLOErrorBudgetResetService sloErrorBudgetResetService;
  @Inject private UserJourneyService userJourneyService;

  @Override
  public PageResponse<SLODashboardWidget> getSloDashboardWidgets(
      ProjectParams projectParams, SLODashboardApiFilter filter, PageParams pageParams) {
    PageResponse<ServiceLevelObjectiveResponse> sloPageResponse =
        serviceLevelObjectiveService.getSLOForDashboard(projectParams, filter, pageParams);

    // sending second param as null deliberately so that this deprecated function does not break
    List<SLODashboardWidget> sloDashboardWidgets =
        sloPageResponse.getContent()
            .stream()
            .map(sloResponse -> getSloDashboardWidget(projectParams, null, null))
            .collect(Collectors.toList());
    return PageResponse.<SLODashboardWidget>builder()
        .pageSize(sloPageResponse.getPageSize())
        .pageIndex(sloPageResponse.getPageIndex())
        .totalPages(sloPageResponse.getTotalPages())
        .totalItems(sloPageResponse.getTotalItems())
        .pageItemCount(sloPageResponse.getPageItemCount())
        .content(sloDashboardWidgets)
        .build();
  }

  @Override
  public PageResponse<SLOHealthListView> getSloHealthListView(
      ProjectParams projectParams, SLODashboardApiFilter filter, PageParams pageParams) {
    PageResponse<AbstractServiceLevelObjective> sloPageResponse =
        serviceLevelObjectiveV2Service.getSLOForListView(projectParams, filter, pageParams);

    Set<String> monitoredServiceIdentifiers =
        sloPageResponse.getContent()
            .stream()
            .flatMap(slo -> {
              if (slo.getType().equals(ServiceLevelObjectiveType.SIMPLE)) {
                return Stream.of(((SimpleServiceLevelObjective) slo).getMonitoredServiceIdentifier());
              }
              return Stream.empty();
            })
            .collect(Collectors.toSet());

    List<ServiceLevelObjectivesDetail> serviceLevelObjectivesDetailList = Collections.emptyList();
    if (isNotEmpty(filter.getCompositeSLOIdentifier())) {
      serviceLevelObjectivesDetailList = ((CompositeServiceLevelObjective) serviceLevelObjectiveV2Service.getEntity(projectParams, filter.getCompositeSLOIdentifier())).getServiceLevelObjectivesDetails();
    }

    List<MonitoredServiceResponse> monitoredServices =
        monitoredServiceService.getForAssociatedSLOs(projectParams, serviceLevelObjectivesDetailList, monitoredServiceIdentifiers);

    List<String> sloIdentifiers = sloPageResponse.getContent()
                                      .stream()
                                      .map(AbstractServiceLevelObjective::getIdentifier)
                                      .collect(Collectors.toList());
    List<SLOHealthIndicator> sloHealthIndicators =
        sloHealthIndicatorService.getBySLOIdentifiersForAssociatedSLOs(projectParams, serviceLevelObjectivesDetailList, sloIdentifiers);

    List<UserJourney> userJourneyList = userJourneyService.get(projectParams);

    Map<String, MonitoredServiceDTO> monitoredServiceIdentifierToDTOMap =
        monitoredServices.stream()
            .map(MonitoredServiceResponse::getMonitoredServiceDTO)
            .collect(Collectors.toMap(MonitoredServiceDTO::getIdentifier, monitoredServiceDTO -> monitoredServiceDTO));
    Map<String, SLOHealthIndicator> sloIdentifierToHealthIndicatorMap =
        sloHealthIndicators.stream().collect(Collectors.toMap(
            SLOHealthIndicator::getServiceLevelObjectiveIdentifier, sloHealthIndicator -> sloHealthIndicator));
    Map<String, String> userJourneyIdentifierToNameMap =
        userJourneyList.stream().collect(Collectors.toMap(UserJourney::getIdentifier, UserJourney::getName));

    List<SLOHealthListView> sloWidgets =
        sloPageResponse.getContent()
            .stream()
            .map(sloResponse
                -> getSLOListView(projectParams, sloResponse, monitoredServiceIdentifierToDTOMap,
                    sloIdentifierToHealthIndicatorMap, userJourneyIdentifierToNameMap))
            .collect(Collectors.toList());

    return PageResponse.<SLOHealthListView>builder()
        .pageSize(sloPageResponse.getPageSize())
        .pageIndex(sloPageResponse.getPageIndex())
        .totalPages(sloPageResponse.getTotalPages())
        .totalItems(sloPageResponse.getTotalItems())
        .pageItemCount(sloPageResponse.getPageItemCount())
        .content(sloWidgets)
        .build();
  }

  @Override
  public SLODashboardDetail getSloDashboardDetail(
      ProjectParams projectParams, String identifier, Long startTime, Long endTime) {
    ServiceLevelObjectiveV2Response sloResponse = serviceLevelObjectiveV2Service.get(projectParams, identifier);
    SLODashboardWidget sloDashboardWidget;
    if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
      sloDashboardWidget = getSloDashboardWidget(projectParams, sloResponse, null);
    } else {
      sloDashboardWidget = getSloDashboardWidget(projectParams, sloResponse,
          TimeRangeParams.builder()
              .startTime(Instant.ofEpochMilli(startTime))
              .endTime(Instant.ofEpochMilli(endTime))
              .build());
    }
    return SLODashboardDetail.builder()
        .description(sloResponse.getServiceLevelObjectiveV2DTO().getDescription())
        .createdAt(sloResponse.getCreatedAt())
        .lastModifiedAt(sloResponse.getLastModifiedAt())
        .timeRangeFilters(serviceLevelObjectiveV2Service.getEntity(projectParams, identifier).getTimeRangeFilters())
        .sloDashboardWidget(sloDashboardWidget)
        .build();
  }

  @Override
  public SLORiskCountResponse getRiskCount(
      ProjectParams projectParams, SLODashboardApiFilter serviceLevelObjectiveFilter) {
    return serviceLevelObjectiveService.getRiskCount(projectParams, serviceLevelObjectiveFilter);
  }

  private SLODashboardWidget getSloDashboardWidget(
      ProjectParams projectParams, ServiceLevelObjectiveV2Response sloResponse, TimeRangeParams filter) {
    SimpleServiceLevelObjectiveSpec simpleServiceLevelObjectiveSpec =
        (SimpleServiceLevelObjectiveSpec) sloResponse.getServiceLevelObjectiveV2DTO().getSpec();
    Preconditions.checkState(simpleServiceLevelObjectiveSpec.getServiceLevelIndicators().size() == 1,
        "Only one service level indicator is supported");
    ServiceLevelIndicatorDTO serviceLevelIndicatorDTO =
        simpleServiceLevelObjectiveSpec.getServiceLevelIndicators().get(0);
    ServiceLevelIndicator serviceLevelIndicator =
        serviceLevelIndicatorService.getServiceLevelIndicator(projectParams, serviceLevelIndicatorDTO.getIdentifier());

    ServiceLevelObjectiveV2DTO slo = sloResponse.getServiceLevelObjectiveV2DTO();
    AbstractServiceLevelObjective serviceLevelObjective =
        serviceLevelObjectiveV2Service.getEntity(projectParams, slo.getIdentifier());
    MonitoredServiceDTO monitoredService =
        monitoredServiceService.get(projectParams, simpleServiceLevelObjectiveSpec.getMonitoredServiceRef())
            .getMonitoredServiceDTO();
    LocalDateTime currentLocalDate = LocalDateTime.ofInstant(clock.instant(), serviceLevelObjective.getZoneOffset());
    TimePeriod timePeriod = serviceLevelObjective.getCurrentTimeRange(currentLocalDate);
    Instant currentTimeMinute = DateTimeUtils.roundDownTo1MinBoundary(clock.instant());
    List<SLOErrorBudgetResetDTO> errorBudgetResetDTOS =
        sloErrorBudgetResetService.getErrorBudgetResets(projectParams, slo.getIdentifier());
    int totalErrorBudgetMinutes =
        serviceLevelObjective.getActiveErrorBudgetMinutes(errorBudgetResetDTOS, currentLocalDate);

    SLODashboardWidget.SLOGraphData sloGraphData = sliRecordService.getGraphData(serviceLevelIndicator,
        timePeriod.getStartTime(serviceLevelObjective.getZoneOffset()), currentTimeMinute, totalErrorBudgetMinutes,
        serviceLevelIndicator.getSliMissingDataType(), serviceLevelIndicator.getVersion(), filter);
    return SLODashboardWidget.withGraphData(sloGraphData)
        .sloIdentifier(slo.getIdentifier())
        .title(slo.getName())
        .sloTargetType(slo.getSloTarget().getType())
        .currentPeriodLengthDays(timePeriod.getTotalDays())
        .currentPeriodStartTime(timePeriod.getStartTime(serviceLevelObjective.getZoneOffset()).toEpochMilli())
        .currentPeriodEndTime(timePeriod.getEndTime(serviceLevelObjective.getZoneOffset()).toEpochMilli())
        .sloTargetPercentage(serviceLevelObjective.getSloTargetPercentage())
        .monitoredServiceIdentifier(simpleServiceLevelObjectiveSpec.getMonitoredServiceRef())
        .monitoredServiceName(monitoredService.getName())
        .environmentIdentifier(monitoredService.getEnvironmentRef())
        .environmentName(
            nextGenService
                .getEnvironment(serviceLevelObjective.getAccountId(), serviceLevelObjective.getOrgIdentifier(),
                    serviceLevelObjective.getProjectIdentifier(), monitoredService.getEnvironmentRef())
                .getName())
        .serviceName(nextGenService
                         .getService(serviceLevelObjective.getAccountId(), serviceLevelObjective.getOrgIdentifier(),
                             serviceLevelObjective.getProjectIdentifier(), monitoredService.getServiceRef())
                         .getName())
        .serviceIdentifier(monitoredService.getServiceRef())
        .healthSourceIdentifier(simpleServiceLevelObjectiveSpec.getHealthSourceRef())
        .healthSourceName(getHealthSourceName(monitoredService, simpleServiceLevelObjectiveSpec.getHealthSourceRef()))
        .tags(slo.getTags())
        .type(simpleServiceLevelObjectiveSpec.getServiceLevelIndicators().get(0).getType())
        .totalErrorBudget(totalErrorBudgetMinutes)
        .timeRemainingDays(timePeriod.getRemainingDays(currentLocalDate))
        .burnRate(SLODashboardWidget.BurnRate.builder()
                      .currentRatePercentage(sloGraphData.dailyBurnRate(serviceLevelObjective.getZoneOffset()))
                      .build())
        .build();
  }

  @Override
  public PageResponse<MSDropdownResponse> getSLOAssociatedMonitoredServices(
      ProjectParams projectParams, PageParams pageParams) {
    List<ServiceLevelObjective> serviceLevelObjectiveList = serviceLevelObjectiveService.getAllSLOs(projectParams);
    Set<String> monitoredServiceIdentifiers = serviceLevelObjectiveList.stream()
                                                  .map(ServiceLevelObjective::getMonitoredServiceIdentifier)
                                                  .collect(Collectors.toSet());
    List<MonitoredServiceResponse> monitoredServiceResponseList =
        monitoredServiceService.get(projectParams, monitoredServiceIdentifiers);

    List<MSDropdownResponse> msDropdownResponseList =
        monitoredServiceResponseList.stream()
            .map(monitoredServiceResponse -> getMSDropdownResponse(monitoredServiceResponse.getMonitoredServiceDTO()))
            .collect(Collectors.toList());

    return PageUtils.offsetAndLimit(msDropdownResponseList, pageParams.getPage(), pageParams.getSize());
  }

  private MSDropdownResponse getMSDropdownResponse(MonitoredServiceDTO monitoredServiceDTO) {
    return MSDropdownResponse.builder()
        .identifier(monitoredServiceDTO.getIdentifier())
        .name(monitoredServiceDTO.getName())
        .serviceRef(monitoredServiceDTO.getServiceRef())
        .environmentRef(monitoredServiceDTO.getEnvironmentRef())
        .build();
  }

  private SLOHealthListView getSLOListView(ProjectParams projectParams, AbstractServiceLevelObjective slo,
      Map<String, MonitoredServiceDTO> monitoredServiceIdentifierToDTOMap,
      Map<String, SLOHealthIndicator> sloIdentifierToHealthIndicatorMap,
      Map<String, String> userJourneyIdentifierToNameMap) {
    LocalDateTime currentLocalDate = LocalDateTime.ofInstant(clock.instant(), slo.getZoneOffset());
    List<SLOErrorBudgetResetDTO> errorBudgetResetDTOS =
        sloErrorBudgetResetService.getErrorBudgetResets(projectParams, slo.getIdentifier());
    int totalErrorBudgetMinutes = slo.getActiveErrorBudgetMinutes(errorBudgetResetDTOS, currentLocalDate);
    SLOHealthIndicator sloHealthIndicator = sloIdentifierToHealthIndicatorMap.get(slo.getIdentifier());
    List<UserJourneyDTO> userJourneys = slo.getUserJourneyIdentifiers()
                                            .stream()
                                            .map(userJourneyIdentifier
                                                -> UserJourneyDTO.builder()
                                                       .identifier(userJourneyIdentifier)
                                                       .name(userJourneyIdentifierToNameMap.get(userJourneyIdentifier))
                                                       .build())
                                            .collect(Collectors.toList());

    if (slo.getType().equals(ServiceLevelObjectiveType.SIMPLE)) {
      SimpleServiceLevelObjective simpleServiceLevelObjective = (SimpleServiceLevelObjective) slo;
      Preconditions.checkState(simpleServiceLevelObjective.getServiceLevelIndicators().size() == 1,
          "Only one service level indicator is supported");
      MonitoredServiceDTO monitoredService =
          monitoredServiceIdentifierToDTOMap.get(simpleServiceLevelObjective.getMonitoredServiceIdentifier());

      return SLOHealthListView.getSLOHealthListViewBuilder(slo, userJourneys, totalErrorBudgetMinutes, sloHealthIndicator)
          .monitoredServiceIdentifier(monitoredService.getIdentifier())
          .monitoredServiceName(monitoredService.getName())
          .environmentIdentifier(monitoredService.getEnvironmentRef())
          .environmentName(nextGenService
                               .getEnvironment(slo.getAccountId(), slo.getOrgIdentifier(), slo.getProjectIdentifier(),
                                   monitoredService.getEnvironmentRef())
                               .getName())
          .serviceName(nextGenService
                           .getService(slo.getAccountId(), slo.getOrgIdentifier(), slo.getProjectIdentifier(),
                               monitoredService.getServiceRef())
                           .getName())
          .serviceIdentifier(monitoredService.getServiceRef())
          .healthSourceIdentifier(simpleServiceLevelObjective.getHealthSourceIdentifier())
          .healthSourceName(
              getHealthSourceName(monitoredService, simpleServiceLevelObjective.getHealthSourceIdentifier()))
          .sliType(((SimpleServiceLevelObjective) slo).getServiceLevelIndicatorType())
          .build();
    }

    return SLOHealthListView.getSLOHealthListViewBuilder(slo, userJourneys, totalErrorBudgetMinutes, sloHealthIndicator).build();
  }

  private String getHealthSourceName(MonitoredServiceDTO monitoredServiceDTO, String healthSourceRef) {
    return monitoredServiceDTO.getSources()
        .getHealthSources()
        .stream()
        .filter(healthSource -> healthSource.getIdentifier().equals(healthSourceRef))
        .findFirst()
        .orElseThrow(()
                         -> new IllegalStateException(
                             "Health source identifier" + healthSourceRef + " not found in monitored service"))
        .getName();
  }
}
