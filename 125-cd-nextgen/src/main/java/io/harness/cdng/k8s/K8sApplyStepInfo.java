package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sApplyStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_APPLY)
@SimpleVisitorHelper(helperClass = K8sApplyStepInfoVisitorHelper.class)
@TypeAlias("k8sApplyStepInfo")
public class K8sApplyStepInfo extends K8sApplyBaseStepInfo implements CDStepInfo, Visitable {
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sApplyStepInfo(ParameterField<Boolean> skipDryRun, ParameterField<Boolean> skipSteadyStateCheck,
      ParameterField<List<String>> filePaths, ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(skipDryRun, skipSteadyStateCheck, filePaths, delegateSelectors);
  }

  @Override
  public StepType getStepType() {
    return K8sApplyStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return K8sApplyStepParameters.infoBuilder()
        .filePaths(this.getFilePaths())
        .skipDryRun(this.getSkipDryRun())
        .skipSteadyStateCheck(skipSteadyStateCheck)
        .delegateSelectors(delegateSelectors)
        .build();
  }
}
