package io.harness.cdng.creator.plan.steps.googlefunctions;

import static io.harness.cdng.visitor.YamlTypes.GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.cdng.googlefunctions.trafficShift.GoogleFunctionsTrafficShiftStepNode;
import io.harness.cdng.googlefunctions.trafficShift.GoogleFunctionsTrafficShiftStepParameters;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class GoogleFunctionsTrafficShiftStepPlanCreator
    extends CDPMSStepPlanCreatorV2<GoogleFunctionsTrafficShiftStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.GOOGLE_CLOUD_FUNCTIONS_TRAFFIC_SHIFT);
  }

  @Override
  public Class<GoogleFunctionsTrafficShiftStepNode> getFieldClass() {
    return GoogleFunctionsTrafficShiftStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(
      PlanCreationContext ctx, GoogleFunctionsTrafficShiftStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, GoogleFunctionsTrafficShiftStepNode stepElement) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepElement);

    String googleFunctionDeployWithoutTrafficStepFnq =
        getExecutionStepFqn(ctx.getCurrentField(), GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC);

    GoogleFunctionsTrafficShiftStepParameters googleFunctionsTrafficShiftStepParameters =
        (GoogleFunctionsTrafficShiftStepParameters) ((StepElementParameters) stepParameters).getSpec();
    googleFunctionsTrafficShiftStepParameters.setDelegateSelectors(
        stepElement.getGoogleFunctionsTrafficShiftStepInfo().getDelegateSelectors());
    googleFunctionsTrafficShiftStepParameters.setTrafficPercent(
        stepElement.getGoogleFunctionsTrafficShiftStepInfo().getTrafficPercent());
    googleFunctionsTrafficShiftStepParameters.setGoogleFunctionDeployWithoutTrafficStepFnq(
        googleFunctionDeployWithoutTrafficStepFnq);
    return stepParameters;
  }
}
