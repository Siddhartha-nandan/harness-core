/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.auditevent.streaming.mappers;

import static io.harness.audit.AuditCommonConstants.USER_ID;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.NISHANT;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.audit.Action;
import io.harness.audit.ResourceType;
import io.harness.audit.beans.Principal;
import io.harness.audit.beans.PrincipalType;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventBuilder;
import io.harness.audit.entities.AuthenticationInfo;
import io.harness.audit.entities.Resource;
import io.harness.audit.entities.ResourceScope;
import io.harness.auditevent.streaming.entities.outgoing.OutgoingAuditMessage;
import io.harness.auditevent.streaming.mappers.AuditEventMapper;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;
import io.harness.rule.Owner;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AuditEventMapperTest extends CategoryTest {
  public static final String REQUEST_METHOD_POST = "POST";
  AuditEventMapper auditEventMapper;

  @Before
  public void setup() {
    this.auditEventMapper = new AuditEventMapper();
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testToOutgoingAuditMessage() {
    List<AuditEvent> auditEventList = getAuditEventList();
    auditEventList.forEach(auditEvent -> {
      OutgoingAuditMessage outgoingMessage = auditEventMapper.toOutgoingAuditMessage(auditEvent);
      assertThat(outgoingMessage.getAuditEventId()).isEqualTo(auditEvent.getId());
      assertThat(outgoingMessage.getAuditModule()).isEqualTo(auditEvent.getModule());
      assertThat(outgoingMessage.getAuditResource()).isEqualToComparingFieldByField(auditEvent.getResource());
      assertThat(outgoingMessage.getAuditResourceScope()).isEqualToComparingFieldByField(auditEvent.getResourceScope());
      assertThat(outgoingMessage.getAuditAction()).isEqualTo(auditEvent.getAction());
      assertThat(outgoingMessage.getAuditEventTime()).isEqualTo(auditEvent.getTimestamp());

      if (auditEvent.getHttpRequestInfo() == null && auditEvent.getRequestMetadata() == null) {
        assertThat(outgoingMessage.getAuditHttpRequestInfo()).isNull();
      }
      if (auditEvent.getHttpRequestInfo() != null) {
        assertThat(outgoingMessage.getAuditHttpRequestInfo().getRequestMethod())
            .isEqualTo(auditEvent.getHttpRequestInfo().getRequestMethod());
      }
      if (auditEvent.getRequestMetadata() != null) {
        assertThat(outgoingMessage.getAuditHttpRequestInfo().getClientIP())
            .isEqualTo(auditEvent.getRequestMetadata().getClientIP());
      }

      assertThat(outgoingMessage.getAuditEventAuthor().getPrincipal().getType())
          .isEqualTo(auditEvent.getAuthenticationInfo().getPrincipal().getType());
      if (auditEvent.getAuthenticationInfo().getPrincipal().getType().equals(PrincipalType.USER)) {
        assertThat(outgoingMessage.getAuditEventAuthor().getPrincipal().getEmail())
            .isEqualTo(auditEvent.getAuthenticationInfo().getPrincipal().getIdentifier());
      } else {
        assertThat(outgoingMessage.getAuditEventAuthor().getPrincipal().getEmail()).isNull();
      }
      if (isNotEmpty(auditEvent.getAuthenticationInfo().getLabels())) {
        assertThat(outgoingMessage.getAuditEventAuthor().getPrincipal().getIdentifier())
            .isEqualTo(auditEvent.getAuthenticationInfo()
                           .getLabels()
                           .stream()
                           .collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue))
                           .get(USER_ID));
      } else {
        assertThat(outgoingMessage.getAuditEventAuthor().getPrincipal().getIdentifier())
            .isEqualTo(auditEvent.getAuthenticationInfo().getPrincipal().getIdentifier());
      }
    });
  }

  private List<AuditEvent> getAuditEventList() {
    AuditEvent auditEvent1 =
        getCommonBuilder()
            .authenticationInfo(
                AuthenticationInfo.builder()
                    .principal(Principal.builder().type(PrincipalType.USER).identifier(randomAlphabetic(15)).build())
                    .labels(List.of(KeyValuePair.builder().key(USER_ID).value(randomAlphabetic(14)).build()))
                    .build())
            .httpRequestInfo(HttpRequestInfo.builder().requestMethod(REQUEST_METHOD_POST).build())
            .requestMetadata(RequestMetadata.builder().clientIP(randomAlphabetic(15)).build())
            .build();
    AuditEvent auditEvent2 =
        getCommonBuilder()
            .authenticationInfo(
                AuthenticationInfo.builder()
                    .principal(
                        Principal.builder().type(PrincipalType.SYSTEM).identifier(PrincipalType.SYSTEM.name()).build())
                    .build())
            .httpRequestInfo(HttpRequestInfo.builder().requestMethod(REQUEST_METHOD_POST).build())
            .build();
    AuditEvent auditEvent3 =
        getCommonBuilder()
            .authenticationInfo(
                AuthenticationInfo.builder()
                    .principal(Principal.builder().type(PrincipalType.USER).identifier(randomAlphabetic(15)).build())
                    .labels(List.of(KeyValuePair.builder().key(USER_ID).value(randomAlphabetic(14)).build()))
                    .build())
            .build();
    return List.of(auditEvent1, auditEvent2, auditEvent3);
  }

  private AuditEventBuilder getCommonBuilder() {
    return AuditEvent.builder()
        .id(randomAlphabetic(10))
        .module(ModuleType.CORE)
        .resource(Resource.builder().type(ResourceType.CONNECTOR.name()).identifier(randomAlphabetic(10)).build())
        .resourceScope(ResourceScope.builder()
                           .accountIdentifier(randomAlphabetic(12))
                           .orgIdentifier(randomAlphabetic(12))
                           .projectIdentifier(randomAlphabetic(12))
                           .build())
        .action(Action.CREATE)
        .timestamp(Instant.now());
  }
}
