/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changestreams.eventhandlers;

import io.harness.entities.Instance;
import io.harness.entities.instanceinfo.K8sInstanceInfo;
import io.harness.eventHandler.DebeziumAbstractRedisEventHandler;
import io.harness.k8s.model.K8sContainer;
import io.harness.ssca.services.CdInstanceSummaryService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
public class InstanceNGRedisEventHandler extends DebeziumAbstractRedisEventHandler {
  @Inject MongoTemplate mongoTemplate;
  @Inject CdInstanceSummaryService cdInstanceSummaryService;

  private static final String NAB_ACCOUNT_ID = "7i5sLmXBSne4D8bPq52bSw";
  private static final String K8S_INSTANCE_INFO_CLASS = "io.harness.entities.instanceinfo.K8sInstanceInfo";
  private static final String _CLASS = "_class";

  private Instance createEntity(String value) {
    Document document = Document.parse(value);
    Document instanceInfo = (Document) document.remove("instanceInfo");
    Instance instance = mongoTemplate.getConverter().read(Instance.class, document);
    if (NAB_ACCOUNT_ID.equals(instance.getAccountIdentifier()) && instanceInfo != null
        && K8S_INSTANCE_INFO_CLASS.equals(instanceInfo.get(_CLASS))) {
      instance.setInstanceInfo(mapToK8sInstanceInfo(instanceInfo));
    }
    return instance;
  }

  @Override
  public boolean handleCreateEvent(String id, String value) {
    Instance instance = createEntity(value);
    return cdInstanceSummaryService.upsertInstance(instance);
  }

  @Override
  public boolean handleDeleteEvent(String id) {
    return true;
  }

  @Override
  public boolean handleUpdateEvent(String id, String value) {
    Instance instance = createEntity(value);
    if (instance.isDeleted()) {
      return cdInstanceSummaryService.removeInstance(instance);
    }
    return true;
  }

  private K8sInstanceInfo mapToK8sInstanceInfo(Document instanceInfo) {
    // Debezium converts list to map. so, mongo won't be able to map containerList directly.
    // Writing manual mapper for the same.
    Document containerMap = (Document) instanceInfo.remove("containerList");
    K8sInstanceInfo k8sInstanceInfo = mongoTemplate.getConverter().read(K8sInstanceInfo.class, instanceInfo);
    if (containerMap != null) {
      Collection<Object> k8sContainerList = containerMap.values();
      List<K8sContainer> containers = new ArrayList<>();
      for (Object object : k8sContainerList) {
        Document document = (Document) object;
        String image = getFieldStringValueOrNull(document, "image");
        String name = getFieldStringValueOrNull(document, "name");
        String containerId = getFieldStringValueOrNull(document, "containerId");
        containers.add(K8sContainer.builder().image(image).name(name).containerId(containerId).build());
      }
      k8sInstanceInfo.setContainerList(containers);
    }
    return k8sInstanceInfo;
  }

  private String getFieldStringValueOrNull(Document document, String field) {
    if (document.get(field) == null) {
      return null;
    }
    return document.get(field).toString();
  }
}
