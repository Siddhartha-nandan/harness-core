/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.entities;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@StoreIn(DbAliases.IDP)
@FieldNameConstants(innerTypeName = "DefaultPluginInfoEntityKeys")
@Persistent
@OwnedBy(HarnessTeam.IDP)
@TypeAlias("io.harness.idp.plugin.entities.DefaultPluginInfoEntity")
public class DefaultPluginInfoEntity extends PluginInfoEntity {
  @Builder.Default private boolean core = false;
}
