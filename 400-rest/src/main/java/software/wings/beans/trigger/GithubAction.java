/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 8/5/18.
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public enum GithubAction {
  CLOSED("Closed", "closed"),
  EDITED("Edited", "edited"),
  OPENED("Opened", "opened"),
  REOPENED("Reopened", "reopened"),
  ASSIGNED("Assigned", "assigned"),
  UNASSIGNED("Unassigned", "unassigned"),
  LABELED("Labeled", "labeled"),
  UNLABELED("Unlabeled", "unlabeled"),
  SYNCHRONIZED("Synchronized", "synchronize"),
  REVIEW_REQUESTED("Review Requested", "review_requested"),
  REVIEW_REQUESTED_REMOVED("Review Request Removed", "review_request_removed"),
  PACKAGE_PUBLISHED("Package published", "package:published");

  private String displayName;
  private String value;

  GithubAction(String displayName, String value) {
    this.displayName = displayName;
    this.value = value;
    GitHubActionHolder.map.put(value, this);
  }

  private static class GitHubActionHolder { static Map<String, GithubAction> map = new HashMap<>(); }

  public static GithubAction find(String val) {
    GithubAction githubAction = GitHubActionHolder.map.get(val);
    if (githubAction == null) {
      throw new InvalidRequestException(String.format("Unsupported Github action %s.", val));
    }
    return githubAction;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getValue() {
    return value;
  }
}
