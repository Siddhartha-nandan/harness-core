/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.bash;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.shell.ScriptType.BASH;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.shell.ScriptType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ShellScriptTaskParametersNG implements TaskParameters, ExecutionCapabilityDemander {
  private final String commandUnit = "Execute";
  private final ScriptType scriptType = BASH;
  private final String executionId;
  private final List<String> outputVars;
  private final List<String> secretOutputVars;
  @Expression(ALLOW_SECRETS) private final String script;
  private final long sshTimeOut;
  private final String accountId;
  private final String appId;
  private final String workingDirectory;
  @Expression(ALLOW_SECRETS) private final Map<String, String> environmentVariables;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(final ExpressionEvaluator maskingEvaluator) {
    return new ArrayList<>();
  }
}
