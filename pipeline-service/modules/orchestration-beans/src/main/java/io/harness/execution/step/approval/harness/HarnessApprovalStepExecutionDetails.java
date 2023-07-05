/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.step.approval.harness;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.execution.step.StepExecutionDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
@JsonTypeName("HarnessApproval")
@TypeAlias("HarnessApprovalStepExecutionDetails")
public class HarnessApprovalStepExecutionDetails implements StepExecutionDetails {
  List<HarnessApprovalActivity> approvalActivities;

  @Data
  @Builder
  public static class HarnessApprovalActivity {
    EmbeddedUser user;
    String approvalAction;
    Map<String, String> approverInputs;
    String comments;
    Date approvedAt;
  }
}
