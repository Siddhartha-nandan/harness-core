/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changestreams.eventhandlers;

import io.harness.eventHandler.DebeziumAbstractRedisEventHandler;
import io.harness.ssca.beans.instance.InstanceDTO;
import io.harness.ssca.services.CdInstanceSummaryService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
public class InstanceNGRedisEventHandler extends DebeziumAbstractRedisEventHandler {
  @Inject MongoTemplate mongoTemplate;
  @Inject CdInstanceSummaryService cdInstanceSummaryService;

  @VisibleForTesting
  public InstanceDTO createEntity(String value) {
    Document document = Document.parse(value);
    document.remove("instanceInfo");
    return mongoTemplate.getConverter().read(InstanceDTO.class, document);
  }

  @Override
  public boolean handleCreateEvent(String id, String value) {
    InstanceDTO instance = createEntity(value);
    return cdInstanceSummaryService.upsertInstance(instance);
  }

  @Override
  public boolean handleDeleteEvent(String id) {
    return true;
  }

  @Override
  public boolean handleUpdateEvent(String id, String value) {
    InstanceDTO instance = createEntity(value);
    if (instance.isDeleted()) {
      return cdInstanceSummaryService.removeInstance(instance);
    }
    return true;
  }
}
