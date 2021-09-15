/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class InfoForGitPush {
  ScmConnector scmConnector;
  String folderPath;
  String filePath;
  String branch;
  boolean isNewBranch;
  String baseBranch;
  String accountId;
  String projectIdentifier;
  String orgIdentifier;
  String commitMsg;
  String oldFileSha;
  String yaml;
}
