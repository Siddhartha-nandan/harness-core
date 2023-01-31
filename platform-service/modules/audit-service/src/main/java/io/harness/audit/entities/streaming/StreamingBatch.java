/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.audit.entities.streaming;

import io.harness.annotations.StoreIn;
import io.harness.auditevent.streaming.beans.BatchFailureInfo;
import io.harness.auditevent.streaming.beans.BatchStatus;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "StreamingBatchKeys")
@StoreIn(DbAliases.AUDITS)
@Entity(value = "streamingBatches", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("streamingBatches")
@TypeAlias("StreamingBatch")
public class StreamingBatch {
  @Id String id;
  @NotBlank String streamingDestinationIdentifier;
  @NotBlank String accountIdentifier;
  @NotNull Long startTime;
  @NotNull Long endTime;
  Long lastSuccessfulRecordTimestamp;
  Long numberOfRecords;
  Long numberOfRecordsPublished;
  Long lastStreamedAt;
  @NotNull BatchStatus status;
  int retryCount;
  @Valid BatchFailureInfo failureInfo;
  @CreatedDate long createdAt;
  @LastModifiedDate long lastModifiedAt;
}
