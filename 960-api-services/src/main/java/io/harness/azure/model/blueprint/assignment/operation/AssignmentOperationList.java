/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.model.blueprint.assignment.operation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.azure.core.http.rest.Page;
import com.azure.core.util.IterableStream;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDP)
public class AssignmentOperationList implements Page<AssignmentOperation> {
  private String nextLink;
  private List<AssignmentOperation> value;

  @Override
  public IterableStream getElements() {
    return IterableStream.of(value);
  }

  @Override
  public String getContinuationToken() {
    return nextLink;
  }
}
