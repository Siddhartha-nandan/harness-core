/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifest;

import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.ngmigration.beans.ManifestProvidedEntitySpec;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.ngmigration.service.entity.ManifestMigrationService;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.NGMigrationEntityType;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class ValuesManifestRemoteStoreService implements NgManifestService {
  @Inject ManifestMigrationService manifestMigrationService;

  @Override
  public ManifestConfigWrapper getManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, NGYamlFile> migratedEntities,
      ManifestProvidedEntitySpec entitySpec, List<NGYamlFile> yamlFileList) {
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    NgEntityDetail connector =
        migratedEntities
            .get(CgEntityId.builder().id(gitFileConfig.getConnectorId()).type(NGMigrationEntityType.CONNECTOR).build())
            .getNgEntityDetail();

    ValuesManifest valuesManifest =
        ValuesManifest.builder()
            .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
            .store(ParameterField.createValueField(
                StoreConfigWrapper.builder()
                    .type(StoreConfigType.GIT)
                    .spec(manifestMigrationService.getGitStore(gitFileConfig, entitySpec, connector))
                    .build()))
            .build();
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder()
                      .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
                      .type(ManifestConfigType.VALUES)
                      .spec(valuesManifest)
                      .build())
        .build();
  }
}
