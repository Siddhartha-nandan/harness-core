/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.sam;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.helper.SerializedResponseDataHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plugin.AbstractContainerStepV2;
import io.harness.pms.sdk.core.plugin.ContainerUnitStepUtils;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class AwsSamDeployStep extends AbstractContainerStepV2<StepElementParameters> {
  @Inject Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Inject AwsSamStepHelper awsSamStepHelper;

  @Inject private InstanceInfoService instanceInfoService;

  @Inject private SerializedResponseDataHelper serializedResponseDataHelper;

  @Inject private KryoSerializer referenceFalseKryoSerializer;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.AWS_SAM_DEPLOY.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public long getTimeout(Ambiance ambiance, StepElementParameters stepElementParameters) {
    return Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
  }

  @Override
  public UnitStep getSerialisedStep(Ambiance ambiance, StepElementParameters stepElementParameters, String accountId,
      String logKey, long timeout, String parkedTaskId) {
    AwsSamDeployStepParameters awsSamDeployStepParameters =
        (AwsSamDeployStepParameters) stepElementParameters.getSpec();

    // Check if image exists
    awsSamStepHelper.verifyPluginImageIsProvider(awsSamDeployStepParameters.getImage());

    Map<String, String> samDeployEnvironmentVariablesMap = new HashMap<>();

    awsSamStepHelper.putK8sServiceAccountEnvVars(ambiance, samDeployEnvironmentVariablesMap);

    return ContainerUnitStepUtils.serializeStepWithStepParameters(
        getPort(ambiance, stepElementParameters.getIdentifier()), parkedTaskId, logKey,
        stepElementParameters.getIdentifier(), getTimeout(ambiance, stepElementParameters), accountId,
        stepElementParameters.getName(), delegateCallbackTokenSupplier, ambiance, samDeployEnvironmentVariablesMap,
        awsSamDeployStepParameters.getImage().getValue(), Collections.EMPTY_LIST);
  }

  @Override
  public StepResponse.StepOutcome getAnyOutComeForStep(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    // If any of the responses are in serialized format, deserialize them
    for (Map.Entry<String, ResponseData> entry : responseDataMap.entrySet()) {
      log.info(String.format("AWS SAM Deploy: ResponseData with class %s received", entry.getValue().getClass()));
      entry.setValue(serializedResponseDataHelper.deserialize(entry.getValue()));
      if (entry.getValue() instanceof BinaryResponseData) {
        entry.setValue((ResponseData) referenceFalseKryoSerializer.asInflatedObject(
            ((BinaryResponseData) entry.getValue()).getData()));
      }
    }

    StepResponse.StepOutcome stepOutcome = null;

    List<ServerInstanceInfo> serverInstanceInfoList =
        awsSamStepHelper.fetchServerInstanceInfoFromDelegateResponse(responseDataMap);

    if (serverInstanceInfoList != null) {
      InfrastructureOutcome infrastructureOutcome = awsSamStepHelper.getInfrastructureOutcome(ambiance);

      awsSamStepHelper.updateServerInstanceInfoList(serverInstanceInfoList, infrastructureOutcome);
      log.info("Saving AWS SAM server instances into sweeping output");
      stepOutcome = instanceInfoService.saveServerInstancesIntoSweepingOutput(ambiance, serverInstanceInfoList);
    } else {
      log.info("No instances were received in SAM Deploy Response");
    }

    return stepOutcome;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // we need to check if rbac check is req or not.
  }
}
