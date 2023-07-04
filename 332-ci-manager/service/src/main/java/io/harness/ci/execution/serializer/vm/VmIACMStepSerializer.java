/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.serializer.vm;

import io.harness.beans.steps.stepinfo.IACMTerraformPluginInfo;
import io.harness.beans.sweepingoutputs.StageInfraDetails;
import io.harness.ci.buildstate.ConnectorUtils;
import io.harness.ci.execution.CIExecutionConfigService;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.ci.utils.HarnessImageUtils;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep.VmPluginStepBuilder;
import io.harness.iacm.execution.IACMStepsUtils;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class VmIACMStepSerializer {
  @Inject private CIExecutionConfigService ciExecutionConfigService;
  @Inject private HarnessImageUtils harnessImageUtils;
  @Inject private IACMStepsUtils iacmStepsUtils;
  @Inject private ConnectorUtils connectorUtils;

  public VmPluginStep serialize(Ambiance ambiance, IACMTerraformPluginInfo stepInfo,
      StageInfraDetails stageInfraDetails, ParameterField<Timeout> parameterFieldTimeout) {
    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, stepInfo.getDefaultTimeout());

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    String workspaceId = stepInfo.getWorkspace();
    String command = stepInfo.getCommand().getValue();
    Map<String, String> envVars = iacmStepsUtils.getIACMEnvVariables(ambiance, workspaceId, command);

    envVars = iacmStepsUtils.replaceHarnessVariables(ambiance, envVars);
    String image;
    if (stepInfo.getImage().getValue() != null) {
      image = stepInfo.getImage().getValue();
    } else {
      image = ciExecutionConfigService.getPluginVersionForVM(
          stepInfo.getNonYamlInfo().getStepInfoType(), ngAccess.getAccountIdentifier());
    }

    ConnectorDetails harnessInternalImageConnector =
        harnessImageUtils.getHarnessImageConnectorDetailsForVM(ngAccess, stageInfraDetails);

    VmPluginStepBuilder vmPluginStepBuilder =
        VmPluginStep.builder()
            .image(IntegrationStageUtils.getFullyQualifiedImageName(image, harnessInternalImageConnector))
            .envVariables(envVars)
            .timeoutSecs(timeout)
            .imageConnector(harnessInternalImageConnector);

    vmPluginStepBuilder.connector(iacmStepsUtils.retrieveIACMConnectorDetails(ambiance, stepInfo.getWorkspace()));

    return vmPluginStepBuilder.build();
  }
}