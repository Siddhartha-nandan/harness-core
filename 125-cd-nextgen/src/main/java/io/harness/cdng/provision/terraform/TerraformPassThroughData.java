/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terraform;

import io.harness.annotation.RecasterAlias;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import io.harness.logging.UnitProgress;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformPassThroughData")
public class TerraformPassThroughData implements PassThroughData {
  @Accessors(fluent = true) boolean hasGitFiles;
  @Accessors(fluent = true) boolean hasS3Files;
  @Accessors(fluent = true) boolean skipTerraformRollback;
  @Builder.Default List<UnitProgress> unitProgresses = new ArrayList<>();
  @Builder.Default Map<String, String> fetchedCommitIdsMap = new HashMap<>();
  @Builder.Default Map<String, Map<String, String>> keyVersionMap = new HashMap<>();
  @Builder.Default List<String> remoteVarFilesContent = new ArrayList<>();
  @Builder.Default
  TerraformTaskNGParametersBuilder terraformTaskNGParametersBuilder = TerraformTaskNGParameters.builder();
}
