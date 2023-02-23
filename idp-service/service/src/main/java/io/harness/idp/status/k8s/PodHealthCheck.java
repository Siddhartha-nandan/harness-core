/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.status.k8s;

import io.harness.exception.InvalidRequestException;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.status.enums.Status;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.client.K8sClient;
import io.harness.spec.server.idp.v1.model.NamespaceInfo;
import io.harness.spec.server.idp.v1.model.StatusInfo;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class PodHealthCheck implements HealthCheck {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private NamespaceService namespaceService;
  @Inject private K8sClient k8sClient;

  private static final String LABEL_SELECTOR = "app=idp-ui";
  public static final String MESSAGE_SEPARATOR = ". ";

  @Override
  public Optional<StatusInfo> getCurrentStatus(String accountId) {
    StatusInfo statusInfo = new StatusInfo();
    try {
      String namespace = getNamespaceForAccountId(accountId);
      ApiClient apiClient = kubernetesHelperService.getApiClient(k8sClient.getKubernetesConfig(namespace));
      CoreV1Api api = new CoreV1Api(apiClient);
      // TODO: Implement logic for pod restart scenarios
      V1PodList podList =
          api.listNamespacedPod(namespace, null, null, null, null, LABEL_SELECTOR, null, null, null, null, false);
      if (CollectionUtils.isEmpty(podList.getItems())) {
        statusInfo.setCurrentStatus(Status.NOT_FOUND.toString());
        statusInfo.setReason("No pod exists for namespace: " + namespace);
      } else if (podList.getItems().size() == 1) {
        V1Pod pod = podList.getItems().get(0);
        if (!isPodInPendingPhase(pod) && !isPodInWaitingState(pod)) {
          statusInfo.setCurrentStatus(Status.RUNNING.toString());
        } else if (isPodInPendingPhase(pod)) {
          statusInfo.setCurrentStatus(Status.PENDING.toString());
          statusInfo.setReason(getPodMessage(pod));
        } else {
          statusInfo.setCurrentStatus(Status.FAILED.toString());
          statusInfo.setReason(getPodMessage(pod));
        }
      }
    } catch (Exception e) {
      String err = "Could not check for pod status, " + e.getMessage();
      log.error(err, e);
      statusInfo.setReason(err);
    }
    return Optional.of(statusInfo);
  }

  private String getNamespaceForAccountId(String accountId) {
    Optional<NamespaceInfo> namespace = namespaceService.getNamespaceForAccountIdentifier(accountId);
    if (namespace.isPresent()) {
      return namespace.get().getNamespace();
    }
    throw new InvalidRequestException("No namespace present for accountId: " + accountId);
  }

  private boolean isPodInPendingPhase(V1Pod pod) {
    return Status.PENDING.toString().equalsIgnoreCase(pod.getStatus().getPhase());
  }

  private boolean isPodInWaitingState(V1Pod pod) {
    for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
      if (containerStatus.getState().getWaiting() != null) {
        return true;
      }
    }
    return false;
  }

  private String getPodMessage(V1Pod pod) {
    String msg;
    if (!CollectionUtils.isEmpty(pod.getStatus().getContainerStatuses())) {
      msg = pod.getStatus()
                .getContainerStatuses()
                .stream()
                .filter(containerStatus -> containerStatus.getState().getWaiting() != null)
                .filter(containerStatus -> containerStatus.getState().getWaiting().getMessage() != null)
                .map(containerStatus -> containerStatus.getState().getWaiting().getMessage())
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
    } else {
      msg = pod.getStatus()
                .getConditions()
                .stream()
                .filter(Objects::nonNull)
                .map(V1PodCondition::getMessage)
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
    }
    return msg;
  }
}
