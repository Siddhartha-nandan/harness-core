/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.springdata;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.transaction.TransactionException;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class PersistenceUtils {
  public static final RetryPolicy<Object> DEFAULT_RETRY_POLICY =
      getRetryPolicy("Retrying Operation. Attempt No. {}", "Operation Failed. Attempt No. {}");

  public static RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    log.info("inside retry function");
    return new RetryPolicy<>()
        .handleIf(ex -> {
          log.info("inside retry policy object");
          if ((ex instanceof TransactionException) || (ex instanceof TransientDataAccessException)) {
            return true;
          } else if (ex instanceof MongoException) {
            log.info(format("encountered MongoException exception: %s, retrying.", ex));
            return ((MongoException) ex).hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
          } else if (ex instanceof UncategorizedMongoDbException) {
            log.info(format("encountered UncategorizedMongoDbException exception: %s, retrying.", ex));
            boolean b =
                ((MongoCommandException) ex.getCause()).hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
            log.info(format("has transienterror label: %s", b));
            log.info(format("ex.getCause(): %s", ex.getCause().toString()));
            return true;
          }
          return false;
        })
        .withBackoff(1, 10, ChronoUnit.SECONDS)
        .withMaxAttempts(3)
        .onFailedAttempt(event -> log.warn(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
  public static RetryPolicy<Object> getRetryPolicyWithDuplicateKeyException(
      String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handleIf(ex -> {
          if ((ex instanceof TransactionException) || (ex instanceof TransientDataAccessException)
              || (ex instanceof DuplicateKeyException)) {
            return true;
          } else if (ex instanceof MongoException) {
            return ((MongoException) ex).hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
          }
          return false;
        })
        .withBackoff(1, 10, ChronoUnit.SECONDS)
        .withMaxAttempts(3)
        .onFailedAttempt(event -> log.warn(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
