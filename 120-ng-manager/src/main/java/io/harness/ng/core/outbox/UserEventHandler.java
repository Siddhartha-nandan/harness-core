package io.harness.ng.core.outbox;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.beans.AuthenticationInfoDTO.fromSecurityPrincipal;
import static io.harness.ng.core.invites.remote.RoleBindingMapper.toAuditRoleBindings;
import static io.harness.ng.core.user.UserMembershipUpdateMechanism.ACCEPTED_INVITE;
import static io.harness.ng.core.user.UserMembershipUpdateMechanism.SYSTEM;
import static io.harness.remote.NGObjectMapperHelper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.beans.custom.user.UserInviteAuditEventData;
import io.harness.audit.beans.custom.user.UserMembershipAuditEventData;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.usermembership.UserMembershipDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.events.UserInviteCreateEvent;
import io.harness.ng.core.events.UserInviteDeleteEvent;
import io.harness.ng.core.events.UserInviteUpdateEvent;
import io.harness.ng.core.events.UserMembershipAddEvent;
import io.harness.ng.core.events.UserMembershipRemoveEvent;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.security.dto.ServicePrincipal;
import io.harness.utils.ScopeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class UserEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final Producer producer;
  private final AuditClientService auditClientService;

  @Inject
  public UserEventHandler(
      @Named(EventsFrameworkConstants.USERMEMBERSHIP) Producer producer, AuditClientService auditClientService) {
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    this.producer = producer;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case "UserInviteCreated":
          return handleUserInviteCreateEvent(outboxEvent);
        case "UserInviteUpdated":
          return handleUserInviteUpdateEvent(outboxEvent);
        case "UserInviteDeleted":
          return handleUserInviteDeleteEvent(outboxEvent);
        case "UserMembershipAdded":
          return handleUserMembershipAddedEvent(outboxEvent);
        case "UserMembershipRemoved":
          return handleUserMembershipRemovedEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (IOException exception) {
      return false;
    }
  }

  private boolean handleUserInviteCreateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteCreateEvent userInviteCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteCreateEvent.class);
    UserInviteAuditEventData auditEventData =
        new UserInviteAuditEventData(toAuditRoleBindings(userInviteCreateEvent.getInvite().getRoleBindings()));
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleUserInviteUpdateEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteUpdateEvent userInviteUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteUpdateEvent.class);
    UserInviteAuditEventData auditEventData =
        new UserInviteAuditEventData(toAuditRoleBindings(userInviteUpdateEvent.getNewInvite().getRoleBindings()));
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.RESEND_INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleUserInviteDeleteEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserInviteDeleteEvent userInviteDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserInviteDeleteEvent.class);
    UserInviteAuditEventData auditEventData =
        new UserInviteAuditEventData(toAuditRoleBindings(userInviteDeleteEvent.getInvite().getRoleBindings()));
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.REVOKE_INVITE)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleUserMembershipAddedEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserMembershipAddEvent userMembershipAddEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserMembershipAddEvent.class);
    UserMembershipAuditEventData auditEventData =
        new UserMembershipAuditEventData(userMembershipAddEvent.getMechanism().name());
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.ADD_MEMBERSHIP)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    boolean eventPublished = publishEvent(userMembershipAddEvent.getUserId(), userMembershipAddEvent.getScope(),
        EventsFrameworkMetadataConstants.CREATE_ACTION);
    boolean auditPublished;
    if (SYSTEM == userMembershipAddEvent.getMechanism() || ACCEPTED_INVITE == userMembershipAddEvent.getMechanism()) {
      auditPublished = auditClientService.publishAudit(
          auditEntry, fromSecurityPrincipal(new ServicePrincipal(NG_MANAGER.getServiceId())), globalContext);
    } else {
      auditPublished = auditClientService.publishAudit(auditEntry, globalContext);
    }
    return eventPublished && auditPublished;
  }

  private boolean handleUserMembershipRemovedEvent(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    UserMembershipRemoveEvent userMembershipRemoveEvent =
        objectMapper.readValue(outboxEvent.getEventData(), UserMembershipRemoveEvent.class);
    UserMembershipAuditEventData auditEventData =
        new UserMembershipAuditEventData(userMembershipRemoveEvent.getMechanism().name());
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.REMOVE_MEMBERSHIP)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .auditEventData(auditEventData)
                                .build();
    boolean eventPublished = publishEvent(userMembershipRemoveEvent.getUserId(), userMembershipRemoveEvent.getScope(),
        EventsFrameworkMetadataConstants.DELETE_ACTION);
    boolean auditPublished;
    if (SYSTEM == userMembershipRemoveEvent.getMechanism()
        || ACCEPTED_INVITE == userMembershipRemoveEvent.getMechanism()) {
      auditPublished = auditClientService.publishAudit(
          auditEntry, fromSecurityPrincipal(new ServicePrincipal(NG_MANAGER.getServiceId())), globalContext);
    } else {
      auditPublished = auditClientService.publishAudit(auditEntry, globalContext);
    }
    return eventPublished && auditPublished;
  }

  private boolean publishEvent(String userId, UserMembership.Scope scope, String action) {
    try {
      io.harness.eventsframework.schemas.usermembership.Scope.Builder scopeBuilder =
          io.harness.eventsframework.schemas.usermembership.Scope.newBuilder().setAccountIdentifier(
              StringValue.of(scope.getAccountIdentifier()));
      if (scope.getOrgIdentifier() != null) {
        scopeBuilder.setOrgIdentifier(StringValue.of(scope.getOrgIdentifier()));
      }
      if (scope.getProjectIdentifier() != null) {
        scopeBuilder.setProjectIdentifier(StringValue.of(scope.getProjectIdentifier()));
      }
      producer.send(Message.newBuilder()
                        .putAllMetadata(ImmutableMap.of("accountId", scope.getAccountIdentifier(),
                            EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkConstants.USERMEMBERSHIP,
                            EventsFrameworkMetadataConstants.ACTION, action))
                        .setData(UserMembershipDTO.newBuilder()
                                     .setAction(action)
                                     .setUserId(userId)
                                     .setScope(scopeBuilder.build())
                                     .build()
                                     .toByteString())
                        .build());
      return true;
    } catch (ProducerShutdownException e) {
      log.error("Failed to send event to events framework for {} on user {} and scope {}: ", action, userId,
          ScopeUtils.toString(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()), e);
      return false;
    }
  }
}
