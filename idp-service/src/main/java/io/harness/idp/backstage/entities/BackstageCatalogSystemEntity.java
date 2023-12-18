/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.entities;

import static io.harness.idp.backstage.Constants.ENTITY_UNKNOWN_OWNER;
import static io.harness.idp.backstage.Constants.PIPE_DELIMITER;
import static io.harness.idp.backstage.Constants.PROJECT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.beans.BackstageCatalogEntityTypes;
import io.harness.spec.server.idp.v1.model.HarnessBackstageEntities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.stream.Collectors;
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
public class BackstageCatalogSystemEntity extends BackstageCatalogEntity {
  private Spec spec;

  public BackstageCatalogSystemEntity() {
    super.setKind(BackstageCatalogEntityTypes.SYSTEM.kind);
  }

  public BackstageCatalogSystemEntity(Spec spec) {
    super.setKind(BackstageCatalogEntityTypes.SYSTEM.kind);
    this.spec = spec;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Spec {
    private String owner;
    private String domain;
  }

  public static List<HarnessBackstageEntities> map(List<BackstageCatalogSystemEntity> backstageCatalogSystemEntities) {
    return backstageCatalogSystemEntities.stream()
        .map(BackstageCatalogSystemEntity::convert)
        .collect(Collectors.toList());
  }

  private static HarnessBackstageEntities convert(BackstageCatalogSystemEntity backstageCatalogSystemEntity) {
    HarnessBackstageEntities idpHarnessProjectEntity = new HarnessBackstageEntities();

    idpHarnessProjectEntity.setIdentifier(backstageCatalogSystemEntity.getSpec().getDomain() + PIPE_DELIMITER
        + backstageCatalogSystemEntity.getMetadata().getIdentifier());
    idpHarnessProjectEntity.setEntityType(PROJECT);
    idpHarnessProjectEntity.setName(backstageCatalogSystemEntity.getMetadata().getName());
    idpHarnessProjectEntity.setType(PROJECT);
    idpHarnessProjectEntity.setOwner(ENTITY_UNKNOWN_OWNER);
    idpHarnessProjectEntity.setSystem(backstageCatalogSystemEntity.getSpec().getDomain());

    return idpHarnessProjectEntity;
  }
}
