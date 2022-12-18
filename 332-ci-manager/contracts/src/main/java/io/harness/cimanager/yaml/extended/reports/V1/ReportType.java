/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended.reports.V1;

import io.harness.beans.yaml.extended.reports.UnitTestReportType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportType {
  @JsonProperty("junit")
  JUNIT("junit") {
    @Override
    public UnitTestReportType toUnitTestReportType() {
      return UnitTestReportType.JUNIT;
    }
  };

  private final String yamlName;

  ReportType(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  public abstract UnitTestReportType toUnitTestReportType();
}
