/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.ecs.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@TypeAlias("ecsBasicPrepareRollbackDataOutcome")
@JsonTypeName("ecsBasicPrepareRollbackDataOutcome")
@RecasterAlias("io.harness.cdng.ecs.EcsBasicPrepareRollbackDataOutcome")
public class EcsBasicPrepareRollbackDataOutcome implements ExecutionSweepingOutput {
  String oldServiceName;
  String createServiceRequestYaml;
  List<String> registerScalableTargetRequestYaml;
  List<String> registerScalingPolicyRequestYaml;
  boolean isFirstDeployment;
  String newServiceName;
}
