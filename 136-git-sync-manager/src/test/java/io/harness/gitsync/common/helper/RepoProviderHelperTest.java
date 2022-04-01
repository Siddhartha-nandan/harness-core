/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.gitsync.common.dtos.RepoProviders.BITBUCKET;
import static io.harness.gitsync.common.dtos.RepoProviders.GITHUB;
import static io.harness.gitsync.common.dtos.RepoProviders.GITLAB;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnsupportedOperationException;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class RepoProviderHelperTest extends CategoryTest {
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getTheFilePathUrlTest() throws IOException {
    // Testing for Github
    String filePathUrl = RepoProviderHelper.getTheFilePathUrl(
        "https://github.com/harness/harness-core", "master", GITHUB, "/.harness/Pipeline1.yaml");
    assertThat(filePathUrl).isEqualTo("https://github.com/harness/harness-core/blob/master/.harness/Pipeline1.yaml");

    String filePathUrlForGithubEnterprise = RepoProviderHelper.getTheFilePathUrl(
        "https://harness.github.com/harness/harness-core", "master", GITHUB, "/.harness/Pipeline1.yaml");
    assertThat(filePathUrlForGithubEnterprise)
        .isEqualTo("https://harness.github.com/harness/harness-core/blob/master/.harness/Pipeline1.yaml");

    // Testing for Bitbucket
    String bitbucketFilePathUrl = RepoProviderHelper.getTheFilePathUrl(
        "https://bitbucket.org/deepakpatankar/git-sync-test", "master", BITBUCKET, "bitbucket/.harness/abcd.yaml");
    assertThat(bitbucketFilePathUrl)
        .isEqualTo("https://bitbucket.org/deepakpatankar/git-sync-test/src/master/bitbucket/.harness/abcd.yaml");

    assertThatThrownBy(()
                           -> RepoProviderHelper.getTheFilePathUrl("https://bitbucket.org/deepakpatankar/git-sync-test",
                               "master", GITLAB, "/.harness/abcd.yaml"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getRepoProviderFromTheUrlTest() throws IOException {
    RepoProviders repoProviderFromBitbucketUrl =
        RepoProviderHelper.getRepoProviderFromTheUrl("https://bitbucket.org/deepakpatankar/git-sync-test");
    assertThat(repoProviderFromBitbucketUrl).isEqualTo(BITBUCKET);

    RepoProviders repoProviderFromGithubUrl =
        RepoProviderHelper.getRepoProviderFromTheUrl("https://github.com/harness/harness-core");
    assertThat(repoProviderFromGithubUrl).isEqualTo(GITHUB);
  }
}
