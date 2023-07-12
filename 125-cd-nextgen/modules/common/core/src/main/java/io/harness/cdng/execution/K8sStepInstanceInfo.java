/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.execution;

import io.harness.annotation.RecasterAlias;
import io.harness.delegate.cdng.execution.StepInstanceInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RecasterAlias("io.harness.cdng.execution.K8sStepInstanceInfo")
public class K8sStepInstanceInfo implements StepInstanceInfo {
  String podName;

  @Override
  public String getName() {
    return podName;
  }
}
