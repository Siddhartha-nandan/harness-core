/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.ecs.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.outcome.InfrastructureOutcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@TypeAlias("ecsPrepareRollbackDataPassThroughData")
@RecasterAlias("io.harness.cdng.ecs.beans.EcsPrepareRollbackDataPassThroughData")
public class EcsPrepareRollbackDataPassThroughData implements PassThroughData {
  String ecsTaskDefinitionManifestContent;
  String ecsServiceDefinitionManifestContent;
  List<String> ecsScalableTargetManifestContentList;
  List<String> ecsScalingPolicyManifestContentList;
  InfrastructureOutcome infrastructureOutcome;
  String targetGroupArnKey;
}
