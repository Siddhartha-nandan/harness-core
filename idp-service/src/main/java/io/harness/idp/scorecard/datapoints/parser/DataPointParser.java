/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.parser;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.IDP)
public interface DataPointParser {
  Object parseDataPoint(Map<String, Object> data, String dataPointIdentifier, Set<String> strings);

  default String extractInputValue(String expression) {
    return "INV";
  }

  String getReplaceKey();
}
