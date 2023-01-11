/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@StoreIn(DbAliases.ALL)
@Entity(value = "!!!custom_delegateTaskProgressResponses", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateTaskProgressResponseKeys")
public class DelegateTaskProgressResponse implements PersistentEntity {
  @Id @org.springframework.data.annotation.Id private String uuid;
  private String correlationId;
  private byte[] progressData;
  @FdIndex private long processAfter;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusHours(2).toInstant());

  private Boolean usingKryoWithoutReference;

  public Boolean getUsingKryoWithoutReference() {
    return isUsingKryoWithoutReference();
  }

  public boolean isUsingKryoWithoutReference() {
    if (usingKryoWithoutReference == null) {
      return false;
    }
    return usingKryoWithoutReference;
  }
}
