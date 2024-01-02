/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;

@Getter
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "Keys")
@OwnedBy(HarnessTeam.CDC)
public class CountByServiceIdAndEnvId {
  private String serviceIdentifier;
  private String envIdentifier;
  private int count;

  public static ProjectionOperation getProjection() {
    return Aggregation.project()
        .andExpression("_id." + Keys.serviceIdentifier)
        .as(Keys.serviceIdentifier)
        .andExpression("_id." + Keys.envIdentifier)
        .as(Keys.envIdentifier)
        .andExpression(Keys.count)
        .as(Keys.count);
  }
}
