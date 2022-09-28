/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.ngmigration.beans.DiscoverEntityInput;
import io.harness.ngmigration.beans.DiscoveryInput;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.connector.ConnectorFactory;
import io.harness.ngmigration.dto.ImportConnectorDTO;

import software.wings.beans.SettingAttribute;
import software.wings.ngmigration.DiscoveryResult;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectorImportService {
  @Inject private SettingsService settingsService;
  @Inject DiscoveryService discoveryService;

  private List<String> getSettingIdsForType(String accountId, Set<SettingVariableTypes> types) {
    return types.stream()
        .flatMap(type -> settingsService.getGlobalSettingAttributesByType(accountId, type.name()).stream())
        .map(SettingAttribute::getUuid)
        .collect(Collectors.toList());
  }

  public List<NGYamlFile> importConnectors(String authToken, ImportConnectorDTO importConnectorDTO) {
    String accountId = importConnectorDTO.getAccountIdentifier();
    List<String> settingIds;
    switch (importConnectorDTO.getMechanism()) {
      case ALL:
        // Note: All here means all the connectors we support today
        settingIds = getSettingIdsForType(accountId, ConnectorFactory.CONNECTOR_FACTORY_MAP.keySet());
        break;
      case TYPE:
        settingIds = getSettingIdsForType(accountId, importConnectorDTO.getTypes());
        break;
      case SPECIFIC:
        settingIds = importConnectorDTO.getIds();
        break;
      default:
        settingIds = new ArrayList<>();
    }
    DiscoveryResult discoveryResult = discoveryService.discoverMulti(accountId,
        DiscoveryInput.builder()
            .entities(settingIds.stream()
                          .map(settingId
                              -> DiscoverEntityInput.builder()
                                     .entityId(settingId)
                                     .type(NGMigrationEntityType.CONNECTOR)
                                     .build())
                          .collect(Collectors.toList()))
            .exportImage(false)
            .build());
    MigrationInputDTO migrationInputDTO =
        MigrationInputDTO.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(importConnectorDTO.getOrgIdentifier())
            .projectIdentifier(importConnectorDTO.getProjectIdentifier())
            .migrateReferencedEntities(importConnectorDTO.isMigrateReferencedEntities())
            .build();
    return discoveryService.migrateEntity(authToken, migrationInputDTO, discoveryResult, true);
  }
}
