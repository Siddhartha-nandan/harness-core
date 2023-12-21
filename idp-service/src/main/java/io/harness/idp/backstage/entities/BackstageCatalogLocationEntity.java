/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.beans.BackstageCatalogEntityTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.IDP)
public class BackstageCatalogLocationEntity extends BackstageCatalogEntity {
  private Spec spec;

  public BackstageCatalogLocationEntity() {
    super.setKind(BackstageCatalogEntityTypes.LOCATION.kind);
  }

  public BackstageCatalogLocationEntity(Spec spec) {
    super.setKind(BackstageCatalogEntityTypes.LOCATION.kind);
    this.spec = spec;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Spec {
    private String type;
    private String target;
    private Object targets;
    private Object presence;
  }
}
