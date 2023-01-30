/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.entity;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.ngmigration.NGMigrationEntityType.ELASTIGROUP_CONFIGURATION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.MigratedEntityMapping;
import io.harness.cdng.elastigroup.ElastigroupConfiguration;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.encryption.Scope;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.ng.core.filestore.FileUsage;
import io.harness.ngmigration.beans.FileYamlDTO;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.client.NGClient;
import io.harness.ngmigration.client.PmsClient;
import io.harness.ngmigration.client.TemplateClient;
import io.harness.ngmigration.dto.MigrationImportSummaryDTO;
import io.harness.ngmigration.service.NgMigrationService;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.pms.yaml.ParameterField;

import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.ngmigration.CgBasicInfo;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.service.intfc.InfrastructureDefinitionService;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class ElastigroupConfigurationMigrationService extends NgMigrationService {
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;

  @Override
  public MigratedEntityMapping generateMappingEntity(NGYamlFile yamlFile) {
    return null;
  }

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    if (entity == null) {
      return null;
    }
    InfrastructureDefinition infrastructureDefinition = (InfrastructureDefinition) entity;
    CgEntityId cgEntityId = CgEntityId.builder()
                                .id(infrastructureDefinition.getUuid())
                                .type(NGMigrationEntityType.ELASTIGROUP_CONFIGURATION)
                                .build();
    CgEntityNode cgEntityNode = CgEntityNode.builder()
                                    .entityId(cgEntityId)
                                    .entity(infrastructureDefinition)
                                    .appId(infrastructureDefinition.getAppId())
                                    .id(infrastructureDefinition.getUuid())
                                    .type(NGMigrationEntityType.ELASTIGROUP_CONFIGURATION)
                                    .build();
    return DiscoveryNode.builder().entityNode(cgEntityNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(infrastructureDefinitionService.get(appId, entityId));
  }

  @Override
  public MigrationImportSummaryDTO migrate(String auth, NGClient ngClient, PmsClient pmsClient,
      TemplateClient templateClient, MigrationInputDTO inputDTO, NGYamlFile yamlFile) throws IOException {
    return migrateFile(auth, ngClient, inputDTO, yamlFile);
  }

  @Override
  public List<NGYamlFile> generateYaml(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NGYamlFile> migratedEntities) {
    InfrastructureDefinition infrastructureDefinition = (InfrastructureDefinition) entities.get(entityId).getEntity();
    NGYamlFile yamlFile = getYamlFile(infrastructureDefinition, inputDTO, entities);
    return yamlFile != null ? Collections.singletonList(yamlFile) : Collections.emptyList();
  }

  private NGYamlFile getYamlFile(InfrastructureDefinition infrastructureDefinition, MigrationInputDTO inputDTO,
      Map<CgEntityId, CgEntityNode> entities) {
    AwsAmiInfrastructure infrastructure = (AwsAmiInfrastructure) infrastructureDefinition.getInfrastructure();
    if (StringUtils.isBlank(infrastructure.getSpotinstElastiGroupJson())) {
      return null;
    }
    byte[] fileContent = infrastructure.getSpotinstElastiGroupJson().getBytes(StandardCharsets.UTF_8);
    return getYamlFile(inputDTO, infrastructureDefinition, fileContent, infrastructureDefinition.getName());
  }

  private static NGYamlFile getYamlFile(
      MigrationInputDTO inputDTO, InfrastructureDefinition infrastructureDefinition, byte[] content, String infraName) {
    if (isEmpty(content)) {
      return null;
    }
    String prefix = infraName + ' ';
    String fileUsage = FileUsage.CONFIG.name();
    String projectIdentifier = MigratorUtility.getProjectIdentifier(Scope.PROJECT, inputDTO);
    String orgIdentifier = MigratorUtility.getOrgIdentifier(Scope.PROJECT, inputDTO);
    String identifier = MigratorUtility.generateManifestIdentifier(prefix + "ElastigroupConfigurationSpec");
    String name = identifier + ".json";
    return NGYamlFile.builder()
        .type(NGMigrationEntityType.ELASTIGROUP_CONFIGURATION)
        .filename(null)
        .yaml(FileYamlDTO.builder()
                  .identifier(identifier)
                  .fileUsage(fileUsage)
                  .name(name)
                  .content(new String(content))
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .build())
        .ngEntityDetail(NgEntityDetail.builder()
                            .identifier(identifier)
                            .orgIdentifier(orgIdentifier)
                            .projectIdentifier(projectIdentifier)
                            .build())
        .cgBasicInfo(CgBasicInfo.builder()
                         .id(infrastructureDefinition.getUuid())
                         .name(infrastructureDefinition.getName())
                         .type(ELASTIGROUP_CONFIGURATION)
                         .appId(infrastructureDefinition.getAppId())
                         .accountId(infrastructureDefinition.getAccountId())
                         .build())
        .build();
  }

  @Override
  protected YamlDTO getNGEntity(NgEntityDetail ngEntityDetail, String accountIdentifier) {
    return null;
  }

  @Override
  protected boolean isNGEntityExists() {
    return true;
  }

  public List<ElastigroupConfiguration> getElastigroupConfigurations(
      Set<CgEntityId> infraSpecIds, MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities) {
    if (isEmpty(infraSpecIds)) {
      return new ArrayList<>();
    }
    List<ElastigroupConfiguration> elastigroupConfigurations = new ArrayList<>();
    for (CgEntityId configEntityId : infraSpecIds) {
      CgEntityNode configNode = entities.get(configEntityId);
      if (configNode != null) {
        InfrastructureDefinition infrastructureDefinition = (InfrastructureDefinition) configNode.getEntity();
        NGYamlFile file = getYamlFile(infrastructureDefinition, inputDTO, entities);
        if (file != null) {
          elastigroupConfigurations.add(getConfigFileWrapper(file));
        }
      }
    }
    return elastigroupConfigurations;
  }

  @Override
  public boolean canMigrate(CgEntityId id, CgEntityId root, boolean migrateAll) {
    return migrateAll || root.getType() == NGMigrationEntityType.INFRA;
  }

  private static ElastigroupConfiguration getConfigFileWrapper(NGYamlFile file) {
    ParameterField<List<String>> files;
    files = MigratorUtility.getFileStorePaths(Collections.singletonList(file));
    return ElastigroupConfiguration.builder()
        .store(StoreConfigWrapper.builder()
                   .type(StoreConfigType.HARNESS)
                   .spec(HarnessStore.builder().files(files).build())
                   .build())
        .build();
  }
}
