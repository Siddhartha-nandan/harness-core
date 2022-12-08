/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAccess;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents response generated by a correlationId.
 */
@Value
@Builder
@FieldNameConstants(innerTypeName = "ProgressUpdateKeys")
@StoreIn(DbAliases.ALL)
@Document("progressUpdate")
@TypeAlias("progressUpdate")
@Entity(value = "progressUpdate", noClassnameStored = true)
@HarnessEntity(exportable = false)
@OwnedBy(HarnessTeam.DEL)
public class ProgressUpdate implements WaitEngineEntity, CreatedAtAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("correlation")
                 .field(ProgressUpdateKeys.correlationId)
                 .descSortField(ProgressUpdateKeys.createdAt)
                 .build())
        .build();
  }

  public static final Duration TTL = ofDays(21);

  @Id @org.springframework.data.annotation.Id String uuid;
  String correlationId;
  @FdIndex long createdAt;
  byte[] progressData;
  long expireProcessing;

  @Default @FdTtlIndex @NonFinal @Wither Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
  private boolean usingKryoWithoutReference;
}
