/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.steps.output;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@JsonTypeName("UnresolvedManifestsOutcome")
@TypeAlias("unresolvedManifestsOutcome")
@RecasterAlias("io.harness.cdng.manifest.steps.output.UnresolvedManifestsOutcome")
public class UnresolvedManifestsOutput implements ExecutionSweepingOutput {
  private ManifestsOutcome manifestsOutcome;
  private Map<String, String> taskIdMapping;
}
