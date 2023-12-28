/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.pluginstep;

import static io.harness.plancreator.steps.pluginstep.KubernetesInfraOutput.KUBERNETES_INFRA_OUTPUT;

import static java.lang.String.format;

import io.harness.beans.FeatureName;
import io.harness.common.ParameterFieldHelper;
import io.harness.delegate.ComputingResource;
import io.harness.delegate.ExecutionInfrastructure;
import io.harness.delegate.K8sInfraSpec;
import io.harness.delegate.LogConfig;
import io.harness.delegate.Secret;
import io.harness.delegate.Secrets;
import io.harness.delegate.StepSpec;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.scheduler.InitializeExecutionInfraResponse;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidArgumentsException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.TaskRequestsUtils;
import io.harness.steps.container.execution.ContainerStepRbacHelper;
import io.harness.steps.container.utils.ContainerSpecUtils;
import io.harness.steps.executable.TaskExecutableWithRbac;
import io.harness.steps.plugin.ContainerStepInfo;
import io.harness.steps.plugin.ContainerStepSpec;
import io.harness.steps.plugin.infrastructure.ContainerK8sInfra;
import io.harness.steps.plugin.infrastructure.ContainerStepInfra;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.InitialiseTaskUtils;
import io.harness.utils.PmsFeatureFlagService;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitKubernetesInfraContainerStep
    implements TaskExecutableWithRbac<StepElementParameters, InitializeExecutionInfraResponse> {
  public static final StepType STEP_TYPE = StepSpecTypeConstants.INIT_KUBERNETES_INFRA_CONTAINER_STEP_TYPE;
  private static final String DEFAULT_CONTAINER_RESOURCE_MEMORY = "100Mi";
  private static final String DEFAULT_CONTAINER_RESOURCE_CPU = "100m";

  @Inject private ContainerStepRbacHelper containerStepRbacHelper;
  @Inject private InitialiseTaskUtils initialiseTaskUtils;
  @Inject private PmsFeatureFlagService featureFlagService;

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    ContainerStepSpec stepParameter = (ContainerStepSpec) stepParameters.getSpec();
    containerStepRbacHelper.validateResources(stepParameter, ambiance);
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<InitializeExecutionInfraResponse> responseDataSupplier) throws Exception {
    InitializeExecutionInfraResponse k8sInfra = responseDataSupplier.get();
    PluginStepUtility.validateInitExecutionInfraResponseAndThrow(k8sInfra);
    // it's needed for pod cleanup
    KubernetesInfraOutput kubernetesInfraOutput =
        KubernetesInfraOutput.builder().infraRefId(k8sInfra.getInfraRefId()).build();
    executionSweepingOutputService.consume(
        ambiance, KUBERNETES_INFRA_OUTPUT, kubernetesInfraOutput, StepOutcomeGroup.STAGE.name());
    return StepResponse.builder()
        .status(PluginStepUtility.getStatus(k8sInfra))
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(KUBERNETES_INFRA_OUTPUT)
                         .outcome(kubernetesInfraOutput)
                         .group(StepCategory.STEP_GROUP.name())
                         .build())
        .build();
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    ContainerStepSpec containerStepSpec = (ContainerStepSpec) stepElementParameters.getSpec();
    final List<TaskSelector> delegateSelectors = getTaskSelectors(ambiance, containerStepSpec);

    ExecutionInfrastructure executionInfrastructure =
        buildExecutionInfrastructure(ambiance, List.of(stepElementParameters));
    long timeout =
        Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
    return TaskRequestsUtils.prepareInitTaskRequest(ambiance, executionInfrastructure, timeout,
        TaskCategory.DELEGATE_TASK_V2, true, delegateSelectors, Scope.PROJECT);
  }

  private ExecutionInfrastructure buildExecutionInfrastructure(
      Ambiance ambiance, List<StepElementParameters> stepElementParametersList) {
    // iterate over the steps and populate step specs. The values are hardcoded for POC
    List<StepSpec> stepSpecList = new ArrayList<>();
    for (StepElementParameters stepElementParameters : stepElementParametersList) {
      if (stepElementParameters.getSpec() instanceof ContainerStepInfo) {
        ContainerStepInfo containerStepInfo = (ContainerStepInfo) stepElementParameters.getSpec();
        if (containerStepInfo.getInfrastructure().getType() != ContainerStepInfra.Type.KUBERNETES_DIRECT) {
          throw new InvalidArgumentsException(format("Not supported infrastructure type for container step, %s",
              containerStepInfo.getInfrastructure().getType()));
        }
        String image = ParameterFieldHelper.getParameterFieldValue(containerStepInfo.getImage());
        ContainerK8sInfra infrastructure = (ContainerK8sInfra) containerStepInfo.getInfrastructure();
        Map<String, String> envVariables =
            ParameterFieldHelper.getParameterFieldValue(containerStepInfo.getEnvVariables());

        stepSpecList.add(
            StepSpec.newBuilder()
                .setImage(image)
                .putAllEnv(envVariables)
                .setStepId(containerStepInfo.getIdentifier())
                .setComputeResource(getComputingResource(infrastructure))
                .setSecrets(
                    Secrets.newBuilder()
                        .addSecrets(Secret.newBuilder().setScopeSecretIdentifier("org.shell_script_secret").build())
                        .build())
                .build());
      }
    }

    String logPrefix = initialiseTaskUtils.getLogPrefix(ambiance, "STEP");
    return ExecutionInfrastructure.newBuilder()
        .setLogConfig(LogConfig.newBuilder().setLogPrefix(logPrefix).build())
        .setK8S(K8sInfraSpec.newBuilder().addAllSteps(stepSpecList).build())
        .build();
  }

  private ComputingResource getComputingResource(ContainerK8sInfra infrastructure) {
    ContainerResource.Limits limits = infrastructure.getSpec().getResources().getLimits();
    if (limits != null) {
      String cpu = ParameterFieldHelper.getParameterFieldValue(limits.getCpu());
      String memory = ParameterFieldHelper.getParameterFieldValue(limits.getMemory());
      return ComputingResource.newBuilder().setCpu(cpu).setMemory(memory).build();
    }

    return ComputingResource.newBuilder()
        .setCpu(DEFAULT_CONTAINER_RESOURCE_CPU)
        .setMemory(DEFAULT_CONTAINER_RESOURCE_MEMORY)
        .build();
  }

  private List<TaskSelector> getTaskSelectors(Ambiance ambiance, ContainerStepSpec containerStepSpec) {
    return featureFlagService.isEnabled(
               AmbianceUtils.getAccountId(ambiance), FeatureName.CD_CONTAINER_STEP_DELEGATE_SELECTOR)
        ? ContainerSpecUtils.getStepDelegateSelectors(containerStepSpec)
        : new ArrayList<>();
  }

  // move to helper class
  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
