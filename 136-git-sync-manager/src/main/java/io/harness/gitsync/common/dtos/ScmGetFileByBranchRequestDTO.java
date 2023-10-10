/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.common.beans.GitXSettingsParams;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ScmGetFileByBranchRequestDTO {
  Scope scope;
  String repoName;
  String branchName;
  String commitId;
  String filePath;
  String connectorRef;
  boolean useCache;
  boolean getOnlyFileContent;
  // If ScmConnector is not null, then we use it instead of processing connectorRef
  ScmConnector scmConnector;
  GitXSettingsParams gitXSettingsParams;
  boolean isGetFileFlow;
}
