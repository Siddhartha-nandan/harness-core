/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.cet.cetqualitygatestep;

import io.harness.cdng.executables.CdTaskExecutable;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.v1.StepBaseParameters;
import io.harness.supplier.ThrowingSupplier;


public class CETQualityGateStep extends CdTaskExecutable<ArtifactTaskResponse> {
    public static final StepType STEP_TYPE =
            StepType.newBuilder().setType(StepSpecTypeConstants.CET_QUALITY_GATE).setStepCategory(StepCategory.STEP).build();
    public String COMMAND_UNIT = "Execute";

    @Override
    public StepResponse handleTaskResultWithSecurityContextAndNodeInfo(Ambiance ambiance,
                                                                       StepBaseParameters stepParameters, ThrowingSupplier<ArtifactTaskResponse> responseDataSupplier) throws Exception {
        try {
            return new StepResponse();
        } finally {

        }
    }

    @Override
    public TaskRequest obtainTaskAfterRbac(
            Ambiance ambiance, StepBaseParameters stepParameters, StepInputPackage inputPackage) {
        return new TaskRequest();
    }

    @Override
    public void validateResources(Ambiance ambiance, StepBaseParameters stepParameters) {}

    @Override
    public Class<StepBaseParameters> getStepParametersClass() {
        return StepBaseParameters.class;
    }

}
