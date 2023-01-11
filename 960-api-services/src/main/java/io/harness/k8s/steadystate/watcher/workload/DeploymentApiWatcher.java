/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.steadystate.watcher.workload;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.steadystate.K8sSteadyStateConstants.WATCH_CALL_TIMEOUT_SECONDS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.configuration.KubernetesCliCommandType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.KubernetesCliTaskRuntimeException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.steadystate.model.K8ApiResponseDTO;
import io.harness.k8s.steadystate.model.K8sStatusWatchDTO;
import io.harness.k8s.steadystate.statusviewer.DeploymentStatusViewer;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import java.io.InterruptedIOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class DeploymentApiWatcher implements WorkloadWatcher {
  @Inject private DeploymentStatusViewer statusViewer;

  @Override
  public boolean watchRolloutStatus(K8sStatusWatchDTO k8SStatusWatchDTO, KubernetesResourceId workload,
      LogCallback executionLogCallback) throws Exception {
    return watchDeployment(
        k8SStatusWatchDTO.getApiClient(), workload, executionLogCallback, k8SStatusWatchDTO.isErrorFrameworkEnabled());
  }

  private boolean watchDeployment(ApiClient apiClient, KubernetesResourceId deploymentResource,
      LogCallback executionLogCallback, boolean errorFrameworkEnabled) throws Exception {
    Preconditions.checkNotNull(apiClient, "K8s API Client cannot be null.");
    AppsV1Api appsV1Api = new AppsV1Api(apiClient);
    while (!Thread.currentThread().isInterrupted()) {
      try (Watch<V1Deployment> watch = createWatchCall(apiClient, appsV1Api, deploymentResource.getNamespace())) {
        for (Watch.Response<V1Deployment> event : watch) {
          V1Deployment deployment = event.object;
          V1ObjectMeta meta = deployment.getMetadata();
          if (meta != null && !deploymentResource.getName().equals(meta.getName())) {
            continue;
          }
          switch (event.type) {
            case "ADDED":
            case "MODIFIED":
              K8ApiResponseDTO rolloutStatus = statusViewer.extractRolloutStatus(deployment);
              executionLogCallback.saveExecutionLog(rolloutStatus.getMessage());
              if (rolloutStatus.isFailed()) {
                if (errorFrameworkEnabled) {
                  throw new KubernetesCliTaskRuntimeException(
                      rolloutStatus.getMessage(), KubernetesCliCommandType.STEADY_STATE_CHECK);
                }
                return false;
              }
              if (rolloutStatus.isDone()) {
                return true;
              }
              break;
            case "DELETED":
              throw new KubernetesCliTaskRuntimeException(
                  "object has been deleted", KubernetesCliCommandType.STEADY_STATE_CHECK);
            default:
              log.warn(String.format("Unexpected k8s event type %s", event.type));
          }
        }
      } catch (IOException e) {
        IOException ex = ExceptionMessageSanitizer.sanitizeException(e);
        String errorMessage = "Failed to close Kubernetes watch." + ExceptionUtils.getMessage(ex);
        log.error(errorMessage, ex);
        return false;
      } catch (ApiException e) {
        ApiException ex = ExceptionMessageSanitizer.sanitizeException(e);
        String errorMessage =
            String.format("Failed to watch rollout status for workload [%s]. ", deploymentResource.kindNameRef())
            + ExceptionUtils.getMessage(ex);
        log.error(errorMessage, ex);
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
        if (errorFrameworkEnabled) {
          throw e;
        }
        return false;
      } catch (RuntimeException e) {
        if (e.getCause() instanceof InterruptedIOException) {
          log.warn("Kubernetes watch was aborted.", e);
          Thread.currentThread().interrupt();
          return false;
        }
        log.error("Runtime exception during Kubernetes watch.", e);
        throw e;
      }
    }
    return false;
  }

  private Watch<V1Deployment> createWatchCall(ApiClient apiClient, AppsV1Api appsV1Api, String namespace)
      throws ApiException {
    Call call = appsV1Api.listNamespacedDeploymentCall(
        namespace, null, null, null, null, null, null, null, null, WATCH_CALL_TIMEOUT_SECONDS, true, null);
    return Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Deployment>>() {}.getType());
  }
}
