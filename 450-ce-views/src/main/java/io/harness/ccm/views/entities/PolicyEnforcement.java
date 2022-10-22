/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "PolicyEnforcementId")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "governancePolicyEnforcement", noClassnameStored = true)
@Schema(description = "This object will contain the complete definition of a Cloud Cost Policy enforcement")

public final class PolicyEnforcement implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware,
                                                AccountAccess, CreatedByAware, UpdatedByAware {
  @Id @Schema(description = "unique id") String uuid;
  @Schema(description = "account id") String accountId;
  @Schema(description = "name") String name;
  @Schema(description = NGCommonEntityConstants.DESCRIPTION) String description;
  @Schema(description = NGCommonEntityConstants.TAGS) List<String> tags;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @Schema(description = "cloudProvider") String cloudProvider;
  @Schema(description = "policyIds") List<String> policyIds;
  @Schema(description = "policyPackIDs") List<String> policyPackIDs;
  @Schema(description = "executionSchedule") String executionSchedule;
  @Schema(description = "executionTimezone") String executionTimezone;
  @Schema(description = "targetAccounts") List<String> targetAccounts;
  @Schema(description = "targetRegions") List<String> targetRegions;
  @Schema(description = "isDryRun") Boolean isDryRun;
  @Schema(description = "deleted") String deleted;
  @Schema(description = "isEnabled") String isEnabled;
  @Schema(description = NGCommonEntityConstants.CREATED_AT_MESSAGE) long createdAt;
  @Schema(description = NGCommonEntityConstants.UPDATED_AT_MESSAGE) long lastUpdatedAt;
  @Schema(description = "created by") private EmbeddedUser createdBy;
  @Schema(description = "updated by") private EmbeddedUser lastUpdatedBy;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("PolicyEnforcement")
                 .field(PolicyEnforcementId.name)
                 .field(PolicyEnforcementId.accountId)
                 .field(PolicyEnforcementId.cloudProvider)
                 .field(PolicyEnforcementId.orgIdentifier)
                 .field(PolicyEnforcementId.projectIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder().name("sort1").field(PolicyEnforcementId.lastUpdatedAt).build())
        .add(CompoundMongoIndex.builder().name("sort2").field(PolicyEnforcementId.createdAt).build())
        .build();
  }
  public PolicyEnforcement toDTO() {
    return PolicyEnforcement.builder()
        .uuid(getUuid())
        .accountId(getAccountId())
        .name(getName())
        .description(getDescription())
        .tags(getTags())
        .orgIdentifier(getOrgIdentifier())
        .projectIdentifier(getProjectIdentifier())
        .cloudProvider(getCloudProvider())
        .executionSchedule(getExecutionSchedule())
        .executionTimezone(getExecutionTimezone())
        .policyIds(getPolicyIds())
        .policyPackIDs(getPolicyPackIDs())
        .targetAccounts(getTargetAccounts())
        .targetRegions(getTargetRegions())
        .isDryRun(getIsDryRun())
        .isEnabled(getIsEnabled())
        .deleted(getDeleted())
        .createdAt(getCreatedAt())
        .lastUpdatedAt(getLastUpdatedAt())
        .createdBy(getCreatedBy())
        .lastUpdatedBy(getLastUpdatedBy())
        .build();
  }
}
