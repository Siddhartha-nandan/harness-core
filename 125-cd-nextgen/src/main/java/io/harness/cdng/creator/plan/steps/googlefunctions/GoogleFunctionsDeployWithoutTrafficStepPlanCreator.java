package io.harness.cdng.creator.plan.steps.googlefunctions;

import com.google.common.collect.Sets;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.cdng.googlefunctions.deployWithoutTraffic.GoogleFunctionsDeployWithoutTrafficStepNode;
import io.harness.cdng.googlefunctions.deployWithoutTraffic.GoogleFunctionsDeployWithoutTrafficStepParameters;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class GoogleFunctionsDeployWithoutTrafficStepPlanCreator extends CDPMSStepPlanCreatorV2<GoogleFunctionsDeployWithoutTrafficStepNode> {
    @Override
    public Set<String> getSupportedStepTypes() {
        return Sets.newHashSet(StepSpecTypeConstants.GOOGLE_CLOUD_FUNCTIONS_DEPLOY_WITHOUT_TRAFFIC);
    }

    @Override
    public Class<GoogleFunctionsDeployWithoutTrafficStepNode> getFieldClass() {
        return GoogleFunctionsDeployWithoutTrafficStepNode.class;
    }

    @Override
    public PlanCreationResponse createPlanForField(PlanCreationContext ctx, GoogleFunctionsDeployWithoutTrafficStepNode stepElement) {
        return super.createPlanForField(ctx, stepElement);
    }

    @Override
    protected StepParameters getStepParameters(PlanCreationContext ctx, GoogleFunctionsDeployWithoutTrafficStepNode stepElement) {
        final StepParameters stepParameters = super.getStepParameters(ctx, stepElement);
        GoogleFunctionsDeployWithoutTrafficStepParameters googleFunctionsDeployWithoutTrafficStepParameters =
                (GoogleFunctionsDeployWithoutTrafficStepParameters) ((StepElementParameters) stepParameters).getSpec();
        googleFunctionsDeployWithoutTrafficStepParameters.setDelegateSelectors(
                stepElement.getGoogleFunctionsDeployWithoutTrafficStepInfo().getDelegateSelectors());
        googleFunctionsDeployWithoutTrafficStepParameters.setUpdateFieldMask(
                stepElement.getGoogleFunctionsDeployWithoutTrafficStepInfo().getUpdateFieldMask());
        return stepParameters;
    }
}
