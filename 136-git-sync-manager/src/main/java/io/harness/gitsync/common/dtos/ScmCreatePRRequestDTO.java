/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.gitsync.beans.GitRepoScopeParams;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ScmCreatePRRequestDTO {
  Scope scope;
  String sourceBranch;
  String targetBranch;
  String title;
  String connectorRef;
  String repoName;
  GitRepoScopeParams gitRepoScopeParams;
}
