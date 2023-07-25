/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SbomOrchestrationToolType {
  @JsonProperty(SbomToolConstants.SYFT) SYFT(SbomToolConstants.SYFT),
  @JsonProperty(SbomToolConstants.BLACKDUCK) BLACKDUCK(SbomToolConstants.BLACKDUCK);

  private final String name;

  SbomOrchestrationToolType(String name) {
    this.name = name;
  }

  @Override
  @JsonValue
  public String toString() {
    return this.name;
  }
}
