/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.gitsync;

import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.ngsettings.client.remote.NGSettingsClient;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;

public class GitExperienceSettingsHelper {
  @Inject private NGSettingsClient ngSettingsClient;
  public boolean isGitExperienceEnforcedInSettings(String accountId, String orgIdentifier, String projIdentifier) {
    String isGitExperienceEnforced =
        NGRestUtils
            .getResponse(ngSettingsClient.getSetting(
                GitSyncConstants.ENFORCE_GIT_EXPERIENCE, accountId, orgIdentifier, projIdentifier))
            .getValue();
    return GitSyncConstants.TRUE_VALUE.equals(isGitExperienceEnforced);
  }
}
