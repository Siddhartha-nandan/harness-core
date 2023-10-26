/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.http.v1;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.steps.http.HttpBaseOutcome;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDC)
@RecasterAlias("io.harness.steps.http.v1.HttpOutcome")
public class HttpOutcome extends HttpBaseOutcome {
  String url;
  String method;
  int response_code;
  String response_body;
  CommandExecutionStatus status;
  String error_msg;
  Map<String, String> output_vars;
}
