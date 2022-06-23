/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSwapSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;

import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureWebAppSlotSwapTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSwapSlotsParameters slotSwapParameters = (AzureWebAppSwapSlotsParameters) azureAppServiceTaskParameters;
    validateSlotSwapParameters(slotSwapParameters);

    AzureWebClientContext webClientContext = buildAzureWebClientContext(slotSwapParameters, azureConfig);

    swapSlots(slotSwapParameters, webClientContext, logStreamingTaskClient);

    markDeploymentStatusAsSuccess(azureAppServiceTaskParameters, logStreamingTaskClient);
    return AzureWebAppSwapSlotsResponse.builder().preDeploymentData(slotSwapParameters.getPreDeploymentData()).build();
  }

  private void validateSlotSwapParameters(AzureWebAppSwapSlotsParameters slotSwapParameters) {
    String webAppName = slotSwapParameters.getAppName();
    String sourceSlot = slotSwapParameters.getSourceSlotName();
    String targetSlot = slotSwapParameters.getTargetSlotName();
    azureAppServiceResourceUtilities.validateSlotSwapParameters(webAppName, sourceSlot, targetSlot);
  }

  private void swapSlots(AzureWebAppSwapSlotsParameters slotSwapParameters, AzureWebClientContext webClientContext,
      ILogStreamingTaskClient logStreamingTaskClient) {
    String targetSlotName = slotSwapParameters.getTargetSlotName();
    Integer timeoutIntervalInMin = slotSwapParameters.getTimeoutIntervalInMin();

    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = toAzureAppServiceDeploymentContext(
        slotSwapParameters, webClientContext, timeoutIntervalInMin, logStreamingTaskClient);

    slotSwapParameters.getPreDeploymentData().setDeploymentProgressMarker(
        AppServiceDeploymentProgress.SWAP_SLOT.name());
    azureAppServiceDeploymentService.swapSlotsUsingCallback(
        azureAppServiceDeploymentContext, targetSlotName, logCallbackProviderFactory.createCg(logStreamingTaskClient));
    slotSwapParameters.getPreDeploymentData().setDeploymentProgressMarker(
        AppServiceDeploymentProgress.DEPLOYMENT_COMPLETE.name());
  }

  private AzureAppServiceDeploymentContext toAzureAppServiceDeploymentContext(
      AzureWebAppSwapSlotsParameters slotSwapParameters, AzureWebClientContext azureWebClientContext,
      Integer steadyTimeoutIntervalInMin, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = new AzureAppServiceDeploymentContext();
    azureAppServiceDeploymentContext.setAzureWebClientContext(azureWebClientContext);
    azureAppServiceDeploymentContext.setLogCallbackProvider(
        logCallbackProviderFactory.createCg(logStreamingTaskClient));
    azureAppServiceDeploymentContext.setSlotName(slotSwapParameters.getSourceSlotName());
    azureAppServiceDeploymentContext.setSteadyStateTimeoutInMin(steadyTimeoutIntervalInMin);
    return azureAppServiceDeploymentContext;
  }
}
