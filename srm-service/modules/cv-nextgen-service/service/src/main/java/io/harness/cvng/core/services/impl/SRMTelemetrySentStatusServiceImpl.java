/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.entities.SRMTelemetrySentStatus;
import io.harness.cvng.core.entities.SRMTelemetrySentStatus.SRMTelemetrySentStatusKeys;
import io.harness.cvng.core.services.api.SRMTelemetrySentStatusService;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.mongodb.MongoCommandException;
import dev.morphia.FindAndModifyOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRMTelemetrySentStatusServiceImpl implements SRMTelemetrySentStatusService {
  @Inject HPersistence hPersistence;

  @Override
  public boolean updateTimestampIfOlderThan(String accountId, long olderThanTime, long updateToTime) {
    Query<SRMTelemetrySentStatus> query = hPersistence.createQuery(SRMTelemetrySentStatus.class)
                                              .field(SRMTelemetrySentStatusKeys.accountId)
                                              .equal(accountId)
                                              .field(SRMTelemetrySentStatusKeys.lastSent)
                                              .lessThanOrEq(olderThanTime);

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().returnNew(true).upsert(true);
    UpdateOperations<SRMTelemetrySentStatus> updateOperations =
        hPersistence.createUpdateOperations(SRMTelemetrySentStatus.class)
            .set(SRMTelemetrySentStatusKeys.lastSent, updateToTime);
    SRMTelemetrySentStatus result;
    hPersistence.getDatastore(SRMTelemetrySentStatus.class)
        .getDatabase()
        .getCollection("srmTelemetrySentStatus")
        .listIndexes()
        .forEach(document -> {
          System.out.println(document.toJson());
          log.info(document.toJson());
        });

    hPersistence.getDatastore(ServiceLevelIndicator.class)
        .getDatabase()
        .getCollection("serviceLevelIndicator")
        .listIndexes()
        .forEach(document -> {
          System.out.println(document.toJson());
          log.info(document.toJson());
        });
    try {
      // Atomic lock acquiring attempt
      // Everything after this line is critical section
      result = hPersistence.findAndModify(query, updateOperations, findAndModifyOptions);
    } catch (MongoCommandException e) {
      // Account ID is unique here so setting upsert to true will throw Mongo command exception if trying to create
      // So we should return failed to acquire the lock here
      return false;
    }
    return result != null;
  }
}
