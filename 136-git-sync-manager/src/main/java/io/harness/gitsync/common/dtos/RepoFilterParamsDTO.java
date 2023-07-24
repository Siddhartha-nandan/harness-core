/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Getter
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Schema(name = "RepoFilterParamsDTO", description = "This contains repo filter params")
@OwnedBy(PIPELINE)
public class RepoFilterParamsDTO {
  @Parameter(description = GitSyncApiConstants.REPO_NAME_SEARCH_TERM_PARAM_MESSAGE)
  @QueryParam(NGCommonEntityConstants.REPO_NAME_SEARCH_TERM)
  String repoName;

  @Parameter(description = GitSyncApiConstants.USER_NAME_SEARCH_TERM_PARAM_MESSAGE)
  @QueryParam(NGCommonEntityConstants.USER_NAME_SEARCH_TERM)
  String userName;
}
