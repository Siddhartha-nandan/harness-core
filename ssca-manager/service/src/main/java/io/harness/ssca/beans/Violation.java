/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans;

import io.harness.ssca.enforcement.constants.ViolationType;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class Violation {
  List<String> artifactUuids;
  JsonNode rule;
  @Getter(AccessLevel.NONE) String type;
  @Getter(AccessLevel.NONE) String violationDetail;
  public String getViolationDetail() {
    // TODO: Make this more readable
    return rule.toString();
  }

  public String getType() {
    if (type.equals("allow")) {
      return ViolationType.ALLOWLIST_VIOLATION.getViolation();
    } else if (type.equals("deny")) {
      return ViolationType.DENYLIST_VIOLATION.getViolation();
    } else {
      return ViolationType.UNKNOWN_VIOLATION.getViolation();
    }
  }
}
