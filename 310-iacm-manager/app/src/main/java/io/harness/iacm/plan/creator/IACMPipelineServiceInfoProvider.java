/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.iacm.plan.creator;

import static io.harness.beans.steps.StepSpecTypeConstants.PLUGIN;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.StepSpecTypeConstants;
import io.harness.ci.execution.creator.variables.ActionStepVariableCreator;
import io.harness.ci.execution.creator.variables.GitCloneStepVariableCreator;
import io.harness.ci.execution.creator.variables.PluginStepVariableCreator;
import io.harness.ci.execution.creator.variables.RunStepVariableCreator;
import io.harness.ci.execution.plan.creator.steps.CIStepsPlanCreator;
import io.harness.ci.plancreator.ActionStepPlanCreator;
import io.harness.ci.plancreator.PluginStepPlanCreator;
import io.harness.ci.plancreator.RunStepPlanCreator;
import io.harness.ci.plancreator.V1.ActionStepPlanCreatorV1;
import io.harness.ci.plancreator.V1.BackgroundStepPlanCreatorV1;
import io.harness.ci.plancreator.V1.GitClonePlanCreator;
import io.harness.ci.plancreator.V1.RunStepPlanCreatorV1;
import io.harness.ci.plancreator.V1.TestStepPlanCreator;
import io.harness.filters.ExecutionPMSFilterJsonCreator;
import io.harness.iacm.IACMStepType;
import io.harness.iacm.creator.variables.IACMStageVariableCreator;
import io.harness.iacm.creator.variables.IACMStepVariableCreator;
import io.harness.iacm.plan.creator.filter.IACMStageFilterJsonCreator;
import io.harness.iacm.plan.creator.stage.IACMStagePMSPlanCreator;
import io.harness.iacm.plan.creator.stage.IACMStagePMSPlanCreatorV1;
import io.harness.iacm.plan.creator.step.IACMApprovalStepVariableCreator;
import io.harness.iacm.plan.creator.step.IACMPMSStepFilterJsonCreator;
import io.harness.iacm.plan.creator.step.IACMPMSStepFilterJsonCreatorV1;
import io.harness.iacm.plan.creator.step.IACMPluginStepPlanCretorV1;
import io.harness.iacm.plan.creator.step.IACMStepFilterJsonCreatorV2;
import io.harness.iacm.plan.creator.step.IACMStepPlanCreator;
import io.harness.iacm.plan.creator.step.IACMTerraformPluginStepVariableCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.EmptyVariableCreator;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(HarnessTeam.IACM)
public class IACMPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  @Inject InjectorUtils injectorUtils;

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new LinkedList<>();
    planCreators.add(new IACMStagePMSPlanCreator()); // Creation of the Stage execution plan
    planCreators.add(new IACMStepPlanCreator()); // Supported steps for the Stage
    planCreators.add(new PluginStepPlanCreator()); // Plugin step
    planCreators.add(new GitClonePlanCreator());
    planCreators.add(new RunStepPlanCreator());
    planCreators.add(new ActionStepPlanCreator()); // Add GithubAction step
    planCreators.addAll(
        Arrays.stream(IACMStepType.values()).map(IACMStepType::getPlanCreator).collect(Collectors.toList()));

    // Add V1 Plan creators
    planCreators.add(new IACMStagePMSPlanCreatorV1());
    planCreators.add(new RunStepPlanCreatorV1());
    planCreators.add(new IACMPluginStepPlanCretorV1());
    planCreators.add(new TestStepPlanCreator());
    planCreators.add(new BackgroundStepPlanCreatorV1());
    planCreators.add(new ActionStepPlanCreatorV1());
    planCreators.add(new CIStepsPlanCreator());

    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new IACMStageFilterJsonCreator()); // Filter for the Stage
    filterJsonCreators.add(new IACMPMSStepFilterJsonCreator()); // Filter for the supported steps in the stage v0
    filterJsonCreators.add(new IACMStepFilterJsonCreatorV2());
    filterJsonCreators.add(new ExecutionPMSFilterJsonCreator()); // Filter for the Execution step

    // V1 Filters
    filterJsonCreators.add(new IACMPMSStepFilterJsonCreatorV1()); // Filter for the supported steps in the stage v1

    injectorUtils.injectMembers(filterJsonCreators);

    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new IACMStageVariableCreator()); // Variable creator for the stage
    variableCreators.add(new IACMStepVariableCreator()); // V1 step variable creator for external steps
    variableCreators.add(new PluginStepVariableCreator()); // variable creator for the plugin step
    variableCreators.add(new GitCloneStepVariableCreator());
    variableCreators.add(new IACMTerraformPluginStepVariableCreator());
    variableCreators.add(new IACMApprovalStepVariableCreator());
    variableCreators.add(new RunStepVariableCreator());
    variableCreators.add(new ActionStepVariableCreator()); // variable creator for the action step
    variableCreators.add(new EmptyVariableCreator(STEP, Set.of(LITE_ENGINE_TASK)));

    return variableCreators;
  }

  private StepInfo createStepInfo(IACMStepType iacmStepType, String stepCategory) {
    return StepInfo.newBuilder()
        .setName(iacmStepType.getDisplayName())
        .setType(iacmStepType.getName())
        .setFeatureFlag(iacmStepType.getFeatureName().name())
        .setStepMetaData(StepMetaData.newBuilder().addFolderPaths(stepCategory).build())
        .build();
  }

  /**
   * Does this method requires to have all the steps supported as _steps_ and not as stage steps?
   *
   */
  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo pluginStepInfo = StepInfo.newBuilder()
                                  .setName("Plugin")
                                  .setType(PLUGIN)
                                  .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("Build").build())
                                  .build();
    StepInfo runStepInfo = StepInfo.newBuilder()
                               .setName("Run")
                               .setType(StepSpecTypeConstants.RUN)
                               .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("Build").build())
                               .build();
    StepInfo actionStepInfo = StepInfo.newBuilder()
                                  .setName("Github Action plugin")
                                  .setType(StepSpecTypeConstants.ACTION)
                                  .setStepMetaData(StepMetaData.newBuilder().addFolderPaths("Build").build())
                                  .build();
    StepInfo gitCloneStepInfo =
        StepInfo.newBuilder()
            .setName("Git Clone")
            .setType(StepSpecTypeConstants.GIT_CLONE)
            .setStepMetaData(StepMetaData.newBuilder().addCategory(PLUGIN).addFolderPaths("Build").build())
            .build();
    List<StepInfo> stepInfos = new ArrayList<>();

    stepInfos.add(pluginStepInfo);
    stepInfos.add(runStepInfo);
    stepInfos.add(actionStepInfo);
    stepInfos.add(gitCloneStepInfo);
    Arrays.asList(IACMStepType.values())
        .forEach(e -> e.getStepCategories().forEach(category -> stepInfos.add(createStepInfo(e, category))));
    return stepInfos;
  }
}
