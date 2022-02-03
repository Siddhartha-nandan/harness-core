/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless.request;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.serverless.ServerlessCliVersion;
import io.harness.delegate.task.serverless.ServerlessCommandType;

import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
public interface ServerlessCommandRequest extends TaskParameters {
  String getAccountId();
  String getAppId();
  String getActivityId();
  @NotEmpty ServerlessCommandType getServerlessCommandType();
  String getCommandName();
  ServerlessCliVersion getServerlessCliVersion();
}
