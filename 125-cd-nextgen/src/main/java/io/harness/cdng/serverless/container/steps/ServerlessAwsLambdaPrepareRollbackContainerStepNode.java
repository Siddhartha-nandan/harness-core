/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless.container.steps;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2)
@TypeAlias("serverlessAwsLambdaPrepareRollbackContainerStepNode")
@RecasterAlias("io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaPrepareRollbackContainerStepNode")
public class ServerlessAwsLambdaPrepareRollbackContainerStepNode extends CdAbstractStepNode {
  @JsonProperty("type")
  @NotNull
  ServerlessAwsLambdaPrepareRollbackContainerStepNode.StepType type = StepType.ServerlessAwsLambdaPrepareRollbackV2;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ServerlessAwsLambdaPrepareRollbackContainerStepInfo serverlessAwsLambdaPrepareRollbackContainerStepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return serverlessAwsLambdaPrepareRollbackContainerStepInfo;
  }

  enum StepType {
    ServerlessAwsLambdaPrepareRollbackV2(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
