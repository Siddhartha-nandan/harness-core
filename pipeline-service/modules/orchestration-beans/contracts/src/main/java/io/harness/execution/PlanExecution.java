/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.execution;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.governance.GovernanceMetadata;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.UuidAccess;
import io.harness.plan.NodeType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.UtilityClass;
import lombok.experimental.Wither;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_FIRST_GEN})
@OwnedBy(PIPELINE)
@Value
@Builder
@FieldNameConstants(innerTypeName = "PlanExecutionKeys")
@StoreIn(DbAliases.PMS)
@Entity(value = "planExecutions", noClassnameStored = true)
@Document("planExecutions")
@JsonIgnoreProperties(ignoreUnknown = true, value = {"plan"})
@TypeAlias("planExecution")
public class PlanExecution implements PersistentRegularIterable, UuidAccess, PmsNodeExecution {
  public static final String EXEC_TAG_SET_BY_TRIGGER = "execution_trigger_tag_needed_for_abort";
  public static final long TTL_MONTHS = 6;

  @Wither @Id @dev.morphia.annotations.Id String uuid;
  @Wither @CreatedDate Long createdAt;
  String planId;
  Map<String, String> setupAbstractions;
  // TTL index
  @Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());

  Status status;
  Long startTs;
  Long endTs;

  ExecutionMetadata metadata;
  GovernanceMetadata governanceMetadata;

  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @Version Long version;

  @Getter @NonFinal @Setter Long nextIteration;
  Ambiance ambiance;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public String getNodeId() {
    return planId;
  }

  @Override
  public NodeType getNodeType() {
    return NodeType.PLAN;
  }

  @UtilityClass
  public static class ExecutionMetadataKeys {
    public static final String tagExecutionKey =
        PlanExecutionKeys.metadata + ".triggerInfo.triggeredBy.extraInfo." + EXEC_TAG_SET_BY_TRIGGER;
  }

  @UtilityClass
  public static class PlanExecutionKeys {
    public static final String accountId = PlanExecutionKeys.setupAbstractions + "." + SetupAbstractionKeys.accountId;
    public static final String planId = PlanExecutionKeys.ambiance + ".planId";
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList
        .<MongoIndex>builder()
        // PlanExecutionMonitorService
        .add(CompoundMongoIndex.builder().name("status_idx").field(PlanExecutionKeys.status).build())
        // findPrevUnTerminatedPlanExecutionsByExecutionTag
        .add(SortCompoundMongoIndex.builder()
                 .name("exec_tag_status_idx")
                 .field(ExecutionMetadataKeys.tagExecutionKey)
                 .field(PlanExecutionKeys.status)
                 .descSortField(PlanExecutionKeys.createdAt)
                 .build())
        // countRunningExecutionsForGivenPipelineInAccount
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_status_createdAt_idx")
                 .field(PlanExecutionKeys.setupAbstractions + "." + SetupAbstractionKeys.accountId)
                 .field(PlanExecutionKeys.status)
                 .ascSortField(PlanExecutionKeys.createdAt)
                 .build())
        .build();
  }

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("planExecutionId", uuid);
    logContext.putAll(setupAbstractions != null ? setupAbstractions : new HashMap<>());
    if (metadata != null) {
      logContext.put("pipelineIdentifier", metadata.getPipelineIdentifier());
      logContext.put("triggerType", metadata.getTriggerInfo().getTriggerType().toString());
      logContext.put("triggeredBy", metadata.getTriggerInfo().getTriggeredBy().getIdentifier());
    }
    return logContext;
  }
}
