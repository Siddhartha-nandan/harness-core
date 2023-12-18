/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.approval.step.harness.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder(builderMethodName = "infoBuilder")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarnessApprovalStepParameters implements SpecParameters {
  @NotNull ParameterField<String> message;
  ParameterField<String> callback_id;
  @NotNull ParameterField<Boolean> include_execution_history;
  AutoApprovalParams auto_approval;
  @NotNull Approvers approvers;
  List<ApproverInputInfo> inputs;
  ParameterField<Boolean> auto_reject;

  @Override
  public String getVersion() {
    return HarnessYamlVersion.V1;
  }
}
