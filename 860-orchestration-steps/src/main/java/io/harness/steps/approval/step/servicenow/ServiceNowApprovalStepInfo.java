package io.harness.steps.approval.step.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.servicenow.ServiceNowTicketTypeNG;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.ApprovalFacilitator;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName(StepSpecTypeConstants.SERVICENOW_APPROVAL)
@TypeAlias("serviceNowApprovalStepInfo")
@RecasterAlias("io.harness.steps.approval.step.servicenow.ServiceNowApprovalStepInfo")
public class ServiceNowApprovalStepInfo implements PMSStepInfo, WithConnectorRef {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> ticketNumber;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<ServiceNowTicketTypeNG> ticketType;
  CriteriaSpecWrapper rejectionCriteria;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @YamlSchemaTypes(value = {runtime})
  ParameterField<List<String>> delegateSelectors;

  @Override
  public StepType getStepType() {
    return ServiceNowApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return ApprovalFacilitator.APPROVAL_FACILITATOR;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return ServiceNowApprovalSpecParameters.builder()
        .connectorRef(connectorRef)
        .ticketNumber(ticketNumber)
        .ticketType(ParameterField.createValueField(ticketType.getValue().name()))
        .approvalCriteria(approvalCriteria)
        .rejectionCriteria(rejectionCriteria)
        .delegateSelectors(delegateSelectors)
        .build();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
