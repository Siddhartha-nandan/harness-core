/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sRollingRollbackBaseHandler.ResourceRecreationStatus.RESOURCE_CREATION_SUCCESSFUL;
import static io.harness.delegate.k8s.K8sTestHelper.crdNew;
import static io.harness.delegate.k8s.K8sTestHelper.crdOld;
import static io.harness.delegate.k8s.K8sTestHelper.deployment;
import static io.harness.delegate.k8s.K8sTestHelper.deploymentConfig;
import static io.harness.k8s.model.Release.Status.Succeeded;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.TATHAGAT;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.k8s.K8sRollingRollbackBaseHandler.ResourceRecreationStatus;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.k8s.releasehistory.K8sReleaseHistoryService;
import io.harness.delegate.k8s.releasehistory.K8sReleaseService;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@OwnedBy(CDP)
public class K8sRollingRollbackBaseHandlerTest extends CategoryTest {
  @Mock private LogCallback logCallback;
  @Mock private List<V1Secret> releaseHistory;
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private K8sReleaseHistoryService releaseHistoryService;
  @Mock private K8sReleaseService releaseService;

  @InjectMocks @Spy private K8sRollingRollbackBaseHandler k8sRollingRollbackBaseHandler;

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
  private K8sDelegateTaskParams k8sDelegateTaskParams =
      K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFirstDeploymentFailsRollBack() throws Exception {
    String releaseName = "releaseName";
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(null);
    rollbackHandlerConfig.setKubernetesConfig(kubernetesConfig);
    doReturn("").when(k8sTaskHelperBase).getReleaseHistoryData(kubernetesConfig, releaseName);

    k8sRollingRollbackBaseHandler.init(rollbackHandlerConfig, releaseName, logCallback);
    assertThat(rollbackHandlerConfig.isNoopRollBack()).isTrue();

    boolean result = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, null, logCallback, emptySet(), false);
    assertThat(result).isTrue();
    verify(logCallback).saveExecutionLog("No previous release found. Skipping rollback.");

