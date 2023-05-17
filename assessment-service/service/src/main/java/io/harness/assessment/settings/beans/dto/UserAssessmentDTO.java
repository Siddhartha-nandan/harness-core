/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.beans.dto;

import io.harness.assessment.settings.beans.entities.AssessmentResponseStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAssessmentDTO {
  String assessmentId;
  String assessmentName;
  Long majorVersion;
  Long minorVersion;
  List<QuestionResponse> questions;
  Map<String, List<QuestionResponse>> sectionQuestions;
  Long expectedCompletionDuration;
  Long baseScore;
  List<UserResponseRequestItem> userResponse;
  AssessmentResponseStatus status;
  String resultLink;
}
