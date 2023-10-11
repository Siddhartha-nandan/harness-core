/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHIKA;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskKeys;
import io.harness.cvng.cdng.entities.CVNGStepTask.Status;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.cdng.services.impl.CVNGStep.CVNGResponseData;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CVNGStepTaskServiceImplTest extends CvNextGenTestBase {
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Mock private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  BuilderFactory builderFactory;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    FieldUtils.writeField(cvngStepTaskService, "waitNotifyEngine", waitNotifyEngine, true);
    FieldUtils.writeField(cvngStepTaskService, "verificationJobInstanceService", verificationJobInstanceService, true);
    FieldUtils.writeField(cvngStepTaskService, "deploymentLogAnalysisService", deploymentLogAnalysisService, true);
    FieldUtils.writeField(
        cvngStepTaskService, "deploymentTimeSeriesAnalysisService", deploymentTimeSeriesAnalysisService, true);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate() {
    String activityId = generateUuid();
    cvngStepTaskService.create(
        builderFactory.cvngStepTaskBuilder().activityId(activityId).callbackId(activityId).build());
    assertThat(get(activityId)).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withSkip() {
    String callbackId = generateUuid();
    cvngStepTaskService.create(builderFactory.cvngStepTaskBuilder().callbackId(callbackId).skip(true).build());
    assertThat(get(callbackId)).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_invalid() {
    assertThatThrownBy(()
                           -> cvngStepTaskService.create(
                               CVNGStepTask.builder().accountId(generateUuid()).activityId(generateUuid()).build()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNotifyCVNGStepIfDone() {
    String activityId = generateUuid();
    String accountId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentVerificationJobInstanceSummary.builder()
            .status(ActivityVerificationStatus.VERIFICATION_PASSED)
            .progressPercentage(100)
            .durationMs(Duration.ofMinutes(30).toMillis())
            .activityId(activityId)
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(Mockito.anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    cvngStepTaskService.create(builderFactory.cvngStepTaskBuilder()
                                   .accountId(accountId)
                                   .activityId(activityId)
                                   .callbackId(activityId)
                                   .verificationJobInstanceId(generateUuid())
                                   .status(Status.IN_PROGRESS)
                                   .build());
    cvngStepTaskService.notifyCVNGStep(get(activityId));
    assertThat(get(activityId).getStatus()).isEqualTo(Status.DONE);
    verify(waitNotifyEngine, times(1))
        .doneWith(eq(activityId),
            eq(CVNGResponseData.builder()
                    .activityId(activityId)
                    .verifyStepExecutionId(activityId)
                    .activityStatusDTO(activityStatusDTO)
                    .build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNotifyCVNGStep_whenSkipIsTrue() {
    String callbackId = generateUuid();
    String accountId = generateUuid();
    cvngStepTaskService.create(builderFactory.cvngStepTaskBuilder()
                                   .accountId(accountId)
                                   .skip(true)
                                   .callbackId(callbackId)
                                   .status(Status.IN_PROGRESS)
                                   .build());
    cvngStepTaskService.notifyCVNGStep(get(callbackId));
    assertThat(get(callbackId).getStatus()).isEqualTo(Status.DONE);
    verify(waitNotifyEngine, times(1)).doneWith(eq(callbackId), eq(CVNGResponseData.builder().skip(true).build()));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testHealthSources() {
    String callbackId = generateUuid();
    String accountId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String cvConfigIdentifier = "nameSpaced/identifier";
    CVNGStepTask cvngStepTask = builderFactory.cvngStepTaskBuilder()
                                    .accountId(accountId)
                                    .skip(true)
                                    .callbackId(callbackId)
                                    .verificationJobInstanceId(verificationJobInstanceId)
                                    .status(Status.IN_PROGRESS)
                                    .build();
    cvngStepTaskService.create(cvngStepTask);
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder().identifier(cvConfigIdentifier).build();
    VerificationJobInstance verificationJobInstance = builderFactory.verificationJobInstanceBuilder()
                                                          .uuid(verificationJobInstanceId)
                                                          .cvConfigMap(new HashMap<String, CVConfig>() {
                                                            { put(cvConfigIdentifier, cvConfig); }
                                                          })
                                                          .build();
    when(verificationJobInstanceService.get(Arrays.asList(verificationJobInstanceId)))
        .thenReturn(Arrays.asList(verificationJobInstance));
    Set<HealthSourceDTO> healthSourceDTOSet =
        cvngStepTaskService.healthSources(accountId, cvngStepTask.getCallbackId());
    assertThat(healthSourceDTOSet.size()).isEqualTo(1);
    assertThat(healthSourceDTOSet.iterator().next().getIdentifier()).isEqualTo(cvConfigIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNotifyCVNGStep_ifInProgress() {
    String activityId = generateUuid();
    String accountId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.IN_PROGRESS)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentVerificationJobInstanceSummary.builder()
            .status(ActivityVerificationStatus.IN_PROGRESS)
            .progressPercentage(100)
            .durationMs(Duration.ofMinutes(30).toMillis())
            .activityId(activityId)
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(Mockito.anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    cvngStepTaskService.create(builderFactory.cvngStepTaskBuilder()
                                   .accountId(accountId)
                                   .activityId(activityId)
                                   .callbackId(activityId)
                                   .status(Status.IN_PROGRESS)
                                   .build());
    cvngStepTaskService.notifyCVNGStep(get(activityId));
    assertThat(get(activityId).getStatus()).isEqualTo(Status.IN_PROGRESS);
    verify(waitNotifyEngine, times(1))
        .progressOn(eq(activityId),
            eq(CVNGResponseData.builder()
                    .activityId(activityId)
                    .verifyStepExecutionId(activityId)
                    .activityStatusDTO(activityStatusDTO)
                    .build()));
  }

  @Test
  @Owner(developers = ANSHIKA)
  @Category(UnitTests.class)
  public void testGetDeploymentActivityLogAnalysisClusters(){
    String accountId = generateUuid();
    String callbackId = generateUuid();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = new DeploymentLogAnalysisFilter();
    LogAnalysisClusterChartDTO mockClusterDTO = LogAnalysisClusterChartDTO.builder()
            .label(1)
            .text("text")
            .hostName("hostName")
            .risk(Risk.HEALTHY)
            .x(10.0)
            .y(20.0)
            .clusterType(DeploymentLogAnalysisDTO.ClusterType.NO_BASELINE_AVAILABLE)
            .build();
    List<LogAnalysisClusterChartDTO> expectedClusters = new ArrayList<>();
    expectedClusters.add(mockClusterDTO);
    when(deploymentLogAnalysisService.getLogAnalysisClusters(anyString(), anyString(), any(DeploymentLogAnalysisFilter.class)))
            .thenReturn(expectedClusters);
    List<LogAnalysisClusterChartDTO> resultClusters = deploymentLogAnalysisService.getLogAnalysisClusters(accountId, callbackId, deploymentLogAnalysisFilter);
    assertThat(resultClusters)
            .extracting("label", "text", "hostName", "risk", "x", "y", "clusterType")
            .containsExactly(
                    tuple(1, "text", "hostName", Risk.HEALTHY, 10.0, 20.0, DeploymentLogAnalysisDTO.ClusterType.NO_BASELINE_AVAILABLE)
            );
  }

  @Test
  @Owner(developers = ANSHIKA)
  @Category(UnitTests.class)
  public void testGetDeployementActivityLogAnalysisClustersEmpty() {
    String accountId = generateUuid();
    String callbackId = generateUuid();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = new DeploymentLogAnalysisFilter();
    String verificationJobInstanceId = generateUuid();
    List<LogAnalysisClusterChartDTO> emptyClusters = Collections.emptyList();
    when(deploymentLogAnalysisService.getLogAnalysisClusters(
             accountId, verificationJobInstanceId, deploymentLogAnalysisFilter))
        .thenReturn(emptyClusters);
    List<LogAnalysisClusterChartDTO> resultClusters =
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, callbackId, deploymentLogAnalysisFilter);
    assertThat(emptyClusters).isEqualTo(resultClusters);
  }

  @Test
  @Owner(developers = ANSHIKA)
  @Category(UnitTests.class)
  public void testGetTransactionNamesEmpty() {
    String accountId = generateUuid();
    String callbackId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    List<String> emptyTransactionNames = Collections.emptyList();
    when(deploymentTimeSeriesAnalysisService.getTransactionNames(accountId, verificationJobInstanceId))
        .thenReturn(emptyTransactionNames);
    List<String> resultTransactionNames =
        deploymentTimeSeriesAnalysisService.getTransactionNames(accountId, callbackId);
    assertThat(resultTransactionNames).isEqualTo(emptyTransactionNames);
  }

  @Test
  @Owner(developers = ANSHIKA)
  @Category(UnitTests.class)
  public void testGetExecutionLogsEmpty() {
    String callbackId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    List<VerificationJobInstance.ProgressLog> expectedExecutionLogs = Collections.emptyList();
    when(verificationJobInstanceService.getProgressLogs(verificationJobInstanceId)).thenReturn(expectedExecutionLogs);
    List<VerificationJobInstance.ProgressLog> resultExecutionLogs =
        verificationJobInstanceService.getProgressLogs(callbackId);
    assertThat(expectedExecutionLogs).isEqualTo(resultExecutionLogs);
  }

  private CVNGStepTask get(String callbackId) {
    return hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.callbackId, callbackId).get();
  }
}
