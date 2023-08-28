/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.entity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datasourcelocations.beans.DataSourceLocationType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("DirectHttp")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DirectHttpDataSourceLocationKeys")
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.IDP)
public class DirectHttpDataSourceLocationEntity extends HttpDataSourceLocationEntity {
  public DirectHttpDataSourceLocationEntity() {
    super(DataSourceLocationType.DIRECT_HTTP);
  }
}
