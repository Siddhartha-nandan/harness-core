/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestConstants.CONFIG_MAP_YAML;
import static io.harness.delegate.k8s.K8sTestConstants.DAEMON_SET_YAML;
import static io.harness.delegate.k8s.K8sTestConstants.DEPLOYMENT_YAML;
import static io.harness.delegate.k8s.K8sTestConstants.SECRET_YAML;
import static io.harness.delegate.k8s.K8sTestConstants.SKIP_VERSIONING_CONFIG_MAP_YAML;
import static io.harness.delegate.k8s.K8sTestConstants.SKIP_VERSIONING_SECRET_YAML;
import static io.harness.k8s.releasehistory.IK8sRelease.Status.Failed;
import static io.harness.k8s.releasehistory.IK8sRelease.Status.InProgress;
import static io.harness.k8s.releasehistory.K8sReleaseConstants.RELEASE_NUMBER_LABEL_KEY;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABHINAV2;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BUHA;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.PRATYUSH;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.beans.K8sCanaryHandlerConfig;
import io.harness.delegate.k8s.trafficrouting.TrafficRoutingResourceCreator;
import io.harness.delegate.k8s.trafficrouting.TrafficRoutingResourceCreatorFactory;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.delegate.task.k8s.client.K8sApiClient;
import io.harness.delegate.task.k8s.istio.IstioTaskHelper;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.KubernetesYamlException;
import io.harness.helpers.k8s.releasehistory.K8sReleaseHandler;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.k8s.exception.KubernetesExceptionMessages;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.K8sRequestHandlerContext;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.releasehistory.IK8sRelease;
import io.harness.k8s.releasehistory.IK8sReleaseHistory;
import io.harness.k8s.releasehistory.K8SLegacyReleaseHistory;
import io.harness.k8s.releasehistory.K8sLegacyRelease;
import io.harness.k8s.releasehistory.K8sRelease;
import io.harness.k8s.releasehistory.K8sReleaseHistory;
import io.harness.k8s.releasehistory.K8sReleaseSecretHelper;
import io.harness.k8s.releasehistory.ReleaseHistory;
import io.harness.k8s.trafficrouting.TrafficRoutingInfoDTO;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;

