/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.sto.variables;

import com.fasterxml.jackson.annotation.*;
import io.harness.annotation.*;
import org.springframework.data.annotation.*;

@TypeAlias("stoYamlTargetNamingMode")
@RecasterAlias("io.harness.yaml.sto.variables.STOYamlTargetNamingMode")
public enum STOYamlTargetNamingMode {
  @JsonProperty("manual") MANUAL("manual"),
  @JsonProperty("auto") AUTO("auto");

  private final String yamlName;

  STOYamlTargetNamingMode(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static STOYamlTargetNamingMode getValue(@JsonProperty("type") String yamlName) {
    for (STOYamlTargetNamingMode value : STOYamlTargetNamingMode.values()) {
      if (value.yamlName.equalsIgnoreCase(yamlName) || value.name().equalsIgnoreCase(yamlName)) {
        return value;
      }
    }

    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  @Override
  public String toString() {
    return yamlName;
  }

  public static STOYamlTargetNamingMode fromString(final String s) {
    return STOYamlTargetNamingMode.getValue(s);
  }
}
