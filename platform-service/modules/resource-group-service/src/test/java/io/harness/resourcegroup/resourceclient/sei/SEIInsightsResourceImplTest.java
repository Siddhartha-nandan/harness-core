/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resourcegroup.resourceclient.sei;

import io.harness.category.element.UnitTests;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.resourcegroup.framework.v1.service.ResourceInfo;

import com.google.protobuf.StringValue;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SEIInsightsResourceImplTest {
  @Test
  @Category(UnitTests.class)
  public void testGetResourceInfoFromEvent() {
    SEIInsightsResourceImpl seiInsightsResource = new SEIInsightsResourceImpl();

    EntityChangeDTO entityChangeDTO = EntityChangeDTO.newBuilder()
                                          .setAccountIdentifier(StringValue.of("kmpySmUISimoRrJL6NL73w"))
                                          .setOrgIdentifier(StringValue.of("default"))
                                          .setProjectIdentifier(StringValue.of("project-1"))
                                          .build();

    Message producerMessage = Message.newBuilder().setData(entityChangeDTO.toByteString()).build();

    io.harness.eventsframework.consumer.Message consumerMessage =
        io.harness.eventsframework.consumer.Message.newBuilder().setMessage(producerMessage).build();

    ResourceInfo resourceInfo = seiInsightsResource.getResourceInfoFromEvent(consumerMessage);
    Assertions.assertThat(resourceInfo.getAccountIdentifier()).isEqualTo("kmpySmUISimoRrJL6NL73w");
    Assertions.assertThat(resourceInfo.getOrgIdentifier()).isEqualTo("default");
    Assertions.assertThat(resourceInfo.getProjectIdentifier()).isEqualTo("project-1");
    Assertions.assertThat(resourceInfo.getResourceType()).isEqualTo("SEI_INSIGHTS");
  }
}
