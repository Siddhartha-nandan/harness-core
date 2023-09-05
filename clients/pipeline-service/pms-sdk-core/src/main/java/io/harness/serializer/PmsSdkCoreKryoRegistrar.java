/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
import io.harness.advisers.nextstep.NextStageAdviserParameters;
import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.advisers.pipelinerollback.OnFailPipelineRollbackParameters;
import io.harness.advisers.retry.RetryAdviserRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.OwnedBy;
import io.harness.async.AsyncResponseCallback;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviserParameters;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviserParameters;
import io.harness.pms.sdk.core.adviser.markFailure.OnMarkFailureAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.sdk.core.adviser.proceedwithdefault.ProceedWithDefaultAdviserParameters;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.execution.AsyncSdkProgressCallback;
import io.harness.pms.sdk.core.execution.AsyncSdkResumeCallback;
import io.harness.pms.sdk.core.execution.AsyncSdkSingleCallback;
import io.harness.pms.sdk.core.execution.AsyncTimeoutResponseData;
import io.harness.pms.sdk.core.execution.async.AsyncProgressData;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StatusNotifyResponseData;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.steps.matrix.StrategyMetadata;
import io.harness.steps.section.chain.SectionChainPassThroughData;
import io.harness.steps.section.chain.SectionChainStepParameters;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.ProceedWithDefaultValuesFailureActionConfig;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.abort.v1.AbortFailureConfigV1;
import io.harness.yaml.core.failurestrategy.ignore.IgnoreFailureActionConfig;
import io.harness.yaml.core.failurestrategy.ignore.v1.IgnoreFailureConfigV1;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualFailureSpecConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.OnTimeoutConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.v1.ManualFailureSpecConfigV1;
import io.harness.yaml.core.failurestrategy.manualintervention.v1.ManualInterventionFailureConfigV1;
import io.harness.yaml.core.failurestrategy.markFailure.MarkAsFailFailureActionConfig;
import io.harness.yaml.core.failurestrategy.markFailure.v1.MarkAsFailFailureConfigV1;
import io.harness.yaml.core.failurestrategy.marksuccess.MarkAsSuccessFailureActionConfig;
import io.harness.yaml.core.failurestrategy.marksuccess.v1.MarkAsSuccessFailureConfigV1;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetrySGFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.v1.RetryFailureConfigV1;
import io.harness.yaml.core.failurestrategy.retry.v1.RetryFailureSpecConfigV1;
import io.harness.yaml.core.failurestrategy.retry.v1.RetrySGFailureConfigV1;
import io.harness.yaml.core.failurestrategy.retry.v1.RetryStepGroupFailureSpecConfigV1;
import io.harness.yaml.core.failurestrategy.rollback.PipelineRollbackFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StageRollbackFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StepGroupFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.v1.PipelineRollbackFailureConfigV1;
import io.harness.yaml.core.failurestrategy.rollback.v1.StageRollbackFailureConfigV1;
import io.harness.yaml.core.failurestrategy.v1.NGFailureActionTypeV1;
import io.harness.yaml.core.failurestrategy.v1.NGFailureTypeV1;
import io.harness.yaml.core.failurestrategy.v1.OnConfigV1;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PIPELINE)
public class PmsSdkCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(StatusNotifyResponseData.class, 2502);
    kryo.register(StepOutcome.class, 2521);
    kryo.register(PassThroughData.class, 2535);
    kryo.register(StepResponseNotifyData.class, 2519);

    kryo.register(RetryAdviserParameters.class, 3103);
    kryo.register(OnSuccessAdviserParameters.class, 3104);
    kryo.register(OnFailAdviserParameters.class, 3105);
    kryo.register(IgnoreAdviserParameters.class, 3106);
    kryo.register(ManualInterventionAdviserParameters.class, 3107);
    kryo.register(OnMarkSuccessAdviserParameters.class, 3108);
    kryo.register(OnAbortAdviserParameters.class, 3109);
    kryo.register(OnMarkFailureAdviserParameters.class, 3110);

    // New classes here
    kryo.register(PlanNode.class, 88201);
    kryo.register(ExecutionSweepingOutput.class, 88202);
    kryo.register(AsyncSdkResumeCallback.class, 88204);
    kryo.register(AsyncSdkProgressCallback.class, 88205);
    kryo.register(AsyncSdkSingleCallback.class, 88206);
    kryo.register(AsyncResponseCallback.class, 88407);

    kryo.register(RetryAdviserRollbackParameters.class, 87801);
    kryo.register(RollbackStrategy.class, 87802);
    kryo.register(OnFailRollbackParameters.class, 87803);
    kryo.register(ManualInterventionAdviserRollbackParameters.class, 87804);
    kryo.register(NextStepAdviserParameters.class, 87805);

    kryo.register(ForkStepParameters.class, 3211);
    kryo.register(SectionChainStepParameters.class, 3214);
    kryo.register(SectionChainPassThroughData.class, 3217);
    kryo.register(StrategyMetadata.class, 878001);
    kryo.register(ProceedWithDefaultAdviserParameters.class, 878018);
    kryo.register(AsyncTimeoutResponseData.class, 878019);
    kryo.register(AsyncProgressData.class, 878020);
    kryo.register(OnFailPipelineRollbackParameters.class, 878021);
    kryo.register(NextStageAdviserParameters.class, 878022);
    kryo.register(FailureStrategyActionConfig.class, 878023);
    kryo.register(ManualInterventionFailureActionConfig.class, 878024);
    kryo.register(ManualFailureSpecConfig.class, 878025);
    kryo.register(OnTimeoutConfig.class, 878026);
    kryo.register(MarkAsSuccessFailureActionConfig.class, 878027);
    kryo.register(NGFailureActionType.class, 878028);
    kryo.register(MarkAsFailFailureActionConfig.class, 878029);
    kryo.register(AbortFailureActionConfig.class, 878030);
    kryo.register(IgnoreFailureActionConfig.class, 878031);
    kryo.register(RetryFailureActionConfig.class, 878032);
    kryo.register(StageRollbackFailureActionConfig.class, 878033);
    kryo.register(StepGroupFailureActionConfig.class, 878034);
    kryo.register(PipelineRollbackFailureActionConfig.class, 878035);
    kryo.register(ProceedWithDefaultValuesFailureActionConfig.class, 878036);
    kryo.register(RetrySGFailureActionConfig.class, 878037);
    kryo.register(OnConfigV1.class, 878038);
    kryo.register(IgnoreFailureConfigV1.class, 878039);
    kryo.register(AbortFailureConfigV1.class, 878040);
    kryo.register(RetryFailureConfigV1.class, 878041);
    kryo.register(ManualInterventionFailureConfigV1.class, 878042);
    kryo.register(RetrySGFailureConfigV1.class, 878043);
    kryo.register(MarkAsSuccessFailureConfigV1.class, 878044);
    kryo.register(MarkAsFailFailureConfigV1.class, 878045);
    kryo.register(StageRollbackFailureConfigV1.class, 878046);
    kryo.register(PipelineRollbackFailureConfigV1.class, 878047);
    kryo.register(NGFailureActionTypeV1.class, 878048);
    kryo.register(NGFailureTypeV1.class, 878053);
    kryo.register(ManualFailureSpecConfigV1.class, 878049);
    kryo.register(RetryFailureSpecConfigV1.class, 878050);
    kryo.register(RetryStepGroupFailureSpecConfigV1.class, 878051);
  }
}
