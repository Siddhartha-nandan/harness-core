/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.CanaryAnalysisState;
import io.harness.cvng.statemachine.entities.HostSamplingState;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostSamplingStateExecutor extends AnalysisStateExecutor<HostSamplingState> {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private TimeSeriesRecordService timeSeriesRecordService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;

  @Override
  public AnalysisState execute(HostSamplingState analysisState) {
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(analysisState.getVerificationJobInstanceId());
    VerificationJob verificationJob = verificationJobInstance.getResolvedJob();
    Optional<TimeRange> preDeploymentTimeRange =
        verificationJob.getPreActivityTimeRange(verificationJobInstance.getDeploymentStartTime());
    List<TimeSeriesRecordDTO> preDeploymentTimeSeriesRecords =
        timeSeriesRecordService.getTimeSeriesRecordDTOs(analysisState.getInputs().getVerificationTaskId(),
            preDeploymentTimeRange.get().getStartTime(), preDeploymentTimeRange.get().getEndTime());

    List<TimeSeriesRecordDTO> postDeploymentTimeSeriesRecords =
        timeSeriesRecordService.getTimeSeriesRecordDTOs(analysisState.getInputs().getVerificationTaskId(),
            analysisState.getInputs().getStartTime(), analysisState.getInputs().getEndTime());

    Set<String> preDeploymentHosts =
        preDeploymentTimeSeriesRecords.stream().map(TimeSeriesRecordDTO::getHost).collect(Collectors.toSet());
    Set<String> postDeploymentHosts =
        postDeploymentTimeSeriesRecords.stream().map(TimeSeriesRecordDTO::getHost).collect(Collectors.toSet());

    for (TimeSeriesRecordDTO timeSeriesRecordDTO : preDeploymentTimeSeriesRecords) {
      preDeploymentHosts.add(timeSeriesRecordDTO.getHost());
    }
    for (TimeSeriesRecordDTO timeSeriesRecordDTO : postDeploymentTimeSeriesRecords) {
      postDeploymentHosts.add(timeSeriesRecordDTO.getHost());
    }

    // Case 1: v2 = postDeploymentHosts - (preDeploymentHosts and postDeploymentHosts intersection)
    // is None in that case test hosts is None and control data is postdeployment host
    // Case 2: v2 = postDeploymentHosts - (preDeploymentHosts and postDeploymentHosts intersection)
    // is not None and it's not equal to postDeployment Hosts
    // in that case control hosts are postDeployment old hosts, and test host are v2
    // Case 3: v2 = postDeploymentHosts - (preDeploymentHosts and postDeploymentHosts intersection)
    // is not None and is equal to postDeployment hosts
    // in that case test hosts is None and control hosts is postdeployment hosts
    Set<String> newHosts = new HashSet<>(postDeploymentHosts);
    Set<String> commonHosts = new HashSet<>(preDeploymentHosts);
    commonHosts.retainAll(postDeploymentHosts);
    newHosts.removeAll(commonHosts);
    AnalysisInput.AnalysisInputBuilder analysisInputBuilder = AnalysisInput.builder();
    switch (verificationJob.getType()) {
      case CANARY:
        // always canary
        analysisInputBuilder.learningEngineTaskType(LearningEngineTaskType.CANARY_METRIC);
        analysisState.setStatus(AnalysisStatus.RUNNING);
        if (newHosts.isEmpty()) {
          // predeployment nodes: n1, n2
          // postdeployment nodes: n1, n2
          Set<String> controlHosts = new HashSet<>(postDeploymentHosts);
          Set<String> testHosts = new HashSet<>();
          analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts).testHosts(testHosts);
          analysisState.setInputs(analysisInputBuilder.build());
        } else if (!newHosts.equals(postDeploymentHosts)) {
          // predeployment nodes: n1, n2
          // postdeployment nodes: n1, n2, n3
          Set<String> testHosts = new HashSet<>(newHosts);
          Set<String> controlHosts = new HashSet<>(postDeploymentHosts);
          controlHosts.removeAll(testHosts);
          analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts).testHosts(testHosts);
          analysisState.setInputs(analysisInputBuilder.build());
        } else {
          // predeployment nodes: n1, n2
          // postdeploymnet nodes: n3, n4
          Set<String> testHosts = new HashSet<>();
          Set<String> controlHosts = new HashSet<>();
          analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts).testHosts(testHosts);
          analysisState.setInputs(analysisInputBuilder.build());
        }
        break;
      case ROLLING:
      case BLUE_GREEN:
        // always improvised canary
        analysisState.setLearningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
        analysisInputBuilder.learningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
        Set<String> controlHosts = new HashSet<>(preDeploymentHosts);
        Set<String> testHosts = new HashSet<>(postDeploymentHosts);
        analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts).testHosts(testHosts);
        analysisState.setInputs(analysisInputBuilder.build());
        break;
      case AUTO:
        if (newHosts.isEmpty()) {
          // it's before after
          // predeployment nodes: n1, n2
          // postdeployment nodes: n1, n2
          controlHosts = new HashSet<>(preDeploymentHosts);
          testHosts = new HashSet<>(postDeploymentHosts);
          analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts)
                                     .testHosts(testHosts)
                                     .learningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
          analysisState.setInputs(analysisInputBuilder.build());
          analysisState.setLearningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
        } else {
          // predeployment nodes: n1, n2 (or n1, n2)
          // postdeployment nodes: n1, n2, n3 (or n3, n4)
          controlHosts = new HashSet<>(preDeploymentHosts);
          testHosts = new HashSet<>(newHosts);
          analysisState.setTestHosts(testHosts);
          analysisState.setControlHosts(controlHosts);
          analysisInputBuilder = analysisInputBuilder.controlHosts(controlHosts).testHosts(testHosts);
          if (newHosts.equals(postDeploymentHosts)) {
            analysisState.setLearningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
            analysisInputBuilder.learningEngineTaskType(LearningEngineTaskType.BEFORE_AFTER_DEPLOYMENT_METRIC);
            analysisState.setInputs(analysisInputBuilder.build());
          } else {
            analysisState.setLearningEngineTaskType(LearningEngineTaskType.CANARY_METRIC);
            analysisInputBuilder.learningEngineTaskType(LearningEngineTaskType.CANARY_METRIC);
            analysisState.setInputs(analysisInputBuilder.build());
          }
        }
        break;
      default:
        log.warn("Unrecognized verification job type.");
    }
    return analysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus(HostSamplingState analysisState) {
    if (!analysisState.getControlHosts().isEmpty() || !analysisState.getTestHosts().isEmpty()) {
      return AnalysisStatus.TRANSITION;
    }
    return AnalysisStatus.RUNNING;
  }

  @Override
  public AnalysisState handleRerun(HostSamplingState analysisState) {
    analysisState.setControlHosts(new HashSet<>());
    analysisState.setTestHosts(new HashSet<>());
    analysisState.setRetryCount(analysisState.getRetryCount() + 1);
    analysisState.setStatus(AnalysisStatus.RUNNING);
    return execute(analysisState);
  }

  @Override
  public AnalysisState handleRunning(HostSamplingState analysisState) {
    return analysisState;
  }

  @Override
  public AnalysisState handleSuccess(HostSamplingState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleTransition(HostSamplingState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    CanaryAnalysisState canaryAnalysisState = new CanaryAnalysisState();
    canaryAnalysisState.setInputs(analysisState.getInputs());
    return canaryAnalysisState;
  }

  @Override
  public AnalysisState handleRetry(HostSamplingState analysisState) {
    if (analysisState.getRetryCount() >= getMaxRetry()) {
      analysisState.setStatus(AnalysisStatus.FAILED);
    } else {
      return handleRerun(analysisState);
    }
    return analysisState;
  }
}
