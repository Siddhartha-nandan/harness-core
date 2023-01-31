/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.downtime.services.impl;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.downtime.beans.DowntimeDTO;
import io.harness.cvng.downtime.beans.EntityType;
import io.harness.cvng.downtime.beans.EntityUnavailabilityStatus;
import io.harness.cvng.downtime.beans.EntityUnavailabilityStatusesDTO;
import io.harness.cvng.downtime.entities.EntityUnavailabilityStatuses;
import io.harness.cvng.downtime.entities.EntityUnavailabilityStatuses.EntityUnavailabilityStatusesKeys;
import io.harness.cvng.downtime.services.api.EntityUnavailabilityStatusesService;
import io.harness.cvng.downtime.transformer.EntityUnavailabilityStatusesEntityAndDTOTransformer;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class EntityUnavailabilityStatusesServiceImpl implements EntityUnavailabilityStatusesService {
  @Inject private HPersistence hPersistence;

  @Inject private Clock clock;

  @Inject private MonitoredServiceService monitoredServiceService;

  @Inject private EntityUnavailabilityStatusesEntityAndDTOTransformer statusesEntityAndDTOTransformer;

  @Override
  public void create(
      ProjectParams projectParams, List<EntityUnavailabilityStatusesDTO> entityUnavailabilityStatusesDTOS) {
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        entityUnavailabilityStatusesDTOS.stream()
            .map(statusesDTO -> statusesEntityAndDTOTransformer.getEntity(projectParams, statusesDTO))
            .collect(Collectors.toList());
    hPersistence.save(entityUnavailabilityStatuses);
  }

  @Override
  public void update(ProjectParams projectParams, String entityId,
      List<EntityUnavailabilityStatusesDTO> entityUnavailabilityStatusesDTOS) {
    deleteFutureDowntimeInstances(projectParams, entityId);
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        entityUnavailabilityStatusesDTOS.stream()
            .map(statusesDTO -> statusesEntityAndDTOTransformer.getEntity(projectParams, statusesDTO))
            .collect(Collectors.toList());
    hPersistence.save(entityUnavailabilityStatuses);
  }

  @Override
  public List<EntityUnavailabilityStatusesDTO> getEntityUnavaialabilityStatusesDTOs(
      ProjectParams projectParams, DowntimeDTO downtimeDTO, List<Pair<Long, Long>> futureInstances) {
    List<EntityUnavailabilityStatusesDTO> entityUnavailabilityStatusesDTOS = new ArrayList<>();
    for (Pair<Long, Long> startAndEndTime : futureInstances) {
      entityUnavailabilityStatusesDTOS.add(EntityUnavailabilityStatusesDTO.builder()
                                               .entityId(downtimeDTO.getIdentifier())
                                               .orgIdentifier(projectParams.getOrgIdentifier())
                                               .projectIdentifier(projectParams.getProjectIdentifier())
                                               .status(EntityUnavailabilityStatus.MAINTENANCE_WINDOW)
                                               .entityType(EntityType.MAINTENANCE_WINDOW)
                                               .startTime(startAndEndTime.getLeft())
                                               .endTime(startAndEndTime.getRight())
                                               .build());
    }
    return entityUnavailabilityStatusesDTOS;
  }
  @Override
  public List<EntityUnavailabilityStatusesDTO> getPastInstances(ProjectParams projectParams) {
    List<EntityUnavailabilityStatuses> pastInstances = getPastInstancesQuery(projectParams).asList();
    return pastInstances.stream()
        .map(status -> statusesEntityAndDTOTransformer.getDto(status))
        .collect(Collectors.toList());
  }

  @Override
  public List<EntityUnavailabilityStatusesDTO> getAllInstances(ProjectParams projectParams) {
    List<EntityUnavailabilityStatuses> allInstances = getAllInstancesQuery(projectParams).asList();
    return allInstances.stream()
        .map(status -> statusesEntityAndDTOTransformer.getDto(status))
        .collect(Collectors.toList());
  }

  @Override
  public List<EntityUnavailabilityStatusesDTO> getAllInstances(
      ProjectParams projectParams, long startTime, long endTime) {
    List<EntityUnavailabilityStatuses> allInstances =
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .disableValidation()
            .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .field(EntityUnavailabilityStatusesKeys.startTime)
            .lessThanOrEq(endTime)
            .field(EntityUnavailabilityStatusesKeys.endTime)
            .greaterThanOrEq(startTime)
            .order(Sort.descending(EntityUnavailabilityStatusesKeys.endTime))
            .asList();
    return allInstances.stream()
        .map(status -> statusesEntityAndDTOTransformer.getDto(status))
        .collect(Collectors.toList());
  }

  @Override
  public List<EntityUnavailabilityStatusesDTO> getActiveOrFirstUpcomingInstance(
      ProjectParams projectParams, List<String> entityIds) {
    Query<EntityUnavailabilityStatuses> query =
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .field(EntityUnavailabilityStatusesKeys.entityIdentifier)
            .in(entityIds);
    query.or(query.criteria(EntityUnavailabilityStatusesKeys.startTime).greaterThanOrEq(clock.millis() / 1000),
        query.and(query.criteria(EntityUnavailabilityStatusesKeys.startTime).lessThanOrEq(clock.millis() / 1000),
            query.criteria(EntityUnavailabilityStatusesKeys.endTime).greaterThanOrEq(clock.millis() / 1000)));
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        query.order(Sort.ascending(EntityUnavailabilityStatusesKeys.startTime)).asList();
    Map<String, EntityUnavailabilityStatuses> firstUnavailabilityInstances = new HashMap<>();
    for (EntityUnavailabilityStatuses downtime : entityUnavailabilityStatuses) {
      if (firstUnavailabilityInstances.get(downtime.getEntityIdentifier()) == null) {
        firstUnavailabilityInstances.put(downtime.getEntityIdentifier(), downtime);
      }
    }
    return firstUnavailabilityInstances.values()
        .stream()
        .map(status -> statusesEntityAndDTOTransformer.getDto(status))
        .collect(Collectors.toList());
  }

  @Override
  public boolean deleteFutureDowntimeInstances(ProjectParams projectParams, String entityId) {
    return hPersistence.delete(getFutureInstances(projectParams, entityId));
  }

  @Override
  public boolean deleteAllInstances(ProjectParams projectParams, String entityId) {
    return hPersistence.delete(
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .filter(EntityUnavailabilityStatusesKeys.entityIdentifier, entityId));
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<EntityUnavailabilityStatuses> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .filter(EntityUnavailabilityStatusesKeys.accountId, accountId)
            .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, orgIdentifier)
            .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectIdentifier)
            .asList();
    entityUnavailabilityStatuses.forEach(entityUnavailabilityStatus
        -> deleteAllInstances(ProjectParams.builder()
                                  .accountIdentifier(entityUnavailabilityStatus.getAccountId())
                                  .orgIdentifier(entityUnavailabilityStatus.getOrgIdentifier())
                                  .projectIdentifier(entityUnavailabilityStatus.getProjectIdentifier())
                                  .build(),
            entityUnavailabilityStatus.getEntityIdentifier()));
  }

  @Override
  public void deleteByOrgIdentifier(Class<EntityUnavailabilityStatuses> clazz, String accountId, String orgIdentifier) {
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .filter(EntityUnavailabilityStatusesKeys.accountId, accountId)
            .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, orgIdentifier)
            .asList();
    entityUnavailabilityStatuses.forEach(entityUnavailabilityStatus
        -> deleteAllInstances(ProjectParams.builder()
                                  .accountIdentifier(entityUnavailabilityStatus.getAccountId())
                                  .orgIdentifier(entityUnavailabilityStatus.getOrgIdentifier())
                                  .projectIdentifier(entityUnavailabilityStatus.getProjectIdentifier())
                                  .build(),
            entityUnavailabilityStatus.getEntityIdentifier()));
  }

  @Override
  public void deleteByAccountIdentifier(Class<EntityUnavailabilityStatuses> clazz, String accountId) {
    List<EntityUnavailabilityStatuses> entityUnavailabilityStatuses =
        hPersistence.createQuery(EntityUnavailabilityStatuses.class)
            .filter(EntityUnavailabilityStatusesKeys.accountId, accountId)
            .asList();
    entityUnavailabilityStatuses.forEach(entityUnavailabilityStatus
        -> deleteAllInstances(ProjectParams.builder()
                                  .accountIdentifier(entityUnavailabilityStatus.getAccountId())
                                  .orgIdentifier(entityUnavailabilityStatus.getOrgIdentifier())
                                  .projectIdentifier(entityUnavailabilityStatus.getProjectIdentifier())
                                  .build(),
            entityUnavailabilityStatus.getEntityIdentifier()));
  }

  private Query<EntityUnavailabilityStatuses> getAllInstancesQuery(ProjectParams projectParams) {
    return hPersistence.createQuery(EntityUnavailabilityStatuses.class)
        .disableValidation()
        .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .order(Sort.descending(EntityUnavailabilityStatusesKeys.endTime));
  }
  private Query<EntityUnavailabilityStatuses> getPastInstancesQuery(ProjectParams projectParams) {
    return hPersistence.createQuery(EntityUnavailabilityStatuses.class)
        .disableValidation()
        .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .field(EntityUnavailabilityStatusesKeys.endTime)
        .lessThanOrEq(clock.millis() / 1000)
        .order(Sort.descending(EntityUnavailabilityStatusesKeys.endTime));
  }

  private Query<EntityUnavailabilityStatuses> getFutureInstances(ProjectParams projectParams, String entityId) {
    return hPersistence.createQuery(EntityUnavailabilityStatuses.class)
        .filter(EntityUnavailabilityStatusesKeys.accountId, projectParams.getAccountIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(EntityUnavailabilityStatusesKeys.entityIdentifier, entityId)
        .field(EntityUnavailabilityStatusesKeys.startTime)
        .greaterThanOrEq(clock.millis() / 1000);
  }
}
