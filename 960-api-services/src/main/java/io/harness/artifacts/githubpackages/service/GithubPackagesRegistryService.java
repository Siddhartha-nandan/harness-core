/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.githubpackages.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.githubpackages.beans.GithubPackagesInternalConfig;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface GithubPackagesRegistryService {
  /**
   * Get builds
   */
  List<BuildDetails> getBuilds(
      GithubPackagesInternalConfig githubPackagesInternalConfig, String packageName, int maxNoOfTagsPerImage);

  /**
   * Get last successful build
   */
  BuildDetails getLastSuccessfulBuildFromRegex(
      GithubPackagesInternalConfig toGithubPackagesInternalConfig, String packageName, String versionRegex);

  /**
   * Get build
   */
  BuildDetails getBuild(
      GithubPackagesInternalConfig toGithubPackagesInternalConfig, String packageName, String version);
}
