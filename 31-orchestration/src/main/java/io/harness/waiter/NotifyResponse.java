package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.delegate.beans.ResponseData;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Represents response generated by a correlationId.
 */
@Value
@Builder
@FieldNameConstants(innerTypeName = "NotifyResponseKeys")
@Entity(value = "notifyResponses", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class NotifyResponse implements PersistentEntity, UuidAccess, CreatedAtAccess {
  public static final Duration TTL = ofDays(21);

  @Id private String uuid;
  @FdIndex private long createdAt;
  private ResponseData response;
  private boolean error;

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}
