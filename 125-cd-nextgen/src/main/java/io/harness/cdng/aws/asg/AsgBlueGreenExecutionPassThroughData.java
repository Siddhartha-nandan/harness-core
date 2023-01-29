/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.asg;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AsgLoadBalancerConfig;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.logstreaming.UnitProgressData;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Getter
@NoArgsConstructor
@OwnedBy(CDP)
@TypeAlias("asgBlueGreenExecutionPassThroughData")
@RecasterAlias("io.harness.cdng.aws.asg.AsgBlueGreenExecutionPassThroughData")
public class AsgBlueGreenExecutionPassThroughData extends AsgExecutionPassThroughData {
  AsgLoadBalancerConfig loadBalancerConfig;
  String asgName;
  boolean firstDeployment;

  @Builder(builderMethodName = "blueGreenBuilder")
  public AsgBlueGreenExecutionPassThroughData(InfrastructureOutcome infrastructure,
      UnitProgressData lastActiveUnitProgressData, AsgManifestFetchData asgManifestFetchData,
      AsgLoadBalancerConfig loadBalancerConfig, String asgName, boolean firstDeployment) {
    super(infrastructure, lastActiveUnitProgressData, asgManifestFetchData);
    this.loadBalancerConfig = loadBalancerConfig;
    this.asgName = asgName;
    this.firstDeployment = firstDeployment;
  }
}
