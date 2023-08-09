/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.api;

import io.harness.cdng.artifact.outcome.ArtifactsOutcome;
import io.harness.cvng.analysis.entities.SRMAnalysisStepDetailDTO;
import io.harness.cvng.analysis.entities.SRMAnalysisStepExecutionDetail;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.time.Duration;
import java.util.Optional;

public interface SRMAnalysisStepService {
  String createSRMAnalysisStepExecution(Ambiance ambiance, String monitoredServiceIdentifier, String stepName,
      ServiceEnvironmentParams serviceEnvironmentParams, Duration duration,
      Optional<ArtifactsOutcome> optionalArtifactsOutcome);

  SRMAnalysisStepExecutionDetail getSRMAnalysisStepExecutionDetail(String analysisStepExecutionDetailId);

  void abortRunningStepsForMonitoredService(ProjectParams projectParams, String monitoredServiceIdentifier);

  SRMAnalysisStepDetailDTO abortRunningSrmAnalysisStep(String executionDetailId);

  void completeSrmAnalysisStep(SRMAnalysisStepExecutionDetail stepExecutionDetail);

  SRMAnalysisStepDetailDTO getSRMAnalysisSummary(String executionDetailId);

  void handleReportNotification(SRMAnalysisStepExecutionDetail stepExecutionDetail);
}
