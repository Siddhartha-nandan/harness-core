/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serviceoverridesv2.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.serviceoverridesv2.validators.ServiceOverrideValidatorService;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.events.EnvironmentUpdatedEvent;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.serviceoverridesv2.custom.ServiceOverrideRepositoryHelper;
import io.harness.repositories.serviceoverridesv2.spring.ServiceOverridesRepositoryV2;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.client.result.DeleteResult;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ServiceOverridesServiceV2Impl implements ServiceOverridesServiceV2 {
  private final ServiceOverridesRepositoryV2 serviceOverrideRepositoryV2;
  private final OutboxService outboxService;
  private final ServiceOverrideValidatorService overrideValidatorService;
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_RETRY_POLICY;
  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private final TransactionTemplate transactionTemplate;

  @Inject
  public ServiceOverridesServiceV2Impl(ServiceOverridesRepositoryV2 serviceOverrideRepositoryV2,
      OutboxService outboxService, ServiceOverrideValidatorService overrideValidatorService,
      TransactionTemplate transactionTemplate) {
    this.serviceOverrideRepositoryV2 = serviceOverrideRepositoryV2;
    this.outboxService = outboxService;
    this.overrideValidatorService = overrideValidatorService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public Optional<NGServiceOverridesEntity> get(@NotNull String accountId, String orgIdentifier,
      String projectIdentifier, @NonNull String serviceOverridesIdentifier) {
    return serviceOverrideRepositoryV2
        .getNGServiceOverridesEntityByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountId, orgIdentifier, projectIdentifier, serviceOverridesIdentifier);
  }

  @Override
  public NGServiceOverridesEntity create(@NonNull NGServiceOverridesEntity requestedEntity) {
    validatePresenceOfRequiredFields(
        requestedEntity.getAccountId(), requestedEntity.getEnvironmentRef(), requestedEntity.getType());
    modifyRequestedServiceOverride(requestedEntity);
    Optional<NGServiceOverridesEntity> existingEntity =
        serviceOverrideRepositoryV2
            .getNGServiceOverridesEntityByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
                requestedEntity.getAccountId(), requestedEntity.getOrgIdentifier(),
                requestedEntity.getProjectIdentifier(), requestedEntity.getIdentifier());
    if (existingEntity.isPresent()) {
      throw new InvalidRequestException(
          String.format("Service Override with identifier [%s] already exists", requestedEntity.getIdentifier()));
    }

    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      NGServiceOverridesEntity tempCreateResult = serviceOverrideRepositoryV2.save(requestedEntity);
      if (tempCreateResult == null) {
        throw new InvalidRequestException(String.format(
            "NGServiceOverridesEntity under Project[%s], Organization [%s], Environment [%s] and Service [%s] couldn't be created.",
            requestedEntity.getProjectIdentifier(), requestedEntity.getOrgIdentifier(),
            requestedEntity.getEnvironmentRef(), requestedEntity.getServiceRef()));
      }

      outboxService.save(EnvironmentUpdatedEvent.builder()
                             .accountIdentifier(tempCreateResult.getAccountId())
                             .orgIdentifier(tempCreateResult.getOrgIdentifier())
                             .status(EnvironmentUpdatedEvent.Status.CREATED)
                             .resourceType(EnvironmentUpdatedEvent.ResourceType.SERVICE_OVERRIDE)
                             .projectIdentifier(tempCreateResult.getProjectIdentifier())
                             .newServiceOverridesEntity(tempCreateResult)
                             .build());

      return tempCreateResult;
    }));
  }

  @Override
  public NGServiceOverridesEntity update(@NonNull @Valid NGServiceOverridesEntity requestedEntity) {
    validatePresenceOfRequiredFields(
        requestedEntity.getAccountId(), requestedEntity.getEnvironmentRef(), requestedEntity.getType());
    modifyRequestedServiceOverride(requestedEntity);
    Criteria equalityCriteria = ServiceOverrideRepositoryHelper.getEqualityCriteriaForServiceOverride(
        requestedEntity.getAccountId(), requestedEntity.getOrgIdentifier(), requestedEntity.getProjectIdentifier(),
        requestedEntity.getIdentifier());
    Optional<NGServiceOverridesEntity> existingEntityInDb = get(requestedEntity.getAccountId(),
        requestedEntity.getOrgIdentifier(), requestedEntity.getProjectIdentifier(), requestedEntity.getIdentifier());

    if (existingEntityInDb.isPresent()) {
      overrideValidatorService.checkForImmutableProperties(existingEntityInDb.get(), requestedEntity);

      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        NGServiceOverridesEntity tempResult = serviceOverrideRepositoryV2.update(equalityCriteria, requestedEntity);
        if (tempResult == null) {
          throw new InvalidRequestException(String.format(
              "ServiceOverride [%s] under Project [%s], Organization [%s] couldn't be updated or doesn't exist.",
              requestedEntity.getIdentifier(), requestedEntity.getProjectIdentifier(),
              requestedEntity.getOrgIdentifier()));
        }
        outboxService.save(EnvironmentUpdatedEvent.builder()
                               .accountIdentifier(tempResult.getAccountId())
                               .orgIdentifier(tempResult.getOrgIdentifier())
                               .projectIdentifier(tempResult.getProjectIdentifier())
                               .newServiceOverridesEntity(tempResult)
                               .resourceType(EnvironmentUpdatedEvent.ResourceType.SERVICE_OVERRIDE)
                               .status(EnvironmentUpdatedEvent.Status.UPDATED)
                               .oldServiceOverridesEntity(existingEntityInDb.get())
                               .build());
        return tempResult;
      }));
    } else {
      throw new InvalidRequestException(String.format(
          "ServiceOverride [%s] under Project[%s], Organization [%s] doesn't exist.", requestedEntity.getIdentifier(),
          requestedEntity.getProjectIdentifier(), requestedEntity.getOrgIdentifier()));
    }
  }

  @Override
  public boolean delete(@NonNull String accountId, String orgIdentifier, String projectIdentifier,
      @NonNull String identifier, NGServiceOverridesEntity existingEntity) {
    if (existingEntity == null) {
      existingEntity = checkIfServiceOverrideExistAndThrow(accountId, orgIdentifier, projectIdentifier, identifier);
    }

    return deleteInternal(accountId, orgIdentifier, projectIdentifier, identifier, existingEntity);
  }

  @Override
  public Page<NGServiceOverridesEntity> list(Criteria criteria, Pageable pageRequest) {
    return serviceOverrideRepositoryV2.findAll(criteria, pageRequest);
  }

  private NGServiceOverridesEntity checkIfServiceOverrideExistAndThrow(
      @NotNull String accountId, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    Optional<NGServiceOverridesEntity> existingOverrideInDb =
        serviceOverrideRepositoryV2
            .getNGServiceOverridesEntityByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
                accountId, orgIdentifier, projectIdentifier, identifier);
    if (existingOverrideInDb.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Service Override with identifier: [%s], projectId: [%s], orgId: [%s] does not exist",
              identifier, projectIdentifier, orgIdentifier));
    }
    return existingOverrideInDb.get();
  }

  private boolean deleteInternal(@NonNull String accountId, String orgIdentifier, String projectIdentifier,
      @NonNull String identifier, @NonNull NGServiceOverridesEntity existingEntity) {
    Criteria equalityCriteria = ServiceOverrideRepositoryHelper.getEqualityCriteriaForServiceOverride(
        accountId, orgIdentifier, projectIdentifier, identifier);

    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      DeleteResult deleteResult = serviceOverrideRepositoryV2.delete(equalityCriteria);
      if (!deleteResult.wasAcknowledged() || deleteResult.getDeletedCount() != 1) {
        throw new InvalidRequestException(
            String.format("Service Override [%s], Project[%s], Organization [%s] couldn't be deleted.", identifier,
                projectIdentifier, orgIdentifier));
      }
      outboxService.save(EnvironmentUpdatedEvent.builder()
                             .accountIdentifier(accountId)
                             .orgIdentifier(orgIdentifier)
                             .projectIdentifier(projectIdentifier)
                             .status(EnvironmentUpdatedEvent.Status.DELETED)
                             .resourceType(EnvironmentUpdatedEvent.ResourceType.SERVICE_OVERRIDE)
                             .oldServiceOverridesEntity(existingEntity)
                             .build());
      return true;
    }));
  }

  private void modifyRequestedServiceOverride(NGServiceOverridesEntity requestServiceOverride) {
    if (isEmpty(requestServiceOverride.getIdentifier())) {
      requestServiceOverride.setIdentifier(createOverrideIdentifier(requestServiceOverride));
    }
  }

  private String createOverrideIdentifier(NGServiceOverridesEntity requestServiceOverride) {
    return overrideValidatorService.generateServiceOverrideIdentifier(requestServiceOverride);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }
}
