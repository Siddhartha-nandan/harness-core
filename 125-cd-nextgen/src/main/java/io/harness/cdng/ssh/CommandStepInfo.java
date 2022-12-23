/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.helpers.cdstepinfo.CommandStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.variables.NGVariable;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.COMMAND)
@SimpleVisitorHelper(helperClass = CommandStepInfoVisitorHelper.class)
@TypeAlias("commandStepInfo")
@RecasterAlias("io.harness.cdng.ssh.CommandStepInfo")
public class CommandStepInfo extends CommandBaseStepInfo implements Visitable {
  List<NGVariable> environmentVariables;
  @VariableExpression(skipVariableExpression = true) List<NGVariable> outputVariables;

  @Builder(builderMethodName = "infoBuilder")
  public CommandStepInfo(String uuid, ParameterField<Boolean> onDelegate,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, List<NGVariable> environmentVariables,
      List<NGVariable> outputVariables, List<CommandUnitWrapper> commandUnits, ParameterField<String> host) {
    super(uuid, onDelegate, delegateSelectors, commandUnits, host);
    this.environmentVariables = environmentVariables;
    this.outputVariables = outputVariables;
  }
}
