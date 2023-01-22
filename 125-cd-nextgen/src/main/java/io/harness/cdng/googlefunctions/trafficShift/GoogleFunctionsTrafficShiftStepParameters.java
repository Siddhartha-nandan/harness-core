package io.harness.cdng.googlefunctions.trafficShift;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.googlefunctions.GoogleFunctionsSpecParameters;
import io.harness.cdng.googlefunctions.deployWithoutTraffic.GoogleFunctionsDeployWithoutTrafficBaseStepInfo;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.googlefunctions.GoogleFunctionsCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.util.Arrays;
import java.util.List;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("googleFunctionsTrafficShiftStepParameters")
@RecasterAlias("io.harness.cdng.googlefunctions.trafficShift.GoogleFunctionsTrafficShiftStepParameters")
public class GoogleFunctionsTrafficShiftStepParameters extends GoogleFunctionsTrafficShiftBaseStepInfo
        implements GoogleFunctionsSpecParameters {
    @Builder(builderMethodName = "infoBuilder")
    public GoogleFunctionsTrafficShiftStepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                                             ParameterField<Integer> trafficPercent,
                                                     String googleFunctionDeployWithoutTrafficStepFnq) {
        super(delegateSelectors, trafficPercent, googleFunctionDeployWithoutTrafficStepFnq);
    }

    public List<String> getCommandUnits() {
        return Arrays.asList(GoogleFunctionsCommandUnitConstants.trafficShift.toString());
    }

}