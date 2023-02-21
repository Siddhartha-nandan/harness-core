/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step;

import static io.harness.ngmigration.utils.MigratorUtility.RUNTIME_INPUT;

import io.harness.data.structure.EmptyPredicate;
import io.harness.ngmigration.beans.StepOutput;
import io.harness.ngmigration.beans.SupportStatus;
import io.harness.ngmigration.beans.WorkflowMigrationContext;
import io.harness.ngmigration.expressions.step.ServiceNowFunctor;
import io.harness.ngmigration.expressions.step.StepExpressionFunctor;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.servicenow.beans.ImportDataSpecType;
import io.harness.steps.servicenow.beans.ImportDataSpecWrapper;
import io.harness.steps.servicenow.beans.JsonImportDataSpec;
import io.harness.steps.servicenow.beans.ServiceNowField;
import io.harness.steps.servicenow.create.ServiceNowCreateStepInfo;
import io.harness.steps.servicenow.create.ServiceNowCreateStepNode;
import io.harness.steps.servicenow.importset.ServiceNowImportSetStepInfo;
import io.harness.steps.servicenow.importset.ServiceNowImportSetStepNode;
import io.harness.steps.servicenow.update.ServiceNowUpdateStepInfo;
import io.harness.steps.servicenow.update.ServiceNowUpdateStepNode;

