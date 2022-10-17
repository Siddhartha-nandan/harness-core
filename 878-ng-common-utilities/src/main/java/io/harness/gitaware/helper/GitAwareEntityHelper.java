/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitaware.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.gitaware.dto.GitContextRequestParams;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmCreateFileGitRequest;
import io.harness.gitsync.scm.beans.ScmCreateFileGitResponse;
import io.harness.gitsync.scm.beans.ScmGetFileResponse;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.scm.beans.ScmUpdateFileGitRequest;
import io.harness.gitsync.scm.beans.ScmUpdateFileGitResponse;
import io.harness.persistence.gitaware.GitAware;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import groovy.lang.Singleton;
import java.util.Collections;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class GitAwareEntityHelper {
  @Inject SCMGitSyncHelper scmGitSyncHelper;
  public static final String DEFAULT = "__default__";
  public static final String HARNESS_FOLDER_EXTENSION_WITH_SEPARATOR = ".harness/";
  public static final String FILE_PATH_INVALID_HINT = "Please check if the requested filepath is valid.";
  public static final String FILE_PATH_INVALID_EXTENSION_EXPLANATION =
      "Harness File should have [.yaml] or [.yml] extension.";

  public static final String FILE_PATH_INVALID_EXTENSION_ERROR_FORMAT = "FilePath [%s] doesn't have right extension.";

  public static final String FILE_PATH_SEPARATOR = "/";
  public static final String NULL_FILE_PATH_ERROR_MESSAGE = "FilePath cannot be null or empty.";
  public static final String INVALID_FILE_PATH_FORMAT_ERROR_MESSAGE = "FilePath [%s] should not start or end with [/].";

  public GitAware fetchEntityFromRemote(
      GitAware entity, Scope scope, GitContextRequestParams gitContextRequestParams, Map<String, String> contextMap) {
    String repoName = gitContextRequestParams.getRepoName();
    // if branch is empty, then git sdk will figure out the default branch for the repo by itself
    String branch =
        isNullOrDefault(gitContextRequestParams.getBranchName()) ? "" : gitContextRequestParams.getBranchName();
    String filePath = gitContextRequestParams.getFilePath();
    String connectorRef = gitContextRequestParams.getConnectorRef();
    validateFilePath(gitContextRequestParams.getFilePath());
    ScmGetFileResponse scmGetFileResponse =
        scmGitSyncHelper.getFileByBranch(Scope.builder()
                                             .accountIdentifier(scope.getAccountIdentifier())
                                             .orgIdentifier(scope.getOrgIdentifier())
                                             .projectIdentifier(scope.getProjectIdentifier())
                                             .build(),
            repoName, branch, filePath, connectorRef, contextMap);
    entity.setData(scmGetFileResponse.getFileContent());
    GitAwareContextHelper.updateScmGitMetaData(scmGetFileResponse.getGitMetaData());
    return entity;
  }

  // todo: make pipeline import call this method too
  public String fetchYAMLFromRemote(String accountId, String orgIdentifier, String projectIdentifier) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    Scope scope = Scope.of(accountId, orgIdentifier, projectIdentifier);
    GitContextRequestParams gitContextRequestParams = GitContextRequestParams.builder()
                                                          .branchName(gitEntityInfo.getBranch())
                                                          .connectorRef(gitEntityInfo.getConnectorRef())
                                                          .filePath(gitEntityInfo.getFilePath())
                                                          .repoName(gitEntityInfo.getRepoName())
                                                          .build();
    return fetchYAMLFromRemote(scope, gitContextRequestParams, Collections.emptyMap());
  }

  public String fetchYAMLFromRemote(
      Scope scope, GitContextRequestParams gitContextRequestParams, Map<String, String> contextMap) {
    String repoName = gitContextRequestParams.getRepoName();
    if (isNullOrDefault(repoName)) {
      throw new InvalidRequestException("No Repo Name provided.");
    }
    // if branch is empty, then git sdk will figure out the default branch for the repo by itself
    String branch =
        isNullOrDefault(gitContextRequestParams.getBranchName()) ? "" : gitContextRequestParams.getBranchName();
    String filePath = gitContextRequestParams.getFilePath();
    if (isNullOrDefault(filePath)) {
      throw new InvalidRequestException("No file path provided.");
    }
    String connectorRef = gitContextRequestParams.getConnectorRef();
    if (isNullOrDefault(connectorRef)) {
      throw new InvalidRequestException("No Connector reference provided.");
    }
    ScmGetFileResponse scmGetFileResponse =
        scmGitSyncHelper.getFileByBranch(Scope.builder()
                                             .accountIdentifier(scope.getAccountIdentifier())
                                             .orgIdentifier(scope.getOrgIdentifier())
                                             .projectIdentifier(scope.getProjectIdentifier())
                                             .build(),
            repoName, branch, filePath, connectorRef, contextMap);
    GitAwareContextHelper.updateScmGitMetaData(scmGetFileResponse.getGitMetaData());
    return scmGetFileResponse.getFileContent();
  }

  public ScmCreateFileGitResponse createEntityOnGit(GitAware gitAwareEntity, String yaml, Scope scope) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    String repoName = gitAwareEntity.getRepo();
    if (isNullOrDefault(repoName)) {
      throw new InvalidRequestException("No repo name provided.");
    }
    String filePath = gitAwareEntity.getFilePath();
    if (isNullOrDefault(filePath)) {
      throw new InvalidRequestException("No file path provided.");
    }
    String connectorRef = gitAwareEntity.getConnectorRef();
    if (isNullOrDefault(connectorRef)) {
      throw new InvalidRequestException("No Connector reference provided.");
    }
    String baseBranch = gitEntityInfo.getBaseBranch();
    if (gitEntityInfo.isNewBranch() && isNullOrDefault(baseBranch)) {
      throw new InvalidRequestException("No base branch provided for committing to new branch");
    }
    validateFilePath(gitEntityInfo.getFilePath());
    // if branch is empty, then git sdk will figure out the default branch for the repo by itself
    String branch = isNullOrDefault(gitEntityInfo.getBranch()) ? "" : gitEntityInfo.getBranch();
    // if commitMsg is empty, then git sdk will use some default Commit Message
    String commitMsg = isNullOrDefault(gitEntityInfo.getCommitMsg()) ? "" : gitEntityInfo.getCommitMsg();
    ScmCreateFileGitRequest scmCreateFileGitRequest = ScmCreateFileGitRequest.builder()
                                                          .repoName(repoName)
                                                          .branchName(branch)
                                                          .fileContent(yaml)
                                                          .filePath(filePath)
                                                          .connectorRef(connectorRef)
                                                          .isCommitToNewBranch(gitEntityInfo.isNewBranch())
                                                          .commitMessage(commitMsg)
                                                          .baseBranch(baseBranch)
                                                          .build();

    ScmCreateFileGitResponse scmCreateFileGitResponse =
        scmGitSyncHelper.createFile(scope, scmCreateFileGitRequest, Collections.emptyMap());
    GitAwareContextHelper.updateScmGitMetaData(scmCreateFileGitResponse.getGitMetaData());
    return scmCreateFileGitResponse;
  }

  public ScmUpdateFileGitResponse updateEntityOnGit(GitAware gitAwareEntity, String yaml, Scope scope) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    return updateEntityOnGit(
        gitAwareEntity, yaml, scope, gitEntityInfo.getLastObjectId(), gitEntityInfo.getLastCommitId());
  }

  public ScmUpdateFileGitResponse updateFileImportedFromGit(GitAware gitAwareEntity, String yaml, Scope scope) {
    ScmGitMetaData scmGitMetaData = GitAwareContextHelper.getScmGitMetaData();
    return updateEntityOnGit(gitAwareEntity, yaml, scope, scmGitMetaData.getBlobId(), scmGitMetaData.getCommitId());
  }

  ScmUpdateFileGitResponse updateEntityOnGit(
      GitAware gitAwareEntity, String yaml, Scope scope, String oldFileSHA, String oldCommitID) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    String repoName = gitAwareEntity.getRepo();
    if (isNullOrDefault(repoName)) {
      throw new InvalidRequestException("No repo name provided.");
    }
    String filePath = gitAwareEntity.getFilePath();
    if (isNullOrDefault(filePath)) {
      throw new InvalidRequestException("No file path provided.");
    }
    String connectorRef = gitAwareEntity.getConnectorRef();
    if (isNullOrDefault(connectorRef)) {
      throw new InvalidRequestException("No Connector reference provided.");
    }
    String baseBranch = gitEntityInfo.getBaseBranch();
    if (gitEntityInfo.isNewBranch() && isNullOrDefault(baseBranch)) {
      throw new InvalidRequestException("No base branch provided for committing to new branch");
    }
    // if branch is empty, then git sdk will figure out the default branch for the repo by itself
    String branch = gitEntityInfo.getBranch();
    if (isNullOrDefault(branch)) {
      throw new InvalidRequestException("No branch provided for updating the file.");
    }
    validateFilePath(gitEntityInfo.getFilePath());
    // if commitMsg is empty, then git sdk will use some default Commit Message
    String commitMsg = isNullOrDefault(gitEntityInfo.getCommitMsg()) ? "" : gitEntityInfo.getCommitMsg();
    ScmUpdateFileGitRequest scmUpdateFileGitRequest = ScmUpdateFileGitRequest.builder()
                                                          .repoName(repoName)
                                                          .branchName(branch)
                                                          .fileContent(yaml)
                                                          .filePath(filePath)
                                                          .connectorRef(connectorRef)
                                                          .isCommitToNewBranch(gitEntityInfo.isNewBranch())
                                                          .commitMessage(commitMsg)
                                                          .baseBranch(baseBranch)
                                                          .oldFileSha(oldFileSHA)
                                                          .oldCommitId(oldCommitID)
                                                          .build();

    ScmUpdateFileGitResponse scmUpdateFileGitResponse =
        scmGitSyncHelper.updateFile(scope, scmUpdateFileGitRequest, Collections.emptyMap());
    GitAwareContextHelper.updateScmGitMetaData(scmUpdateFileGitResponse.getGitMetaData());
    return scmUpdateFileGitResponse;
  }

  private boolean isNullOrDefault(String val) {
    return EmptyPredicate.isEmpty(val) || val.equals(DEFAULT);
  }

  public String getRepoUrl(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    Scope scope = Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
    return scmGitSyncHelper
        .getRepoUrl(scope, gitEntityInfo.getRepoName(), gitEntityInfo.getConnectorRef(), Collections.emptyMap())
        .getRepoUrl();
  }

  @VisibleForTesting
  void validateFilePath(String filePath) {
    validateFilePathFormat(filePath);
    validateFilePathHasCorrectExtension(filePath);
  }

  private void validateFilePathHasCorrectExtension(String filePath) {
    if (!filePath.endsWith(".yaml") && !filePath.endsWith(".yml")) {
      throw NestedExceptionUtils.hintWithExplanationException(FILE_PATH_INVALID_HINT,
          FILE_PATH_INVALID_EXTENSION_EXPLANATION,
          new InvalidRequestException(String.format(FILE_PATH_INVALID_EXTENSION_ERROR_FORMAT, filePath)));
    }
  }

  private static void validateFilePathFormat(String filePath) {
    if (isEmpty(filePath)) {
      throw new InvalidRequestException(NULL_FILE_PATH_ERROR_MESSAGE);
    }
    if (filePath.startsWith(FILE_PATH_SEPARATOR) || filePath.endsWith(FILE_PATH_SEPARATOR)) {
      throw new InvalidRequestException(String.format(INVALID_FILE_PATH_FORMAT_ERROR_MESSAGE, filePath));
    }
  }
}
