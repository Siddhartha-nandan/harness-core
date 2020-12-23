package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.outcomes.DependencyOutcome;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Slf4j
public class LiteEngineTaskStep implements TaskExecutable<LiteEngineTaskStepInfo> {
  public static final String TASK_TYPE_CI_BUILD = "CI_BUILD";
  public static final String LE_STATUS_TASK_TYPE = "CI_LE_STATUS";
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private CIDelegateTaskExecutor ciDelegateTaskExecutor;

  private static final String DEPENDENCY_OUTCOME = "dependencies";
  public static final StepType STEP_TYPE = LiteEngineTaskStepInfo.STEP_TYPE;

  @Override
  public Class<LiteEngineTaskStepInfo> getStepParametersClass() {
    return LiteEngineTaskStepInfo.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, StepInputPackage inputPackage) {
    addCallBackIds(stepParameters, ambiance);

    CIBuildSetupTaskParams buildSetupTaskParams = buildSetupUtils.getBuildSetupTaskParams(stepParameters, ambiance);
    log.info("Created params for build task: {}", buildSetupTaskParams);

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(stepParameters.getTimeout())
                                  .taskType(TASK_TYPE_CI_BUILD)
                                  .parameters(new Object[] {buildSetupTaskParams})
                                  .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, Map<String, ResponseData> responseDataMap) {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        (K8sTaskExecutionResponse) responseDataMap.values().iterator().next();

    DependencyOutcome dependencyOutcome =
        getDependencyOutcome(stepParameters, k8sTaskExecutionResponse.getK8sTaskResponse().getPodStatus());
    StepResponse.StepOutcome stepOutcome =
        StepResponse.StepOutcome.builder().name(DEPENDENCY_OUTCOME).outcome(dependencyOutcome).build();
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      log.info(
          "LiteEngineTaskStep pod creation task executed successfully with response [{}]", k8sTaskExecutionResponse);
      return StepResponse.builder().status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();

    } else {
      log.error("LiteEngineTaskStep execution finished with status [{}] and response [{}]",
          k8sTaskExecutionResponse.getCommandExecutionStatus(), k8sTaskExecutionResponse);
      return StepResponse.builder().status(Status.FAILED).stepOutcome(stepOutcome).build();
    }
  }

  private DependencyOutcome getDependencyOutcome(LiteEngineTaskStepInfo stepParameters, PodStatus podStatus) {
    List<ContainerDefinitionInfo> serviceContainers = buildSetupUtils.getBuildServiceContainers(stepParameters);
    List<ServiceDependency> serviceDependencyList = new ArrayList<>();
    if (serviceContainers == null) {
      return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
    }

    Map<String, CIContainerStatus> containerStatusMap = new HashMap<>();
    if (podStatus != null && podStatus.getCiContainerStatusList() != null) {
      for (CIContainerStatus containerStatus : podStatus.getCiContainerStatusList()) {
        containerStatusMap.put(containerStatus.getName(), containerStatus);
      }
    }

    for (ContainerDefinitionInfo serviceContainer : serviceContainers) {
      String containerName = serviceContainer.getName();
      if (containerStatusMap.containsKey(containerName)) {
        CIContainerStatus containerStatus = containerStatusMap.get(containerName);

        ServiceDependency.Status status = ServiceDependency.Status.SUCCESS;
        if (containerStatus.getStatus() == CIContainerStatus.Status.ERROR) {
          status = ServiceDependency.Status.ERROR;
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(containerStatus.getImage())
                                      .startTime(containerStatus.getStartTime())
                                      .endTime(containerStatus.getEndTime())
                                      .errorMessage(containerStatus.getErrorMsg())
                                      .status(status)
                                      .build());
      } else {
        ImageDetails imageDetails = serviceContainer.getContainerImageDetails().getImageDetails();
        String image = imageDetails.getName();
        if (isEmpty(imageDetails.getTag())) {
          image += String.format(":%s", imageDetails.getTag());
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(image)
                                      .errorMessage("Unknown")
                                      .status(ServiceDependency.Status.ERROR)
                                      .build());
      }
    }
    return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
  }

  private void addCallBackIds(LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    Map<String, String> taskIds = new HashMap<>();
    liteEngineTaskStepInfo.getExecutionElementConfig().getSteps().forEach(
        executionWrapper -> addCallBackId(executionWrapper, ambiance, taskIds));

    executionSweepingOutputResolver.consume(
        ambiance, CALLBACK_IDS, StepTaskDetails.builder().taskIds(taskIds).build(), StepOutcomeGroup.STAGE.name());
  }

  private void addCallBackId(ExecutionWrapperConfig executionWrapper, Ambiance ambiance, Map<String, String> taskIds) {
    final String accountId = ambiance.getSetupAbstractionsMap().get("accountId");

    if (executionWrapper != null) {
      if (!executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);

        setCallBackIdInStepInfo(ambiance, stepElementConfig, accountId, taskIds);
      } else if (!executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        parallelStepElementConfig.getSections().forEach(section -> addCallBackId(section, ambiance, taskIds));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }
  private void setCallBackIdInStepInfo(
      Ambiance ambiance, StepElementConfig stepElement, String accountId, Map<String, String> taskIds) {
    // TODO replace identifier as key in case two steps can have same identifier

    if (stepElement.getStepSpecType().getStepType().equals(RunStepInfo.STEP_TYPE)) {
      RunStepInfo runStepInfo = (RunStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, runStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      runStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }

    if (stepElement.getStepSpecType().getStepType().equals(PluginStep.STEP_TYPE)) {
      PluginStepInfo pluginStepInfo = (PluginStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, pluginStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      pluginStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }
    if (stepElement.getStepSpecType().getStepType().equals(PublishStep.STEP_TYPE)) {
      PublishStepInfo publishStepInfo = (PublishStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, publishStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      publishStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }

    if (stepElement.getStepSpecType().getStepType().equals(SaveCacheStep.STEP_TYPE)) {
      SaveCacheStepInfo saveCacheStepInfo = (SaveCacheStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, saveCacheStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      saveCacheStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }

    if (stepElement.getStepSpecType().getStepType().equals(RestoreCacheStep.STEP_TYPE)) {
      RestoreCacheStepInfo restoreCacheStepInfo = (RestoreCacheStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, restoreCacheStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      restoreCacheStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }

    if (stepElement.getStepSpecType().getStepType().equals(TestIntelligenceStep.STEP_TYPE)) {
      TestIntelligenceStepInfo runStepInfo = (TestIntelligenceStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, runStepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      runStepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }

    if (stepElement.getStepSpecType() instanceof PluginCompatibleStep) {
      PluginCompatibleStep stepInfo = (PluginCompatibleStep) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, stepInfo.getTimeout(), accountId, ciDelegateTaskExecutor);
      stepInfo.setCallbackId(taskId);
      taskIds.put(stepElement.getIdentifier(), taskId);
    }
  }

  private String queueDelegateTask(Ambiance ambiance, long timeout, String accountId, CIDelegateTaskExecutor executor) {
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .parked(true)
                                  .taskType(LE_STATUS_TASK_TYPE)
                                  .parameters(new Object[] {StepStatusTaskParameters.builder().build()})
                                  .timeout(timeout)
                                  .build();

    HDelegateTask task =
        (HDelegateTask) StepUtils.prepareDelegateTaskInput(accountId, taskData, ambiance.getSetupAbstractionsMap());

    return executor.queueTask(ambiance.getSetupAbstractionsMap(), task);
  }
}
