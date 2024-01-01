/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.events.consumers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventHandler.DebeziumAbstractRedisEventHandler;
import io.harness.timescaledb.Tables;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class BackstageScaffolderTasksChangeEventHandler extends DebeziumAbstractRedisEventHandler {
  @Inject private DSLContext dsl;

  @Override
  public boolean handleCreateEvent(String id, String value) {
    Record insertRecord = createRecord(id, value);
    if (insertRecord == null) {
      return true;
    }
    try {
      dsl.insertInto(Tables.BACKSTAGE_SCAFFOLDER_TASKS)
          .set(insertRecord)
          .onConflict(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ID)
          .doUpdate()
          .set(insertRecord)
          .execute();
      log.debug("Successfully inserted data in backstage_scaffolder_tasks for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught exception while inserting data in backstage_scaffolder_tasks for id {}", id, ex);
      return false;
    }
    return false;
  }

  @Override
  public boolean handleDeleteEvent(String id) {
    try {
      dsl.delete(Tables.BACKSTAGE_SCAFFOLDER_TASKS).where(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ID.eq(id)).execute();
      log.debug("Successfully deleted data in backstage_scaffolder_tasks for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught exception while deleting data in backstage_scaffolder_tasks for id {}", id, ex);
      return false;
    }
    return true;
  }

  @Override
  public boolean handleUpdateEvent(String id, String value) {
    Record updateRecord = createRecord(id, value);
    if (updateRecord == null) {
      return true;
    }
    try {
      dsl.insertInto(Tables.BACKSTAGE_SCAFFOLDER_TASKS)
          .set(updateRecord)
          .onConflict(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ID)
          .doUpdate()
          .set(updateRecord)
          .execute();
      log.debug("Successfully updated data in backstage_scaffolder_tasks for id {}", id);
    } catch (DataAccessException ex) {
      log.error("Caught Exception while updating data in backstage_scaffolder_tasks for id {}", id, ex);
      return false;
    }
    return true;
  }

  @SneakyThrows
  public Record createRecord(String id, String value) {
    JsonNode node = objectMapper.readTree(value);

    Record createRecord = dsl.newRecord(Tables.BACKSTAGE_SCAFFOLDER_TASKS);
    createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ID, id);

    populateFromRoot(node, createRecord);
    populateFromSpec(node, createRecord);

    return createRecord;
  }

  private void populateFromRoot(JsonNode node, Record createRecord) {
    if (node.get("accountIdentifier") != null) {
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ACCOUNT_IDENTIFIER, node.get("accountIdentifier").asText());
    }

    if (node.get("identifier") != null) {
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.IDENTIFIER, node.get("identifier").asText());
    }

    if (node.get("status") != null) {
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.STATUS, node.get("status").asText());
    }

    long createdAt = 0;
    if (node.get("createdAt") != null) {
      createdAt = node.get("createdAt").asLong();
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.CREATED_AT, createdAt);
    }

    long lastHeartbeatAt = 0;
    if (node.get("lastHeartbeatAt") != null) {
      lastHeartbeatAt = node.get("lastHeartbeatAt").asLong();
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.LAST_HEARTBEAT_AT, lastHeartbeatAt);
    }

    createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.TASK_RUN_TIME_MINUTES,
        lastHeartbeatAt > 0 ? (short) ((lastHeartbeatAt - createdAt) / 60000) : 0);
  }

  @SneakyThrows
  private void populateFromSpec(JsonNode node, Record createRecord) {
    if (node.get("spec") != null) {
      JsonNode spec = objectMapper.readTree(node.get("spec").asText());
      populateFromSpecTemplateInfo(spec, createRecord);
      populateFromSpecSteps(spec, createRecord);
    }
  }

  private void populateFromSpecTemplateInfo(JsonNode spec, Record createRecord) {
    if (spec.get("templateInfo") != null) {
      JsonNode templateInfo = spec.get("templateInfo");
      createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.ENTITY_REF, templateInfo.get("entityRef").asText());
      if (templateInfo.get("entity") != null && templateInfo.get("entity").get("metadata") != null) {
        createRecord.set(
            Tables.BACKSTAGE_SCAFFOLDER_TASKS.NAME, templateInfo.get("entity").get("metadata").get("name").asText());
      }
    }
  }

  private void populateFromSpecSteps(JsonNode spec, Record createRecord) {
    if (spec.get("steps") != null) {
      JsonNode specSteps = spec.get("steps");
      if (specSteps.isArray()) {
        List<JsonNode> steps = StreamSupport.stream(specSteps.spliterator(), false).collect(Collectors.toList());
        createRecord.set(Tables.BACKSTAGE_SCAFFOLDER_TASKS.NUMBER_OF_STEPS, (short) steps.size());
      }
    }
  }
}
