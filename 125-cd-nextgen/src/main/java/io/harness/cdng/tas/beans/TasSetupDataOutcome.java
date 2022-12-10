/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.beans.pcf.TasApplicationInfo;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@TypeAlias("TasSetupDataOutcome")
@JsonTypeName("TasSetupDataOutcome")
@RecasterAlias("io.harness.cdng.tas.beans.TasSetupDataOutcome")
public class TasSetupDataOutcome implements Outcome, ExecutionSweepingOutput {
  Integer totalPreviousInstanceCount;
  CfAppSetupTimeDetails downsizeAppDetail;
  Integer desiredActualFinalCount;
  String newReleaseName;
  Integer maxCount;
  List<CfServiceData> instanceData;
  ResizeStrategy resizeStrategy;
  String cfAppNamePrefix;
  CfCliVersion cfCliVersion;
  Integer timeoutIntervalInMinutes;
  TasApplicationInfo oldApplicationDetails;
  TasApplicationInfo newApplicationDetails;
  List<String> tempRouteMap;
  List<String> routeMaps;
  TasApplicationInfo existingApplicationDetails;
  @Builder.Default Boolean isBlueGreen = Boolean.FALSE;

  boolean useAppAutoscalar;
}
