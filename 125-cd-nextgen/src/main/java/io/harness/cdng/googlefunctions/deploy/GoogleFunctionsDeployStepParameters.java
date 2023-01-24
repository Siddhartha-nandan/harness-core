package io.harness.cdng.googlefunctions.deploy;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.googlefunctions.GoogleFunctionsSpecParameters;
import io.harness.cdng.googlefunctions.deploy.GoogleFunctionsDeployBaseStepInfo;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("googleFunctionsDeployStepParameters")
@RecasterAlias("io.harness.cdng.googlefunctions.deploy.GoogleFunctionsDeployStepParameters")
public class GoogleFunctionsDeployStepParameters
    extends GoogleFunctionsDeployBaseStepInfo implements GoogleFunctionsSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public GoogleFunctionsDeployStepParameters(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, ParameterField<String> updateFieldMask) {
    super(delegateSelectors, updateFieldMask);
  }
}
