/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.beans.source.webhook.v2.github.action;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum GithubReleaseAction implements GitAction {
  @JsonProperty("Create") CREATE("create", "Create"),
  @JsonProperty("Edit") EDIT("edit", "Edit"),
  @JsonProperty("Delete") DELETE("delete", "Delete"),
  @JsonProperty("Prerelease") PRERELEASE("prerelease", "Prerelease"),
  @JsonProperty("Publish") PUBLISH("publish", "Publish"),
  @JsonProperty("Release") RELEASE("release", "Release"),
  @JsonProperty("Unpublish") UNPUBLISH("unpublish", "Unpublish");

  private String value;
  private String parsedValue;

  GithubReleaseAction(String parsedValue, String value) {
    this.parsedValue = parsedValue;
    this.value = value;
  }

  public String getParsedValue() {
    return parsedValue;
  }

  public String getValue() {
    return value;
  }
}