    k8sRollingRollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, null, logCallback);
    verify(logCallback)
        .saveExecutionLog("Skipping Status Check since there is no previous eligible Managed Workload.", INFO);
    verify(k8sTaskHelperBase, never()).doStatusCheck(any(), any(), any(), any());

    k8sRollingRollbackBaseHandler.postProcess(rollbackHandlerConfig, releaseName);
    verify(k8sTaskHelperBase, never()).saveReleaseHistory(any(), any(), any(), eq(false));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkloadAndDoStatusCheck() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = crdNew();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    when(k8sTaskHelperBase.doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(),
             any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean(), anyLong(), eq(false)))
        .thenReturn(true);

    k8sRollingRollbackBaseHandler.init(rollbackHandlerConfig, "releaseName", logCallback);

    boolean result = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback, emptySet(), false);
    assertThat(result).isTrue();

    k8sRollingRollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, 10, logCallback);
    k8sRollingRollbackBaseHandler.postProcess(rollbackHandlerConfig, "releaseName");

    ArgumentCaptor<List> statusCheckCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);
    verify(k8sTaskHelperBase, times(1))
        .doStatusCheckForAllCustomResources(any(Kubectl.class), statusCheckCustomWorkloadsCaptor.capture(),
            any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean(), anyLong());
    List<KubernetesResource> customWorkloadsUnderCheck = statusCheckCustomWorkloadsCaptor.getValue();
    assertThat(customWorkloadsUnderCheck).isNotEmpty();
    assertThat(customWorkloadsUnderCheck.get(0)).isEqualTo(previousCustomResource);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkload() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = crdNew();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    boolean rollback = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback, emptySet(), false);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(LogCallback.class), anyBoolean(), eq(false));

    List<KubernetesResource> previousCustomWorkloads = previousCustomWorkloadsCaptor.getValue();
    assertThat(previousCustomWorkloads).isNotEmpty();
    assertThat(previousCustomWorkloads.get(0)).isEqualTo(previousCustomResource);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRollbackCustomWorkloadWithoutDeletingCurrent() throws Exception {
    KubernetesResource previousCustomResource = crdOld();
    KubernetesResource currentCustomResource = deployment();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig =
        prepareRollbackCustomWorkloads(previousCustomResource, currentCustomResource);

    boolean rollback = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, 2, logCallback, emptySet(), false);
    assertThat(rollback).isTrue();

    ArgumentCaptor<List> previousCustomWorkloadsCaptor = ArgumentCaptor.forClass(List.class);

    verify(k8sTaskHelperBase, times(0))
        .delete(any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());

    verify(k8sTaskHelperBase, times(1))
        .applyManifests(any(Kubectl.class), previousCustomWorkloadsCaptor.capture(), any(K8sDelegateTaskParams.class),
            any(LogCallback.class), anyBoolean(), eq(false));

    List<KubernetesResource> previousCustomWorkloads = previousCustomWorkloadsCaptor.getValue();
    assertThat(previousCustomWorkloads).isNotEmpty();
    assertThat(previousCustomWorkloads.get(0)).isEqualTo(previousCustomResource);
  }

  private K8sRollingRollbackHandlerConfig prepareRollbackCustomWorkloads(
      KubernetesResource previousCustomResource, KubernetesResource currentCustomResource) throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setKubernetesConfig(kubernetesConfig);
    rollbackHandlerConfig.setRelease(new V1SecretBuilder().build());
    rollbackHandlerConfig.setReleaseHistory(emptyList());
    rollbackHandlerConfig.setClient(Kubectl.client("kubectl", "config-path"));
    doReturn(new V1SecretBuilder().build()).when(releaseService).getLastSuccessfulRelease(anyList(), anyInt());
    doReturn(List.of(previousCustomResource)).when(releaseService).getResourcesFromRelease(any());

    when(k8sTaskHelperBase.applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
             any(LogCallback.class), anyBoolean(), eq(false)))
        .thenReturn(true);

    return rollbackHandlerConfig;
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void rollback() throws Exception {
    testRollBackReleaseIsNull();
  }

  private void testRollBackReleaseIsNull() throws Exception {
    final boolean success = k8sRollingRollbackBaseHandler.rollback(new K8sRollingRollbackHandlerConfig(),
        K8sDelegateTaskParams.builder().build(), null, logCallback, emptySet(), false);

    assertThat(success).isTrue();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollback() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setRelease(new V1SecretBuilder().build());
    doReturn(Succeeded.name()).when(releaseService).getReleaseLabelValue(any(), any());

    boolean rollback = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, null, logCallback, emptySet(), false);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, times(1)).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No failed release found. Skipping rollback.");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSkipRollbackNoPreviousEligibleRelease() throws Exception {
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setRelease(new V1SecretBuilder().build());
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    doReturn(null).when(releaseService).getLastSuccessfulRelease(anyList(), anyInt());

    boolean rollback = k8sRollingRollbackBaseHandler.rollback(
        rollbackHandlerConfig, k8sDelegateTaskParams, 1, logCallback, emptySet(), false);
    assertThat(rollback).isTrue();
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getValue()).isEqualTo("No previous eligible release found. Can't rollback.");
  }

  private List<KubernetesResourceIdRevision> getManagedWorkloads(KubernetesResource kubernetesResource) {
    if (kubernetesResource.getResourceId().getKind().equals("Deployment")) {
      return asList(Release.KubernetesResourceIdRevision.builder()
                        .workload(kubernetesResource.getResourceId())
                        .revision("2")
                        .build());
    }

    return emptyList();
  }

  private List<KubernetesResource> getCustomWorkloads(KubernetesResource kubernetesResource) {
    if (kubernetesResource.isManagedWorkload() && !kubernetesResource.getResourceId().getKind().equals("Deployment")) {
      return asList(kubernetesResource);
    }

    return emptyList();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void TC1_testRecreatePrunedResources() throws Exception {
    // no pruned resource
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    assertThat(k8sRollingRollbackBaseHandler.recreatePrunedResources(
                   rollbackHandlerConfig, 1, emptyList(), logCallback, k8sDelegateTaskParams))
        .isEqualTo(ResourceRecreationStatus.NO_RESOURCE_CREATED);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void TC2_testRecreatePrunedResources() throws Exception {
    // no release history
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(null);
    assertThat(k8sRollingRollbackBaseHandler.recreatePrunedResources(rollbackHandlerConfig, 1,
                   ImmutableList.of(KubernetesResourceId.builder().build()), logCallback, k8sDelegateTaskParams))
        .isEqualTo(ResourceRecreationStatus.NO_RESOURCE_CREATED);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetResourcesRecreated() {
    List<KubernetesResourceId> prunedResourceIds =
        singletonList(KubernetesResourceId.builder().name("dummy_name").build());
    assertThat(k8sRollingRollbackBaseHandler.getResourcesRecreated(prunedResourceIds, RESOURCE_CREATION_SUCCESSFUL))
        .isEqualTo(new HashSet<>(prunedResourceIds));
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void TC3_testRecreatePrunedResources() throws Exception {
    // no successful deployment in release history
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    assertThat(k8sRollingRollbackBaseHandler.recreatePrunedResources(rollbackHandlerConfig, 1,
                   ImmutableList.of(KubernetesResourceId.builder().build()), logCallback, k8sDelegateTaskParams))
        .isEqualTo(ResourceRecreationStatus.NO_RESOURCE_CREATED);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void TC4_testRecreatePrunedResources() throws Exception {
    // pruned resources are not present in last successful release
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    KubernetesResource resourcesInPreviousSuccessfulRelease =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build();

    assertThat(k8sRollingRollbackBaseHandler.recreatePrunedResources(rollbackHandlerConfig, 1,
                   ImmutableList.of(KubernetesResourceId.builder().name("resource1").build()), logCallback,
                   k8sDelegateTaskParams))
        .isEqualTo(ResourceRecreationStatus.NO_RESOURCE_CREATED);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void TC5_testRecreatePrunedResources() throws Exception {
    // pruning resources
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setClient(mock(Kubectl.class));

    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(KubernetesResourceId.builder().name("resource0").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource1").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource2").build());

    List<KubernetesResource> resourcesInPreviousSuccessfulRelease = new ArrayList<>();
    resourcesInPreviousSuccessfulRelease.add(
        KubernetesResource.builder().spec("spec0").resourceId(resourceIds.get(0)).build());
    resourcesInPreviousSuccessfulRelease.add(
        KubernetesResource.builder().spec("spec1").resourceId(resourceIds.get(1)).build());

    V1Secret previousRelease = new V1SecretBuilder().build();
    doReturn(previousRelease).when(releaseService).getLastSuccessfulRelease(anyList(), anyInt());
    doReturn(resourcesInPreviousSuccessfulRelease).when(releaseService).getResourcesFromRelease(any());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(), anyList(), any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean());

    assertThat(k8sRollingRollbackBaseHandler.recreatePrunedResources(
                   rollbackHandlerConfig, 1, resourceIds, logCallback, k8sDelegateTaskParams))
        .isEqualTo(ResourceRecreationStatus.RESOURCE_CREATION_SUCCESSFUL);
    verify(k8sTaskHelperBase, times(1))
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(LogCallback.class), anyBoolean());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteNewlyCreatedResourcesSuccess() throws Exception {
    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(KubernetesResourceId.builder().name("resource0").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource1").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource2").build());

    List<KubernetesResource> resources =
        resourceIds.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(toList());

    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    V1Secret currentRelease = new V1SecretBuilder().withType("currentRelease").build();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setRelease(currentRelease);
    rollbackHandlerConfig.setClient(mock(Kubectl.class));

    List<KubernetesResourceId> resourcesInPreviousSuccessfulRelease = new ArrayList<>();
    resourcesInPreviousSuccessfulRelease.add(resourceIds.get(0));

    List<KubernetesResource> previousResources =
        resourcesInPreviousSuccessfulRelease.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(toList());
    V1Secret previousRelease = new V1SecretBuilder().withType("previousRelease").build();
    doReturn(previousRelease).when(releaseService).getLastSuccessfulRelease(anyList(), anyInt());
    doReturn(previousResources).when(releaseService).getResourcesFromRelease(eq(previousRelease));
    doReturn(resources).when(releaseService).getResourcesFromRelease(eq(currentRelease));
    when(k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(anyList())).thenAnswer(i -> i.getArguments()[0]);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

    k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
        rollbackHandlerConfig, 1, logCallback, k8sDelegateTaskParams);

    verify(k8sTaskHelperBase, times(1))
        .executeDeleteHandlingPartialExecution(any(Kubectl.class), any(K8sDelegateTaskParams.class), captor.capture(),
            any(LogCallback.class), anyBoolean());
    List<KubernetesResourceId> resourceToBeDeleted = new ArrayList<>(captor.getValue());
    assertThat(resourceToBeDeleted).hasSize(2);
    assertThat(resourceToBeDeleted.stream().map(KubernetesResourceId::getName))
        .containsExactlyInAnyOrder("resource1", "resource2");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteNewlyCreatedResourcesFail() throws Exception {
    List<KubernetesResourceId> resourceIds = new ArrayList<>();
    resourceIds.add(KubernetesResourceId.builder().name("resource0").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource1").build());
    resourceIds.add(KubernetesResourceId.builder().name("resource2").build());

    List<KubernetesResource> resources =
        resourceIds.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(toList());

    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    V1Secret currentRelease = new V1SecretBuilder().withType("currentRelease").build();

    rollbackHandlerConfig.setReleaseHistory(releaseHistory);
    rollbackHandlerConfig.setRelease(currentRelease);
    rollbackHandlerConfig.setClient(mock(Kubectl.class));

    List<KubernetesResourceId> resourcesInPreviousSuccessfulRelease = new ArrayList<>();
    resourcesInPreviousSuccessfulRelease.add(resourceIds.get(0));
    resourcesInPreviousSuccessfulRelease.add(resourceIds.get(1));

    List<KubernetesResource> previousResources =
        resourcesInPreviousSuccessfulRelease.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(toList());
    V1Secret previousRelease = new V1SecretBuilder().withType("previousRelease").build();
    doReturn(previousRelease).when(releaseService).getLastSuccessfulRelease(anyList(), anyInt());
    doReturn(previousResources).when(releaseService).getResourcesFromRelease(eq(previousRelease));
    doReturn(resources).when(releaseService).getResourcesFromRelease(eq(currentRelease));
    when(k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(anyList())).thenAnswer(i -> i.getArguments()[0]);

    doThrow(new InvalidRequestException("dummy exception"))
        .when(k8sTaskHelperBase)
        .executeDeleteHandlingPartialExecution(
            any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());

    assertThatCode(()
                       -> k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
                           rollbackHandlerConfig, 1, logCallback, k8sDelegateTaskParams))
        .doesNotThrowAnyException();

    verify(k8sTaskHelperBase, times(1))
        .executeDeleteHandlingPartialExecution(
            any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteWithNoReleaseHistoryPresent() throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(null);

    k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
        rollbackHandlerConfig, 1, logCallback, k8sDelegateTaskParams);

    verify(k8sTaskHelperBase, never())
        .executeDeleteHandlingPartialExecution(
            any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteWithNoPreviousSuccessfulReleasePresent() throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);

    k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
        rollbackHandlerConfig, 1, logCallback, k8sDelegateTaskParams);

    verify(k8sTaskHelperBase, never())
        .executeDeleteHandlingPartialExecution(
            any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteAbortCase() throws Exception {
    K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();
    rollbackHandlerConfig.setReleaseHistory(releaseHistory);

    k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
        rollbackHandlerConfig, null, logCallback, k8sDelegateTaskParams);

    verify(k8sTaskHelperBase, never())
        .executeDeleteHandlingPartialExecution(
            any(Kubectl.class), any(K8sDelegateTaskParams.class), anyList(), any(LogCallback.class), anyBoolean());
  }
}
