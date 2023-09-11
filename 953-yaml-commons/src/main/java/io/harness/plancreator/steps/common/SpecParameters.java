/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.List;

@OwnedBy(PIPELINE)
public interface SpecParameters {
  @JsonIgnore
  default SpecParameters getViewJsonObject() {
    return this;
  }

  @JsonIgnore
  default List<String> stepInputsKeyExclude() {
    return new LinkedList<>();
  }
}
