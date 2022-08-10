/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants;

public enum BitbucketHttpAuthenticationType {
  @JsonProperty(BitbucketConnectorConstants.USERNAME_AND_PASSWORD)
  USERNAME_AND_PASSWORD(BitbucketConnectorConstants.USERNAME_AND_PASSWORD),
  @JsonProperty(BitbucketConnectorConstants.OAUTH) OAUTH(BitbucketConnectorConstants.OAUTH);

  private final String displayName;

  BitbucketHttpAuthenticationType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
