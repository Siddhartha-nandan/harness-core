/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps.serverless;

import static io.harness.cdng.visitor.YamlTypes.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaDeployV2StepNode;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaDeployV2StepParameters;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class ServerlessAwsLambdaDeployV2StepPlanCreator
    extends CDPMSStepPlanCreatorV2<ServerlessAwsLambdaDeployV2StepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_DEPLOY_V2);
  }

  @Override
  public Class<ServerlessAwsLambdaDeployV2StepNode> getFieldClass() {
    return ServerlessAwsLambdaDeployV2StepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(
      PlanCreationContext ctx, ServerlessAwsLambdaDeployV2StepNode stepNode) {
    return super.createPlanForField(ctx, stepNode);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, ServerlessAwsLambdaDeployV2StepNode stepNode) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepNode);
    String packageStepFqn = getExecutionStepFqn(ctx.getCurrentField(), SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2);
    ServerlessAwsLambdaDeployV2StepParameters serverlessAwsLambdaDeployV2StepParameters =
        (ServerlessAwsLambdaDeployV2StepParameters) ((StepElementParameters) stepParameters).getSpec();
    serverlessAwsLambdaDeployV2StepParameters.setPackageStepFqn(packageStepFqn);
    serverlessAwsLambdaDeployV2StepParameters.setDelegateSelectors(
        stepNode.getServerlessAwsLambdaDeployV2StepInfo().getDelegateSelectors());
    return stepParameters;
  }
}
