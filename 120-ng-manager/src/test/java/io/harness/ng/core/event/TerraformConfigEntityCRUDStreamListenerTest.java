/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;
import static io.harness.rule.OwnerRule.TMACARI;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

@OwnedBy(CDP)
public class TerraformConfigEntityCRUDStreamListenerTest extends CategoryTest {
  private TerraformConfigEntityCRUDEventHandler terraformConfigEntityCRUDEventHandler;
  @Inject @InjectMocks TerraformConfigEntityCRUDStreamListener terraformConfigEntityCRUDStreamListener;

  @Before
  public void setup() {
    terraformConfigEntityCRUDEventHandler = mock(TerraformConfigEntityCRUDEventHandler.class);
    terraformConfigEntityCRUDStreamListener =
        spy(new TerraformConfigEntityCRUDStreamListener(terraformConfigEntityCRUDEventHandler));
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testAccountDeleteEvent() {
    String accountId = randomAlphabetic(10);
    Message message = Message.newBuilder()
                          .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountId,
                                              EventsFrameworkMetadataConstants.ENTITY_TYPE, ACCOUNT_ENTITY,
                                              EventsFrameworkMetadataConstants.ACTION,
                                              EventsFrameworkMetadataConstants.DELETE_ACTION))
                                          .setData(getAccountPayload(accountId))
                                          .build())
                          .build();
    final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    when(terraformConfigEntityCRUDEventHandler.deleteAssociatedTerraformConfigForAccount(any())).thenReturn(true);
    boolean result = terraformConfigEntityCRUDStreamListener.handleMessage(message);
    verify(terraformConfigEntityCRUDEventHandler, times(1))
        .deleteAssociatedTerraformConfigForAccount(idCaptor.capture());
    assertEquals(idCaptor.getValue(), accountId);
    assertTrue(result);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testAccountChangeEvent_withoutAnyActionSetInMetadata() {
    String accountId = randomAlphabetic(10);
    Message message = Message.newBuilder()
                          .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountId,
                                              EventsFrameworkMetadataConstants.ENTITY_TYPE, ACCOUNT_ENTITY))
                                          .setData(getAccountPayload(accountId))
                                          .build())
                          .build();
    boolean result = terraformConfigEntityCRUDStreamListener.handleMessage(message);
    verify(terraformConfigEntityCRUDEventHandler, times(0)).deleteAssociatedTerraformConfigForAccount(any());
    assertTrue(result);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testOrganizationDeleteEvent() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    Message message = Message.newBuilder()
                          .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                                              EventsFrameworkMetadataConstants.ENTITY_TYPE, ORGANIZATION_ENTITY,
                                              EventsFrameworkMetadataConstants.ACTION,
                                              EventsFrameworkMetadataConstants.DELETE_ACTION))
                                          .setData(getOrganizationPayload(accountIdentifier, identifier))
                                          .build())
                          .build();
    final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    when(terraformConfigEntityCRUDEventHandler.deleteAssociatedTerraformConfigForOrganization(any(), any()))
        .thenReturn(true);
    terraformConfigEntityCRUDStreamListener.handleMessage(message);
    verify(terraformConfigEntityCRUDEventHandler, times(1))
        .deleteAssociatedTerraformConfigForOrganization(any(), idCaptor.capture());
    assertEquals(idCaptor.getValue(), identifier);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testProjectDeleteEvent() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    Message message = Message.newBuilder()
                          .setMessage(io.harness.eventsframework.producer.Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                                              EventsFrameworkMetadataConstants.ENTITY_TYPE, PROJECT_ENTITY,
                                              EventsFrameworkMetadataConstants.ACTION,
                                              EventsFrameworkMetadataConstants.DELETE_ACTION))
                                          .setData(getProjectPayload(accountIdentifier, orgIdentifier, identifier))
                                          .build())
                          .build();
    final ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    when(terraformConfigEntityCRUDEventHandler.deleteAssociatedTerraformConfigForProject(any(), any(), any()))
        .thenReturn(true);
    terraformConfigEntityCRUDStreamListener.handleMessage(message);
    verify(terraformConfigEntityCRUDEventHandler, times(1))
        .deleteAssociatedTerraformConfigForProject(any(), any(), idCaptor.capture());
    assertEquals(idCaptor.getValue(), identifier);
  }

  private ByteString getProjectPayload(String accountIdentifier, String orgIdentifier, String identifier) {
    return ProjectEntityChangeDTO.newBuilder()
        .setIdentifier(identifier)
        .setOrgIdentifier(orgIdentifier)
        .setAccountIdentifier(accountIdentifier)
        .build()
        .toByteString();
  }

  private ByteString getOrganizationPayload(String accountIdentifier, String identifier) {
    return OrganizationEntityChangeDTO.newBuilder()
        .setIdentifier(identifier)
        .setAccountIdentifier(accountIdentifier)
        .build()
        .toByteString();
  }

  private ByteString getAccountPayload(String identifier) {
    return AccountEntityChangeDTO.newBuilder().setAccountId(identifier).build().toByteString();
  }
}
