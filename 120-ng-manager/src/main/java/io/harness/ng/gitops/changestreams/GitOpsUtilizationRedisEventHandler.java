/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.gitops.changestreams;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventHandler.DebeziumAbstractRedisEventHandler;
import io.harness.timescaledb.Tables;
import io.harness.timescaledb.tables.records.NgInstanceStatsRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
@OwnedBy(GITOPS)
public class GitOpsUtilizationRedisEventHandler extends DebeziumAbstractRedisEventHandler {
  @Inject private DSLContext dsl;

  @SneakyThrows
  private NgInstanceStatsRecord createRecord(String value, String id) {
    JsonNode node = objectMapper.readTree(value);
    NgInstanceStatsRecord record = dsl.newRecord(Tables.NG_INSTANCE_STATS);
    return record;
  }

  @Override
  public boolean handleCreateEvent(String id, String value) {
    return false;
  }

  @Override
  public boolean handleDeleteEvent(String id) {
    return false;
  }

  @Override
  public boolean handleUpdateEvent(String id, String value) {
    return false;
  }
}
