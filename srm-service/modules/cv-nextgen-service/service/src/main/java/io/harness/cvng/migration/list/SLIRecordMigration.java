/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.SRMPersistence;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordKeys;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SLIRecordMigration implements CVNGMigration {
  @Inject SRMPersistence hPersistence;

  private static final int BATCH_SIZE = 1000;

  List<SLIRecordBucket> sliRecordBucketsToSave;

  @Override
  public void migrate() {
    Query<SLIRecord> queryToAddToBucket =
        hPersistence.createQuery(SLIRecord.class)
            .order(Sort.ascending(SLIRecordKeys.sliId), Sort.ascending(SLIRecordKeys.timestamp));
    List<SLIRecord> currentBucket = new ArrayList<>();
    sliRecordBucketsToSave = new ArrayList<>();
    int offset = 0;
    while (true) {
      Query<SLIRecord> query = queryToAddToBucket.offset(offset).limit(BATCH_SIZE);
      List<SLIRecord> records = query.find().toList();
      if (records.isEmpty()) {
        break;
      }
      for (SLIRecord sliRecord : records) {
        currentBucket = processRecord(currentBucket, sliRecord);
        currentBucket = processBucket(currentBucket);
      }
      offset += BATCH_SIZE;
    }
    saveBuckets();
    log.info("[SLI Bucket Migration] Saved all sli bucket records");
  }

  private List<SLIRecord> processRecord(List<SLIRecord> currentBucket, SLIRecord sliRecord) {
    if (!currentBucket.isEmpty()
        && (currentBucket.get(currentBucket.size() - 1).getEpochMinute() + 1 == sliRecord.getEpochMinute())
        && (currentBucket.get(currentBucket.size() - 1).getSliId().equals(sliRecord.getSliId()))) {
      currentBucket.add(sliRecord);
      return currentBucket;
    }
    currentBucket = new ArrayList<>();
    if (sliRecord.getEpochMinute() % 5 == 0) {
      currentBucket.add(sliRecord);
    }
    return currentBucket;
  }

  private List<SLIRecord> processBucket(List<SLIRecord> currentBucket) {
    if (currentBucket.size() == 5) {
      SLIRecordBucket sliRecordBucket = SLIRecordBucket.getSLIRecordBucketFromSLIRecords(currentBucket);
      sliRecordBucketsToSave.add(sliRecordBucket);
      currentBucket = new ArrayList<>();
    }
    if (sliRecordBucketsToSave.size() >= BATCH_SIZE) {
      saveBuckets();
    }
    return currentBucket;
  }

  private void saveBuckets() {
    try {
      if (!sliRecordBucketsToSave.isEmpty()) {
        hPersistence.upsertBatch(SLIRecordBucket.class, sliRecordBucketsToSave, new ArrayList<>());
      }
      sliRecordBucketsToSave = new ArrayList<>();
    } catch (IllegalAccessException e) {
      throw new RuntimeException("[SLI Record Bucketing Error]", e);
    }
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
