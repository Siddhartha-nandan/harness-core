/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngmigration.beans.FileYamlDTO;
import io.harness.ngmigration.beans.ManifestProvidedEntitySpec;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.entity.ManifestMigrationService;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class AzureAppServiceLocalStoreService implements NgManifestService {
  @Inject ManifestMigrationService manifestMigrationService;
  @Override
  public List<ManifestConfigWrapper> getManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, NGYamlFile> migratedEntities,
      ManifestProvidedEntitySpec entitySpec, List<NGYamlFile> yamlFileList) {
    if (EmptyPredicate.isEmpty(yamlFileList)) {
      return new ArrayList<>();
    }

    List<NGYamlFile> orderedFiles = new ArrayList<>();

    List<NGYamlFile> appsettings =
        yamlFileList.stream()
            .filter(file -> MigratorUtility.endsWithIgnoreCase(((FileYamlDTO) file.getYaml()).getName(), "appsettings"))
            .collect(Collectors.toList());
    List<NGYamlFile> connstrings =
        yamlFileList.stream()
            .filter(file -> MigratorUtility.endsWithIgnoreCase(((FileYamlDTO) file.getYaml()).getName(), "connstrings"))
            .collect(Collectors.toList());

    orderedFiles.addAll(appsettings);
    orderedFiles.addAll(connstrings);

    ValuesManifest manifest =
        ValuesManifest.builder()
            .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
            .store(ParameterField.createValueField(StoreConfigWrapper.builder()
                                                       .type(StoreConfigType.HARNESS)
                                                       .spec(manifestMigrationService.getHarnessStore(orderedFiles))
                                                       .build()))
            .build();

    return Collections.singletonList(
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder()
                          .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
                          .type(ManifestConfigType.VALUES)
                          .spec(manifest)
                          .build())
            .build());
  }
}
