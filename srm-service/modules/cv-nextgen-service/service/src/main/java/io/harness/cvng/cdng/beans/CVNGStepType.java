/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.CV)
public enum CVNGStepType {
  CVNG_VERIFY("Continuous Verification", "Verify", "Verify"),

  CVNG_ANALYZE_DEPLOYMENT("Service Reliability", "Analyze Deployment Impact", "AnalyzeDeploymentImpact");
  private final String displayName;
  private final String folderPath;

  private final String type;

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  CVNGStepType(String folderPath, String displayName, String type) {
    this.folderPath = folderPath;
    this.displayName = displayName;
    this.type = type;
  }
  @JsonValue
  public String getFolderPath() {
    return this.folderPath;
  }

  @JsonValue
  public String getType() {
    return this.type;
  }
}