import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.sm.State;
import software.wings.sm.states.collaboration.ServiceNowCreateUpdateState;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceNowStepMapperImpl extends StepMapper {
  @Override
  public SupportStatus stepSupportStatus(GraphNode graphNode) {
    return SupportStatus.SUPPORTED;
  }

  @Override
  public String getStepType(GraphNode stepYaml) {
    ServiceNowCreateUpdateState state = (ServiceNowCreateUpdateState) getState(stepYaml);
    ServiceNowCreateUpdateParams params = state.getServiceNowCreateUpdateParams();
    switch (params.getAction()) {
      case CREATE:
        return StepSpecTypeConstants.SERVICENOW_CREATE;
      case UPDATE:
        return StepSpecTypeConstants.SERVICENOW_UPDATE;
      case IMPORT_SET:
        return StepSpecTypeConstants.SERVICENOW_IMPORT_SET;
      default:
        throw new IllegalStateException("Unsupported service now action");
    }
  }

  @Override
  public State getState(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    ServiceNowCreateUpdateState state = new ServiceNowCreateUpdateState(stepYaml.getName());
    state.parseProperties(properties);
    return state;
  }

  @Override
  public AbstractStepNode getSpec(WorkflowMigrationContext context, GraphNode graphNode) {
    ServiceNowCreateUpdateState state = (ServiceNowCreateUpdateState) getState(graphNode);
    ServiceNowCreateUpdateParams params = state.getServiceNowCreateUpdateParams();
    switch (params.getAction()) {
      case CREATE:
        return buildCreate(state);
      case UPDATE:
        return buildUpdate(state);
      case IMPORT_SET:
        return buildImportSet(state);
      default:
        throw new IllegalStateException("Unsupported service now action");
    }
  }

  @Override
  public boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2) {
    // TODO
    return false;
  }

  @Override
  public List<StepExpressionFunctor> getExpressionFunctor(
      WorkflowMigrationContext context, WorkflowPhase phase, PhaseStep phaseStep, GraphNode graphNode) {
    String sweepingOutputName = getSweepingOutputName(graphNode);

    return Lists.newArrayList(String.format("context.%s", sweepingOutputName), String.format("%s", sweepingOutputName))
        .stream()
        .map(exp
            -> StepOutput.builder()
                   .stageIdentifier(MigratorUtility.generateIdentifier(phase.getName()))
                   .stepIdentifier(MigratorUtility.generateIdentifier(graphNode.getName()))
                   .stepGroupIdentifier(MigratorUtility.generateIdentifier(phaseStep.getName()))
                   .expression(exp)
                   .build())
        .map(ServiceNowFunctor::new)
        .collect(Collectors.toList());
  }

  private ServiceNowCreateStepNode buildCreate(ServiceNowCreateUpdateState state) {
    ServiceNowCreateUpdateParams params = state.getServiceNowCreateUpdateParams();
    ServiceNowCreateStepNode stepNode = new ServiceNowCreateStepNode();
    baseSetup(state, stepNode);
    ServiceNowCreateStepInfo stepInfo = ServiceNowCreateStepInfo.builder()
                                            .connectorRef(RUNTIME_INPUT)
                                            .ticketType(ParameterField.createValueField(params.getTicketType()))
                                            .templateName(RUNTIME_INPUT)
                                            .useServiceNowTemplate(ParameterField.createValueField(false))
                                            .delegateSelectors(ParameterField.createValueField(Collections.emptyList()))
                                            .fields(getFields(params))
                                            .build();

    stepNode.setServiceNowCreateStepInfo(stepInfo);
    return stepNode;
  }

  private static List<ServiceNowField> getFields(ServiceNowCreateUpdateParams parameters) {
    Map<ServiceNowFields, String> fields = parameters.fetchFields();
    Map<String, String> additional = parameters.fetchAdditionalFields();
    List<ServiceNowField> ngFields = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(fields)) {
      fields.forEach((key, value)
                         -> ngFields.add(ServiceNowField.builder()
                                             .name(key.getJsonBodyName())
                                             .value(ParameterField.createValueField(value))
                                             .build()));
    }

    if (EmptyPredicate.isNotEmpty(additional)) {
      additional.forEach(
          (key, value)
              -> ngFields.add(
                  ServiceNowField.builder().name(key).value(ParameterField.createValueField(value)).build()));
    }

    return ngFields;
  }

  private ServiceNowUpdateStepNode buildUpdate(ServiceNowCreateUpdateState state) {
    ServiceNowCreateUpdateParams params = state.getServiceNowCreateUpdateParams();
    ServiceNowUpdateStepNode stepNode = new ServiceNowUpdateStepNode();
    baseSetup(state, stepNode);
    ServiceNowUpdateStepInfo stepInfo = ServiceNowUpdateStepInfo.builder()
                                            .connectorRef(RUNTIME_INPUT)
                                            .delegateSelectors(ParameterField.createValueField(Collections.emptyList()))
                                            .ticketType(ParameterField.createValueField(params.getTicketType()))
                                            .ticketNumber(ParameterField.createValueField(params.getIssueNumber()))
                                            .templateName(RUNTIME_INPUT)
                                            .useServiceNowTemplate(ParameterField.createValueField(false))
                                            .fields(getFields(params))
                                            .build();
    stepNode.setServiceNowUpdateStepInfo(stepInfo);
    return stepNode;
  }

  private ServiceNowImportSetStepNode buildImportSet(ServiceNowCreateUpdateState state) {
    ServiceNowCreateUpdateParams params = state.getServiceNowCreateUpdateParams();
    ServiceNowImportSetStepNode stepNode = new ServiceNowImportSetStepNode();
    baseSetup(state, stepNode);
    ImportDataSpecWrapper importDataSpecWrapper = new ImportDataSpecWrapper();
    importDataSpecWrapper.setType(ImportDataSpecType.JSON);
    JsonImportDataSpec jsonImportDataSpec =
        JsonImportDataSpec.builder().jsonBody(ParameterField.createValueField(params.getJsonBody())).build();
    importDataSpecWrapper.setImportDataSpec(jsonImportDataSpec);
    ServiceNowImportSetStepInfo stepInfo =
        ServiceNowImportSetStepInfo.builder()
            .connectorRef(RUNTIME_INPUT)
            .delegateSelectors(ParameterField.createValueField(Collections.emptyList()))
            .stagingTableName(ParameterField.createValueField(params.getImportSetTableName()))
            .importData(importDataSpecWrapper)
            .build();
    stepNode.setServiceNowImportSetStepInfo(stepInfo);
    return stepNode;
  }
}
