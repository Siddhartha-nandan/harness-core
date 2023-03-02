/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.outbox;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.beans.custom.template.NodeExecutionEventData;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.engine.pms.audits.events.NodeExecutionOutboxEvents;
import io.harness.engine.pms.audits.events.PipelineStartEvent;
import io.harness.engine.pms.audits.events.StageStartEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.serializer.HObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeExecutionOutboxEventHandler implements OutboxEventHandler {
  private ObjectMapper objectMapper;
  private final AuditClientService auditClientService;

  @Inject
  public NodeExecutionOutboxEventHandler(AuditClientService auditClientService) {
    this.objectMapper = HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
  }

  private boolean handlePipelineStartEvent(OutboxEvent outboxEvent) throws JsonProcessingException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    PipelineStartEvent pipelineStartEvent =
        objectMapper.readValue(outboxEvent.getEventData(), PipelineStartEvent.class);
    NodeExecutionEventData nodeExecutionEventData = NodeExecutionEventData.builder()
                                                        .accountIdentifier(pipelineStartEvent.getAccountIdentifier())
                                                        .orgIdentifier(pipelineStartEvent.getOrgIdentifier())
                                                        .projectIdentifier(pipelineStartEvent.getProjectIdentifier())
                                                        .pipelineIdentifier(pipelineStartEvent.getPipelineIdentifier())
                                                        .planExecutionId(pipelineStartEvent.getPlanExecutionId())
                                                        .build();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.START)
                                .module(ModuleType.PMS)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .auditEventData(nodeExecutionEventData)
                                .insertId(outboxEvent.getId())
                                .build();

    return auditClientService.publishAudit(auditEntry, globalContext);
  }
  private boolean handlePipelineEndEvent(OutboxEvent outboxEvent) throws JsonProcessingException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.END)
                                .module(ModuleType.PMS)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();

    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  private boolean handleStageStartEvent(OutboxEvent outboxEvent) throws JsonProcessingException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    StageStartEvent stageStartEvent = objectMapper.readValue(outboxEvent.getEventData(), StageStartEvent.class);
    NodeExecutionEventData nodeExecutionEventData = NodeExecutionEventData.builder()
                                                        .accountIdentifier(stageStartEvent.getAccountIdentifier())
                                                        .orgIdentifier(stageStartEvent.getOrgIdentifier())
                                                        .projectIdentifier(stageStartEvent.getProjectIdentifier())
                                                        .pipelineIdentifier(stageStartEvent.getPipelineIdentifier())
                                                        .planExecutionId(stageStartEvent.getPlanExecutionId())
                                                        .nodeExecutionId(stageStartEvent.getNodeExecutionId())
                                                        .build();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.START)
                                .module(ModuleType.PMS)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .auditEventData(nodeExecutionEventData)
                                .insertId(outboxEvent.getId())
                                .build();

    return auditClientService.publishAudit(auditEntry, globalContext);
  }
  private boolean handleStageEndEvent(OutboxEvent outboxEvent) throws JsonProcessingException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.END)
                                .module(ModuleType.PMS)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();

    return auditClientService.publishAudit(auditEntry, globalContext);
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case NodeExecutionOutboxEvents.PIPELINE_START:
          return handlePipelineStartEvent(outboxEvent);
        case NodeExecutionOutboxEvents.PIPELINE_END:
          return handlePipelineEndEvent(outboxEvent);
        case NodeExecutionOutboxEvents.STAGE_START:
          return handleStageStartEvent(outboxEvent);
        case NodeExecutionOutboxEvents.STAGE_END:
          return handleStageEndEvent(outboxEvent);
        default:
          return false;
      }
    } catch (IOException ex) {
      log.error(String.format("Unexpected error occurred during handling of event", ex));
      return false;
    }
  }
}