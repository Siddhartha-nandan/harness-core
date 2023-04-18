/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.beans.dto;

import io.harness.assessment.settings.beans.entities.AssessmentResponseStatus;
import io.harness.assessment.settings.beans.entities.Score;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentResultsResponse {
  String assessmentId;
  String assessmentName;
  Long majorVersion;
  Long minorVersion;
  AssessmentResponseStatus status;
  List<UserResponsesResponse> responses;
  List<Score> userScores;
  List<Score> organizationScores;
  List<BenchmarkDTO> benchmarks;
  ScoreOverviewDTO scoreOverview;
  String resultLink;
}
