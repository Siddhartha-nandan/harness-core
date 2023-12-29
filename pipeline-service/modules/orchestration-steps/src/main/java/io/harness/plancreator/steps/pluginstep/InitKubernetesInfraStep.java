/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps.pluginstep;

import static io.harness.container.output.KubernetesInfraOutput.KUBERNETES_INFRA_OUTPUT;
import static io.harness.container.utils.DelegateInitTaskUtils.getComputingResource;
import static io.harness.container.utils.DelegateInitTaskUtils.getEnvVariables;
import static io.harness.container.utils.DelegateInitTaskUtils.getImage;
import static io.harness.container.utils.DelegateInitTaskUtils.getSecrets;
import static io.harness.container.utils.DelegateResponseUtils.getDelegateResponseDataAndThrow;
import static io.harness.container.utils.DelegateResponseUtils.getErrorNotifyResponseData;
import static io.harness.container.utils.DelegateResponseUtils.getStatus;
import static io.harness.container.utils.DelegateResponseUtils.prepareFailureResponse;
import static io.harness.container.utils.DelegateResponseUtils.validateInitExecutionInfraResponseAndThrow;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import io.harness.beans.FeatureName;
import io.harness.beans.yaml.extended.infrastrucutre.k8.Capabilities;
import io.harness.common.ParameterFieldHelper;
import io.harness.container.execution.BijouDelegateTaskExecutor;
import io.harness.container.output.KubernetesInfraOutput;
import io.harness.container.output.KubernetesInfraOutputService;
import io.harness.container.utils.DelegateInitTaskUtils;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.ExecutionInfrastructure;
import io.harness.delegate.Secret;
import io.harness.delegate.Secrets;
import io.harness.delegate.SecurityContext;
import io.harness.delegate.StepSpec;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.scheduler.InitializeExecutionInfraResponse;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidArgumentsException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.execution.invokers.StrategyHelper;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.container.utils.ContainerSpecUtils;
import io.harness.steps.executable.AsyncExecutableWithRbac;
import io.harness.steps.plugin.ContainerStepInfo;
import io.harness.steps.plugin.ContainerStepSpec;
import io.harness.steps.plugin.infrastructure.ContainerInfraYamlSpec;
import io.harness.steps.plugin.infrastructure.ContainerK8sInfra;
import io.harness.steps.plugin.infrastructure.ContainerStepInfra;
import io.harness.tasks.ResponseData;
import io.harness.utils.InitialiseTaskUtils;
import io.harness.utils.PmsFeatureFlagService;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class InitKubernetesInfraStep implements AsyncExecutableWithRbac<StepElementParameters> {
  public static final StepType STEP_TYPE = StepSpecTypeConstants.INIT_KUBERNETES_INFRA_STEP_TYPE;

  @Inject private StrategyHelper strategyHelper;
  @Inject private InitialiseTaskUtils initialiseTaskUtils;
  @Inject private PmsFeatureFlagService featureFlagService;
  @Inject private BijouDelegateTaskExecutor bijouDelegateTaskExecutor;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private KubernetesInfraOutputService kubernetesInfraOutputService;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    try (PmsSecurityContextEventGuard ignored = new PmsSecurityContextEventGuard(ambiance)) {
      return handleAsyncResponseInternal(ambiance, responseDataMap);
    } catch (Exception ex) {
      return prepareFailureResponse(ex);
    }
  }

  private StepResponse handleAsyncResponseInternal(Ambiance ambiance, Map<String, ResponseData> responseDataMap) {
    final List<ErrorNotifyResponseData> failedResponses = getErrorNotifyResponseData(responseDataMap);

    if (isNotEmpty(failedResponses)) {
      log.error("Error notify response found for Infrastructure step " + failedResponses);
      return strategyHelper.handleException(failedResponses.get(0).getException());
    }

    DelegateResponseData delegateResponseData = getDelegateResponseDataAndThrow(responseDataMap);
    if (!(delegateResponseData instanceof InitializeExecutionInfraResponse)) {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(EngineExceptionUtils.transformResponseMessagesToFailureInfo(List.of(
              ResponseMessage.builder().message("Unable to parse init k8s infrastructure delegate response").build())))
          .build();
    }

    InitializeExecutionInfraResponse k8sInfra = (InitializeExecutionInfraResponse) delegateResponseData;
    validateInitExecutionInfraResponseAndThrow(k8sInfra);

    kubernetesInfraOutputService.saveK8sInfra(ambiance, k8sInfra.getInfraRefId());
    return StepResponse.builder()
        .status(getStatus(k8sInfra))
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(KUBERNETES_INFRA_OUTPUT)
                         .outcome(KubernetesInfraOutput.builder().infraRefId(k8sInfra.getInfraRefId()).build())
                         .group(StepCategory.STEP_GROUP.name())
                         .build())
        .build();
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {}

  @Override
  public AsyncExecutableResponse executeAsyncAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    ContainerStepSpec containerStepSpec = (ContainerStepSpec) stepParameters.getSpec();
    ContainerStepInfo containerStepInfo = (ContainerStepInfo) containerStepSpec;
    if (containerStepInfo.getInfrastructure().getType() != ContainerStepInfra.Type.KUBERNETES_DIRECT) {
      throw new InvalidArgumentsException(format(
          "Not supported infrastructure type for container step, %s", containerStepInfo.getInfrastructure().getType()));
    }

    final List<TaskSelector> delegateSelectors = getTaskSelectors(ambiance, containerStepSpec);
    String initStepLogPrefix = initialiseTaskUtils.getLogPrefix(ambiance, "STEP");
    long timeout = Timeout.fromString((String) stepParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
    io.harness.beans.Scope scope = io.harness.beans.Scope.of(AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
    // this logic should be moved to step group hence converting to list of step params
    List<StepSpec> stepSpecList = getStepSpecList(List.of(stepParameters));
    SecurityContext podSecurityContext =
        getSecurityContext(((ContainerK8sInfra) containerStepInfo.getInfrastructure()).getSpec());

    ExecutionInfrastructure executionInfrastructure =
        DelegateInitTaskUtils.buildExecutionInfrastructure(stepSpecList, initStepLogPrefix, podSecurityContext);
    String queueInitTaskId = bijouDelegateTaskExecutor.queueInitTask(
        scope, executionInfrastructure, delegateSelectors, Duration.ofMillis(timeout));

    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(queueInitTaskId)
        .addAllLogKeys(CollectionUtils.emptyIfNull(singletonList(initStepLogPrefix)))
        .build();
  }

  @NotNull
  private List<StepSpec> getStepSpecList(List<StepElementParameters> stepElementParametersList) {
    List<StepSpec> stepSpecList = new ArrayList<>();
    for (StepElementParameters stepElementParameters : stepElementParametersList) {
      if (stepElementParameters.getSpec() instanceof ContainerStepInfo) {
        ContainerStepInfo containerStepInfo = (ContainerStepInfo) stepElementParameters.getSpec();
        ContainerInfraYamlSpec infrastructure = ((ContainerK8sInfra) containerStepInfo.getInfrastructure()).getSpec();
        StepSpec.Builder stepSpecBuilder = StepSpec.newBuilder();

        stepSpecBuilder.setImage(getImage(containerStepInfo.getImage()))
            .setStepId(containerStepInfo.getIdentifier())
            .putAllEnv(getEnvVariables(containerStepInfo.getEnvVariables()))
            .setComputeResource(getComputingResource(infrastructure.getResources()));

        List<Secret> secrets = getSecrets(List.of("org.shell_script_secret"));
        if (isNotEmpty(secrets)) {
          stepSpecBuilder.setSecrets(Secrets.newBuilder().addAllSecrets(secrets).build());
        }

        SecurityContext securityContext = getSecurityContext(infrastructure);
        if (securityContext != null) {
          stepSpecBuilder.setSecurityContext(securityContext);
        }

        stepSpecList.add(stepSpecBuilder.build());
      }
    }

    return stepSpecList;
  }

  private List<TaskSelector> getTaskSelectors(Ambiance ambiance, ContainerStepSpec containerStepSpec) {
    return featureFlagService.isEnabled(
               AmbianceUtils.getAccountId(ambiance), FeatureName.CD_CONTAINER_STEP_DELEGATE_SELECTOR)
        ? ContainerSpecUtils.getStepDelegateSelectors(containerStepSpec)
        : new ArrayList<>();
  }

  private SecurityContext getSecurityContext(ContainerInfraYamlSpec infrastructure) {
    if (infrastructure == null) {
      return null;
    }
    ParameterField<io.harness.beans.yaml.extended.infrastrucutre.k8.SecurityContext> securityContext =
        infrastructure.getContainerSecurityContext();
    if (securityContext == null) {
      return null;
    }
    io.harness.beans.yaml.extended.infrastrucutre.k8.SecurityContext context =
        ParameterFieldHelper.getParameterFieldValue(securityContext);
    if (context == null) {
      return null;
    }

    SecurityContext.Builder securityContextBuilder = SecurityContext.newBuilder();

    Boolean allowPrivilegeEscalation =
        ParameterFieldHelper.getParameterFieldValue(context.getAllowPrivilegeEscalation());
    if (allowPrivilegeEscalation != null) {
      securityContextBuilder.setAllowPrivilegeEscalation(allowPrivilegeEscalation);
    }
    Boolean privileged = ParameterFieldHelper.getParameterFieldValue(context.getPrivileged());
    if (privileged != null) {
      securityContextBuilder.setPrivileged(privileged);
    }
    String procMount = ParameterFieldHelper.getParameterFieldValue(context.getProcMount());
    if (isNotEmpty(procMount)) {
      securityContextBuilder.setProcMount(procMount);
    }
    Integer runAsGroup = ParameterFieldHelper.getParameterFieldValue(context.getRunAsGroup());
    if (runAsGroup != null) {
      securityContextBuilder.setRunAsGroup(runAsGroup);
    }
    Boolean readOnlyRootFilesystem = ParameterFieldHelper.getParameterFieldValue(context.getReadOnlyRootFilesystem());
    if (readOnlyRootFilesystem != null) {
      securityContextBuilder.setReadOnlyRootFilesystem(readOnlyRootFilesystem);
    }
    Boolean runAsNonRoot = ParameterFieldHelper.getParameterFieldValue(context.getRunAsNonRoot());
    if (runAsNonRoot != null) {
      securityContextBuilder.setRunAsNonRoot(runAsNonRoot);
    }
    Integer runAsUser = ParameterFieldHelper.getParameterFieldValue(context.getRunAsUser());
    if (runAsUser != null) {
      securityContextBuilder.setRunAsUser(runAsUser);
    }
    Capabilities capabilities = ParameterFieldHelper.getParameterFieldValue(context.getCapabilities());
    if (capabilities != null) {
      List<String> addCapabilities = ParameterFieldHelper.getParameterFieldValue(capabilities.getAdd());
      if (isNotEmpty(addCapabilities)) {
        securityContextBuilder.addAllAddCapability(addCapabilities);
      }
      List<String> dropCapabilities = ParameterFieldHelper.getParameterFieldValue(capabilities.getDrop());
      if (isNotEmpty(dropCapabilities)) {
        securityContextBuilder.addAllDropCapability(dropCapabilities);
      }
    }

    return securityContextBuilder.build();
  }
}
