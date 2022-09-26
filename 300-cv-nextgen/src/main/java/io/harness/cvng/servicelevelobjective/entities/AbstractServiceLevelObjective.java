/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.servicelevelobjective.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.notification.beans.NotificationRuleRef;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardDetail;
import io.harness.cvng.servicelevelobjective.beans.SLOErrorBudgetResetDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveType;
import io.harness.data.structure.CollectionUtils;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "ServiceLevelObjectiveV2Keys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@StoreIn(DbAliases.CVNG)
@Entity(value = "serviceLevelObjectivesV2")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.CV)
public abstract class AbstractServiceLevelObjective
    implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware, CreatedAtAware, PersistentRegularIterable {
  @NotNull String accountId;
  @NotNull String orgIdentifier;
  String projectIdentifier;
  @NotNull @Id private String uuid;
  @NotNull String identifier;
  @NotNull String name;
  String desc;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  List<String> userJourneyIdentifiers;
  List<NotificationRuleRef> notificationRuleRefs;
  @NotNull ServiceLevelObjective.SLOTarget sloTarget;
  private boolean enabled;
  private long lastUpdatedAt;
  private long createdAt;
  @NotNull private Double sloTargetPercentage;
  @FdIndex private long nextNotificationIteration;
  @NotNull ServiceLevelObjectiveType serviceLevelObjectiveType;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_query_idx")
                 .unique(true)
                 .field(ServiceLevelObjectiveV2Keys.accountId)
                 .field(ServiceLevelObjectiveV2Keys.orgIdentifier)
                 .field(ServiceLevelObjectiveV2Keys.projectIdentifier)
                 .field(ServiceLevelObjectiveV2Keys.identifier)
                 .build())
        .build();
  }

  public ZoneOffset getZoneOffset() {
    return ZoneOffset.UTC; // hardcoding it to UTC for now. We need to ask it from user.
  }

  public List<NotificationRuleRef> getNotificationRuleRefs() {
    if (notificationRuleRefs == null) {
      return Collections.emptyList();
    }
    return notificationRuleRefs;
  }

  public TimePeriod getCurrentTimeRange(LocalDateTime currentDateTime) {
    return sloTarget.getCurrentTimeRange(currentDateTime);
  }

  public List<SLODashboardDetail.TimeRangeFilter> getTimeRangeFilters() {
    return sloTarget.getTimeRangeFilters();
  }

  public int getTotalErrorBudgetMinutes(LocalDateTime currentDateTime) {
    int currentWindowMinutes = getCurrentTimeRange(currentDateTime).totalMinutes();
    Double errorBudgetPercentage = getSloTargetPercentage();
    return (int) Math.round(((100 - errorBudgetPercentage) * currentWindowMinutes) / 100);
  }

  public int getActiveErrorBudgetMinutes(
      List<SLOErrorBudgetResetDTO> sloErrorBudgetResets, LocalDateTime currentDateTime) {
    int totalErrorBudgetMinutes = getTotalErrorBudgetMinutes(currentDateTime);
    long totalErrorBudgetIncrementMinutesFromReset =
        CollectionUtils.emptyIfNull(sloErrorBudgetResets)
            .stream()
            .mapToLong(sloErrorBudgetResetDTO -> sloErrorBudgetResetDTO.getErrorBudgetIncrementMinutes())
            .sum();
    return Math.toIntExact(Math.min(getCurrentTimeRange(currentDateTime).totalMinutes(),
        totalErrorBudgetMinutes + totalErrorBudgetIncrementMinutesFromReset));
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (ServiceLevelObjectiveV2Keys.nextNotificationIteration.equals(fieldName)) {
      return this.nextNotificationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (ServiceLevelObjectiveV2Keys.nextNotificationIteration.equals(fieldName)) {
      this.nextNotificationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}