import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class K8sCanaryBaseHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private IstioTaskHelper istioTaskHelper;
  @InjectMocks private K8sCanaryBaseHandler k8sCanaryBaseHandler;

  @Mock private LogCallback logCallback;
  @Mock private Kubectl client;
  @Mock private K8sReleaseHandler releaseHandler;
  @Mock private K8sApiClient kubernetesApiClient;
  @Mock K8sInfraDelegateConfig k8sInfraDelegateConfig;
  @Mock TrafficRoutingResourceCreator trafficRoutingResourceCreator;
  @Mock K8sCanaryHandlerConfig k8sCanaryHandlerConfig;

  private final String namespace = "default";
  private final K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
  private final KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(releaseHandler).when(k8sTaskHelperBase).getReleaseHandler(anyBoolean());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanaryNoWorkload() throws Exception {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareNoWorkloadNoResource();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertInvalidWorkloadsInManifest(result,
        "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment, DeploymentConfig (OpenShift) and StatefulSet workloads are supported in Canary workflow type.");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanaryNoWorkloadIsErrorFrameworkEnabled() {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareNoWorkloadNoResource();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    assertThatThrownBy(()
                           -> k8sCanaryBaseHandler.prepareForCanary(
                               k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, true))
        .matches(throwable -> {
          HintException hint = ExceptionUtils.cause(HintException.class, throwable);
          ExplanationException explanation = ExceptionUtils.cause(ExplanationException.class, throwable);
          KubernetesTaskException taskException = ExceptionUtils.cause(KubernetesTaskException.class, throwable);
          assertThat(hint).hasMessageContaining(KubernetesExceptionHints.CANARY_NO_WORKLOADS_FOUND);
          assertThat(explanation).hasMessageContaining(KubernetesExceptionExplanation.CANARY_NO_WORKLOADS_FOUND);
          assertThat(taskException).hasMessageContaining(KubernetesExceptionMessages.NO_WORKLOADS_FOUND);

          return true;
        });
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanaryMultipleWorkloads() throws Exception {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareMultipleWorkloads();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertInvalidWorkloadsInManifest(result,
        "\nMore than one workloads found in the Manifests. Canary deploy supports only one workload. Others should be marked with annotation harness.io/direct-apply: true");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanaryMultipleWorkloadsIsErrorFrameworkEnabled() {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareMultipleWorkloads();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    assertThatThrownBy(()
                           -> k8sCanaryBaseHandler.prepareForCanary(
                               k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, true))
        .matches(throwable -> {
          HintException hint = ExceptionUtils.cause(HintException.class, throwable);
          ExplanationException explanation = ExceptionUtils.cause(ExplanationException.class, throwable);
          KubernetesTaskException taskException = ExceptionUtils.cause(KubernetesTaskException.class, throwable);
          assertThat(hint).hasMessageContaining(KubernetesExceptionHints.CANARY_MULTIPLE_WORKLOADS);
          assertThat(explanation)
              .hasMessageContaining(format(KubernetesExceptionExplanation.CANARY_MULTIPLE_WORKLOADS, 2,
                  "Deployment/deployment, Deployment/deployment"));
          assertThat(taskException).hasMessageContaining(KubernetesExceptionMessages.MULTIPLE_WORKLOADS);

          return true;
        });
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanary() throws Exception {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareValidWorkloads();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    k8sCanaryHandlerConfig.setUseDeclarativeRollback(true);
    k8sCanaryHandlerConfig.setReleaseHistory(
        K8sReleaseHistory.builder().releaseHistory(Collections.emptyList()).build());

    doNothing().when(releaseHandler).cleanReleaseHistory(any());
    doReturn(K8sRelease.builder().releaseSecret(new V1Secret()).build())
        .when(releaseHandler)
        .createRelease(any(), anyInt());

    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertThat(result).isTrue();
    verify(releaseHandler, times(1)).cleanReleaseHistory(any());
    verify(k8sTaskHelperBase, times(1)).getResourcesInTableFormat(any());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForCanaryCleanupCanaryTrue() throws Exception {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareValidWorkloads();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    k8sCanaryHandlerConfig.setUseDeclarativeRollback(true);
    V1Secret inProgressReleaseSecret = new V1SecretBuilder().build();
    K8sReleaseSecretHelper.putLabelsItem(inProgressReleaseSecret, RELEASE_NUMBER_LABEL_KEY, "1");
    K8sRelease inProgressRelease = K8sRelease.builder().releaseSecret(inProgressReleaseSecret).build();
    inProgressRelease.updateReleaseStatus(InProgress);
    List<K8sRelease> releaseList = List.of(inProgressRelease);

    k8sCanaryHandlerConfig.setCurrentReleaseNumber(2);
    k8sCanaryHandlerConfig.setReleaseHistory(K8sReleaseHistory.builder().releaseHistory(releaseList).build());

    ArgumentCaptor<K8sReleaseHistory> releaseHistoryArgumentCaptor = ArgumentCaptor.forClass(K8sReleaseHistory.class);
    ArgumentCaptor<Integer> releaseNumberCaptor = ArgumentCaptor.forClass(Integer.class);
    doReturn(null)
        .when(k8sTaskHelperBase)
        .createReleaseHistoryCleanupRequest(
            any(), releaseHistoryArgumentCaptor.capture(), any(), any(), any(), releaseNumberCaptor.capture(), any());
    doNothing().when(releaseHandler).cleanReleaseHistory(any());
    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertThat(result).isTrue();
    assertThat(releaseHistoryArgumentCaptor.getValue().getLatestRelease().getReleaseStatus()).isEqualTo(Failed);
    assertThat(releaseNumberCaptor.getValue()).isEqualTo(2);
    verify(releaseHandler, times(1)).saveRelease(any());
    verify(releaseHandler, times(1)).cleanReleaseHistory(any());
    verify(k8sTaskHelperBase, times(1)).getResourcesInTableFormat(any());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testDeploymentWorkloadsForLegacyCanaryCleanupCanaryTrue() throws Exception {
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = prepareValidWorkloads();
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());
    k8sCanaryHandlerConfig.setUseDeclarativeRollback(false);

    List<K8sLegacyRelease> releaseList = new ArrayList<>();
    releaseList.add(K8sLegacyRelease.builder().number(1).status(IK8sRelease.Status.InProgress).build());
    k8sCanaryHandlerConfig.setReleaseHistory(K8SLegacyReleaseHistory.builder()
                                                 .releaseHistory(ReleaseHistory.builder().releases(releaseList).build())
                                                 .build());
    k8sCanaryHandlerConfig.setCurrentReleaseNumber(2);

    ArgumentCaptor<K8SLegacyReleaseHistory> releaseHistoryArgumentCaptor =
        ArgumentCaptor.forClass(K8SLegacyReleaseHistory.class);
    ArgumentCaptor<Integer> releaseNumberCaptor = ArgumentCaptor.forClass(Integer.class);

    doReturn(null)
        .when(k8sTaskHelperBase)
        .createReleaseHistoryCleanupRequest(
            any(), releaseHistoryArgumentCaptor.capture(), any(), any(), any(), releaseNumberCaptor.capture(), any());
    doNothing().when(releaseHandler).cleanReleaseHistory(any());

    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertThat(result).isTrue();

    assertThat(releaseHistoryArgumentCaptor.getValue().getReleaseHistory().getRelease(1).getReleaseStatus())
        .isEqualTo(Failed);
    assertThat(releaseNumberCaptor.getValue()).isEqualTo(2);
    verify(releaseHandler, times(1)).cleanReleaseHistory(any());
    verify(k8sTaskHelperBase, times(1)).getResourcesInTableFormat(any());
    verify(k8sTaskHelperBase, times(1)).addRevisionNumber(any(K8sRequestHandlerContext.class), anyInt());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSupportedWorkloadsInBgWorkflow() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = new K8sCanaryHandlerConfig();

    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(DAEMON_SET_YAML));
    k8sCanaryHandlerConfig.setResources(kubernetesResources);
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(k8sCanaryHandlerConfig.getResources());

    boolean result = k8sCanaryBaseHandler.prepareForCanary(
        k8sCanaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);
    assertInvalidWorkloadsInManifest(result,
        "\nNo workload found in the Manifests. Can't do Canary Deployment. Only Deployment, DeploymentConfig (OpenShift) and StatefulSet workloads are supported in Canary workflow type.");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetAllPods() throws Exception {
    KubernetesResource kubernetesResource =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().namespace("default").build()).build();
    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    canaryHandlerConfig.setKubernetesConfig(kubernetesConfig);
    canaryHandlerConfig.setCanaryWorkload(kubernetesResource);
    testGetAllPodsWithNoPreviousPods(canaryHandlerConfig);
    testGetAllPodsWithPreviousPods(canaryHandlerConfig);
  }

  public void testGetAllPodsWithNoPreviousPods(K8sCanaryHandlerConfig canaryHandlerConfig) throws Exception {
    final String releaseName = "releaseName";
    final long timeoutInMillis = 50000;
    final K8sPod canaryPod = K8sPod.builder().name("pod-canary").build();
    when(k8sTaskHelperBase.getPodDetails(kubernetesConfig, namespace, releaseName, timeoutInMillis))
        .thenReturn(asList(canaryPod));
    when(k8sTaskHelperBase.getPodDetailsWithTrack(kubernetesConfig, namespace, releaseName, "canary", timeoutInMillis))
        .thenReturn(asList(canaryPod));

    final List<K8sPod> allPods = k8sCanaryBaseHandler.getAllPods(canaryHandlerConfig, releaseName, timeoutInMillis);
    assertThat(allPods).hasSize(1);
    assertThat(allPods.get(0).isNewPod()).isTrue();
    assertThat(allPods.get(0).getName()).isEqualTo(canaryPod.getName());
  }

  public void testGetAllPodsWithPreviousPods(K8sCanaryHandlerConfig canaryHandlerConfig) throws Exception {
    final String releaseName = "releaseName";
    final long timeoutInMillis = 50000;
    final K8sPod canaryPod = K8sPod.builder().name("pod-canary").build();
    final List<K8sPod> allPods =
        asList(K8sPod.builder().name("primary-1").build(), K8sPod.builder().name("primary-2").build(), canaryPod);
    when(k8sTaskHelperBase.getPodDetails(kubernetesConfig, namespace, releaseName, timeoutInMillis))
        .thenReturn(allPods);
    when(k8sTaskHelperBase.getPodDetailsWithTrack(kubernetesConfig, namespace, releaseName, "canary", timeoutInMillis))
        .thenReturn(asList(canaryPod));

    final List<K8sPod> pods = k8sCanaryBaseHandler.getAllPods(canaryHandlerConfig, releaseName, timeoutInMillis);
    assertThat(pods).hasSize(3);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(1);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-canary", "primary-1", "primary-2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testPrepareCanary() throws Exception {
    cannotDeployMoreThan1Workload();
    cannotDeployMoreThanEmptyWorkload();
    deploySingleWorkload();
  }

  public void cannotDeployMoreThan1Workload() throws Exception {
    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(false).name("object-2").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().versioned(false).name("object-3").kind("DeploymentConfig").build())
            .build());
    canaryHandlerConfig.setResources(resources);
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(canaryHandlerConfig.getResources());

    boolean success = k8sCanaryBaseHandler.prepareForCanary(
        canaryHandlerConfig, context, K8sDelegateTaskParams.builder().build(), false, logCallback, false);

    assertThat(success).isFalse();
  }

  public void cannotDeployMoreThanEmptyWorkload() throws Exception {
    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build());
    canaryHandlerConfig.setResources(resources);
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(canaryHandlerConfig.getResources());

    boolean success = k8sCanaryBaseHandler.prepareForCanary(
        canaryHandlerConfig, context, K8sDelegateTaskParams.builder().build(), false, logCallback, false);

    assertThat(success).isFalse();
  }

  public void deploySingleWorkload() throws Exception {
    Kubectl client = mock(Kubectl.class);
    KubernetesResource deployment = ManifestHelper.processYaml(DEPLOYMENT_YAML).get(0);
    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ReleaseHistory releaseHist = ReleaseHistory.createNew();
    K8SLegacyReleaseHistory releaseHistory = K8SLegacyReleaseHistory.builder().releaseHistory(releaseHist).build();
    List<KubernetesResource> resources = asList(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().versioned(true).name("object-1").kind("configMap").build())
            .build(),
        deployment);
    canaryHandlerConfig.setResources(resources);
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(canaryHandlerConfig.getResources());
    canaryHandlerConfig.setReleaseHistory(releaseHistory);
    canaryHandlerConfig.setReleaseName("release-01");
    canaryHandlerConfig.setClient(client);
    doReturn(K8sLegacyRelease.builder().build()).when(releaseHandler).createRelease(any(), anyInt());

    boolean success = k8sCanaryBaseHandler.prepareForCanary(
        canaryHandlerConfig, context, delegateTaskParams, false, logCallback, false);

    assertThat(success).isTrue();
    assertThat(canaryHandlerConfig.getCanaryWorkload()).isNotNull();
    verify(releaseHandler, times(1)).cleanReleaseHistory(any());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateTargetInstances() {
    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    canaryHandlerConfig.setCanaryWorkload(ManifestHelper.processYaml(DEPLOYMENT_YAML).get(0));
    canaryHandlerConfig.setReleaseName("release-01");
    K8sRequestHandlerContext context = new K8sRequestHandlerContext();
    context.setResources(canaryHandlerConfig.getResources());

    k8sCanaryBaseHandler.updateTargetInstances(canaryHandlerConfig, context, 4, logCallback);
    assertThat(canaryHandlerConfig.getTargetInstances()).isEqualTo(4);
    KubernetesResource canaryWorkload = canaryHandlerConfig.getCanaryWorkload();
    Map matchLabels = (Map) canaryWorkload.getField("spec.selector.matchLabels");
    Map podsSpecLabels = (Map) canaryWorkload.getField("spec.template.metadata.labels");
    assertThat(canaryWorkload.getResourceId().getName()).endsWith("canary");
    assertThat(matchLabels.get("harness.io/track")).isEqualTo("canary");
    assertThat(podsSpecLabels.get("harness.io/track")).isEqualTo("canary");
    assertThat(podsSpecLabels.get("harness.io/release-name")).isEqualTo("release-01");
    assertThat(canaryWorkload.getReplicaCount()).isEqualTo(4);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateDestinationRuleManifestFilesWithSubsets() throws IOException {
    List<KubernetesResource> kubernetesResources = Collections.emptyList();

    k8sCanaryBaseHandler.updateDestinationRuleManifestFilesWithSubsets(
        kubernetesResources, kubernetesConfig, logCallback);

    verify(istioTaskHelper, times(1))
        .updateDestinationRuleManifestFilesWithSubsets(kubernetesResources,
            asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), kubernetesConfig, logCallback);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testUpdateDestinationRuleManifestFilesWithSubsetsWithoutFabric8() throws IOException {
    List<KubernetesResource> kubernetesResources = Collections.emptyList();

    k8sCanaryBaseHandler.updateDestinationRuleManifestFilesWithSubsets(kubernetesResources, null, logCallback);

    verify(istioTaskHelper, times(1))
        .updateDestinationRuleManifestFilesWithSubsets(kubernetesResources,
            asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), null, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testWrapUp() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    k8sCanaryBaseHandler.wrapUp(client, delegateTaskParams, logCallback);
    verify(k8sTaskHelperBase, times(1)).describe(client, delegateTaskParams, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFailAndSaveKubernetesRelease() throws Exception {
    IK8sReleaseHistory releaseHistory = mock(IK8sReleaseHistory.class);
    IK8sRelease currentRelease = mock(IK8sRelease.class);

    K8sCanaryHandlerConfig canaryHandlerConfig = new K8sCanaryHandlerConfig();
    canaryHandlerConfig.setReleaseHistory(releaseHistory);
    canaryHandlerConfig.setKubernetesConfig(kubernetesConfig);
    canaryHandlerConfig.setUseDeclarativeRollback(false);
    canaryHandlerConfig.setReleaseName("release");
    canaryHandlerConfig.setCurrentRelease(currentRelease);

    k8sCanaryBaseHandler.failAndSaveRelease(canaryHandlerConfig);

    verify(k8sTaskHelperBase, times(1))
        .saveRelease(false, false, kubernetesConfig, currentRelease, releaseHistory, "release");
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testAppendingSecretConfigmapNamesToCanaryWorkloads() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    kubernetesResources.addAll(ManifestHelper.processYaml(CONFIG_MAP_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(SECRET_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.forEach(resource -> resource.getResourceId().setNamespace("ns"));

    String canaryResources =
        k8sCanaryBaseHandler.appendSecretAndConfigMapNamesToCanaryWorkloads("ns/Deployment/test", kubernetesResources);

    assertThat(canaryResources).isEqualTo("ns/Deployment/test,ns/ConfigMap/mycm,ns/Secret/mysecret");

    // resources without any configmaps/secrets
    assertThat(k8sCanaryBaseHandler.appendSecretAndConfigMapNamesToCanaryWorkloads(
                   "test", List.of(kubernetesResources.get(2))))
        .isEqualTo("test");

    kubernetesResources.addAll(ManifestHelper.processYaml(SKIP_VERSIONING_CONFIG_MAP_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(SKIP_VERSIONING_SECRET_YAML));

    canaryResources =
        k8sCanaryBaseHandler.appendSecretAndConfigMapNamesToCanaryWorkloads("ns/Deployment/test", kubernetesResources);
    assertThat(canaryResources).isEqualTo("ns/Deployment/test,ns/ConfigMap/mycm,ns/Secret/mysecret");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testFindPrimaryServiceForCanaryDeploymentWhenPrimaryExists() {
    KubernetesResource primaryService =
        when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(true).getMock();
    KubernetesResource someOtherService =
        when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(false).getMock();
    KubernetesResource deployment = when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(false).getMock();

    List<KubernetesResource> kubernetesResources = List.of(primaryService, someOtherService, deployment);

    KubernetesResource primaryServiceForCanaryDeployment =
        k8sCanaryBaseHandler.findPrimaryServiceForCanaryDeployment(kubernetesResources);

    assertThat(primaryServiceForCanaryDeployment).isEqualTo(primaryService);
  }

  @Test(expected = KubernetesYamlException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testFindPrimaryServiceForCanaryDeploymentWhenTwoPrimaryExists() {
    KubernetesResource primaryService =
        when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(true).getMock();
    KubernetesResource someOtherService =
        when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(true).getMock();
    KubernetesResource deployment = when(mock(KubernetesResource.class).isPrimaryService()).thenReturn(false).getMock();

    List<KubernetesResource> kubernetesResources = List.of(primaryService, someOtherService, deployment);

    k8sCanaryBaseHandler.findPrimaryServiceForCanaryDeployment(kubernetesResources);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testFindPrimaryServiceForCanaryDeploymentWhenOneService() {
    KubernetesResource primaryService = when(mock(KubernetesResource.class).isService()).thenReturn(true).getMock();
    KubernetesResource someResource = when(mock(KubernetesResource.class).isService()).thenReturn(false).getMock();
    KubernetesResource deployment = when(mock(KubernetesResource.class).isService()).thenReturn(false).getMock();

    List<KubernetesResource> kubernetesResources = List.of(primaryService, someResource, deployment);

    KubernetesResource primaryServiceForCanaryDeployment =
        k8sCanaryBaseHandler.findPrimaryServiceForCanaryDeployment(kubernetesResources);

    assertThat(primaryServiceForCanaryDeployment).isEqualTo(primaryService);
  }

  @Test(expected = HintException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testFindPrimaryServiceForCanaryDeploymentWhenNoService() {
    KubernetesResource resource = when(mock(KubernetesResource.class).isService()).thenReturn(false).getMock();
    KubernetesResource someResource = when(mock(KubernetesResource.class).isService()).thenReturn(false).getMock();
    KubernetesResource deployment = when(mock(KubernetesResource.class).isService()).thenReturn(false).getMock();

    List<KubernetesResource> kubernetesResources = List.of(resource, someResource, deployment);

    k8sCanaryBaseHandler.findPrimaryServiceForCanaryDeployment(kubernetesResources);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateCanaryServiceFromPrimary() {
    String spec = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp";

    String canarySpec = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service-canary\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp\n";
    KubernetesResource primaryService =
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("my-service").kind("Service").namespace("default").build())
            .spec(spec)
            .build();

    KubernetesResource canaryService = k8sCanaryBaseHandler.createCanaryServiceFromPrimary(primaryService);
    assertThat(canaryService.getResourceId().getName()).isEqualTo("my-service-canary");
    assertThat(canaryService.getResourceId().getKind()).isEqualTo("Service");
    assertThat(canaryService.getResourceId().getNamespace()).isEqualTo("default");
    assertThat(canaryService.getSpec()).isEqualTo(canarySpec);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testSetStableAndCanaryLabelSelectors() {
    String spec = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp";

    String canarySpec = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service-canary\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp\n";

    String specWithStableSelector = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp\n"
        + "    harness.io/track: stable\n";

    String canarySpecWithCanarySelector = "apiVersion: v1\n"
        + "kind: Service\n"
        + "metadata:\n"
        + "  name: my-service-canary\n"
        + "spec:\n"
        + "  selector:\n"
        + "    app: MyApp\n"
        + "    harness.io/track: canary\n";

    KubernetesResource primaryService =
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("my-service").kind("Service").namespace("default").build())
            .spec(spec)
            .build();

    KubernetesResource canaryService =
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("my-service-canary").kind("Service").namespace("default").build())
            .spec(canarySpec)
            .build();

    k8sCanaryBaseHandler.setStableAndCanaryLabelSelectors(primaryService, canaryService);

    assertThat(primaryService.getSpec()).isEqualTo(specWithStableSelector);
    assertThat(canaryService.getSpec()).isEqualTo(canarySpecWithCanarySelector);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateTrafficRoutingResources() {
    K8sTrafficRoutingConfig trafficRoutingProvider = K8sTrafficRoutingConfig.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    KubernetesResource primaryService =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("primaryService").build()).build();
    KubernetesResource canaryService =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("canaryService").build()).build();
    K8sLegacyRelease legacyRelease = K8sLegacyRelease.builder().build();
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

    final K8sCanaryDeployRequest k8sCanaryDeployRequest =
        K8sCanaryDeployRequest.builder()
            .skipResourceVersioning(true)
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().build())
            .releaseName("releaseName")
            .useDeclarativeRollback(true)
            .trafficRoutingConfig(trafficRoutingProvider)
            .build();

    doReturn(List.of(KubernetesResource.builder().build()))
        .when(trafficRoutingResourceCreator)
        .createTrafficRoutingResources(any(), any(), any(), any(), any(), any());
    doReturn(Optional.of(TrafficRoutingInfoDTO.builder().build()))
        .when(trafficRoutingResourceCreator)
        .getTrafficRoutingInfo(anyList());
    doReturn(legacyRelease).when(k8sCanaryHandlerConfig).getCurrentRelease();
    doReturn(kubernetesConfig).when(k8sCanaryHandlerConfig).getKubernetesConfig();

    try (MockedStatic<TrafficRoutingResourceCreatorFactory> utilities =
             Mockito.mockStatic(TrafficRoutingResourceCreatorFactory.class)) {
      utilities.when(() -> TrafficRoutingResourceCreatorFactory.create(trafficRoutingProvider))
          .thenReturn(trafficRoutingResourceCreator);

      List<KubernetesResource> trafficRoutingResources =
          k8sCanaryBaseHandler.createTrafficRoutingResources(k8sCanaryDeployRequest, k8sDelegateTaskParams,
              trafficRoutingProvider, k8sCanaryHandlerConfig, primaryService, canaryService, logCallback);

      assertThat(trafficRoutingResources).hasSize(1);
      assertThat(legacyRelease.getTrafficRoutingInfo()).isNotNull();
      verify(kubernetesApiClient).getApiVersions(any(), any(), any(), any());
      verify(trafficRoutingResourceCreator).createTrafficRoutingResources(any(), any(), any(), any(), any(), any());
    }
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testCreateTrafficRoutingResourcesSkipStoringInfoInRelease() {
    K8sTrafficRoutingConfig trafficRoutingProvider = K8sTrafficRoutingConfig.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().workingDirectory(".").build();
    KubernetesResource primaryService =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("primaryService").build()).build();
    KubernetesResource canaryService =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("canaryService").build()).build();
    K8sLegacyRelease legacyRelease = K8sLegacyRelease.builder().build();
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

    final K8sCanaryDeployRequest k8sCanaryDeployRequest =
        K8sCanaryDeployRequest.builder()
            .skipResourceVersioning(true)
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().build())
            .releaseName("releaseName")
            .useDeclarativeRollback(true)
            .trafficRoutingConfig(trafficRoutingProvider)
            .build();

    doReturn(List.of(KubernetesResource.builder().build()))
        .when(trafficRoutingResourceCreator)
        .createTrafficRoutingResources(any(), any(), any(), any(), any(), any());
    doReturn(Optional.empty()).when(trafficRoutingResourceCreator).getTrafficRoutingInfo(anyList());
    doReturn(legacyRelease).when(k8sCanaryHandlerConfig).getCurrentRelease();
    doReturn(kubernetesConfig).when(k8sCanaryHandlerConfig).getKubernetesConfig();

    try (MockedStatic<TrafficRoutingResourceCreatorFactory> utilities =
             Mockito.mockStatic(TrafficRoutingResourceCreatorFactory.class)) {
      utilities.when(() -> TrafficRoutingResourceCreatorFactory.create(trafficRoutingProvider))
          .thenReturn(trafficRoutingResourceCreator);

      List<KubernetesResource> trafficRoutingResources =
          k8sCanaryBaseHandler.createTrafficRoutingResources(k8sCanaryDeployRequest, k8sDelegateTaskParams,
              trafficRoutingProvider, k8sCanaryHandlerConfig, primaryService, canaryService, logCallback);

      assertThat(trafficRoutingResources).hasSize(1);
      assertThat(legacyRelease.getTrafficRoutingInfo()).isNull();
      verify(kubernetesApiClient).getApiVersions(any(), any(), any(), any());
      verify(trafficRoutingResourceCreator).createTrafficRoutingResources(any(), any(), any(), any(), any(), any());
    }
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testAppendTrafficRoutingResourceNamesToCanaryWorkloads() {
    List<KubernetesResource> resources = List.of(
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("service-canary").kind("Service").namespace("default").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder()
                            .kind("TrafficSplit")
                            .name("traffic-split-name")
                            .namespace("default")
                            .build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder()
                            .kind("HTTPRouteGroup")
                            .name("routing-group-name")
                            .namespace("default")
                            .build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().kind("Deployment").name("deployment").namespace("default").build())
            .build());

    assertThat(k8sCanaryBaseHandler.appendTrafficRoutingResourceNamesToCanaryWorkloads("default/kind/name", resources))
        .isEqualTo(
            "default/kind/name,default/Service/service-canary,default/TrafficSplit/traffic-split-name,default/HTTPRouteGroup/routing-group-name");
  }

  private void assertInvalidWorkloadsInManifest(boolean result, String expectedMessage) throws Exception {
    assertThat(result).isFalse();
    verify(releaseHandler, never()).cleanReleaseHistory(any());
    verify(k8sTaskHelperBase, times(1)).getResourcesInTableFormat(any());

    ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LogLevel> logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);
    ArgumentCaptor<CommandExecutionStatus> commandExecutionStatusCaptor =
        ArgumentCaptor.forClass(CommandExecutionStatus.class);
    verify(logCallback, times(1))
        .saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture(), commandExecutionStatusCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
    assertThat(commandExecutionStatusCaptor.getValue()).isEqualTo(FAILURE);
    assertThat(msgCaptor.getValue()).isEqualTo(expectedMessage);
  }

  private K8sCanaryHandlerConfig prepareNoWorkloadNoResource() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = new K8sCanaryHandlerConfig();
    kubernetesResources.addAll(ManifestHelper.processYaml(DAEMON_SET_YAML));
    k8sCanaryHandlerConfig.setResources(kubernetesResources);

    return k8sCanaryHandlerConfig;
  }

  private K8sCanaryHandlerConfig prepareMultipleWorkloads() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = new K8sCanaryHandlerConfig();
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    k8sCanaryHandlerConfig.setResources(kubernetesResources);

    return k8sCanaryHandlerConfig;
  }

  private K8sCanaryHandlerConfig prepareValidWorkloads() {
    List<KubernetesResource> kubernetesResources = new ArrayList<>();
    K8sCanaryHandlerConfig k8sCanaryHandlerConfig = new K8sCanaryHandlerConfig();
    kubernetesResources.addAll(ManifestHelper.processYaml(CONFIG_MAP_YAML));
    kubernetesResources.addAll(ManifestHelper.processYaml(DEPLOYMENT_YAML));
    k8sCanaryHandlerConfig.setResources(kubernetesResources);

    return k8sCanaryHandlerConfig;
  }
}
