/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.servicenow;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_APPROVALS})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(CDC)
@JsonTypeName("ServiceNowTicketOutcome")
@TypeAlias("serviceNowTicketOutcome")
@RecasterAlias("io.harness.steps.servicenow.ServiceNowTicketOutcome")
public class ServiceNowTicketOutcome implements Outcome {
  @Data
  @Builder
  public static class MultipleOutcome {
    String ticketUrl;
    String ticketNumber;
  }
  String ticketUrl;
  String ticketNumber;

  Map<String, String> fields;
  List<MultipleOutcome> multipleOutcomeList;

  String message;
}
