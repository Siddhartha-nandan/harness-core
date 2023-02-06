/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.infra;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.elastigroup.ElastigroupConfiguration;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.ng.core.infrastructure.InfrastructureType;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;

import software.wings.infra.InfrastructureDefinition;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
public interface InfraDefMapper {
  ServiceDefinitionType getServiceDefinition(InfrastructureDefinition infrastructureDefinition);
  InfrastructureType getInfrastructureType(InfrastructureDefinition infrastructureDefinition);
  Infrastructure getSpec(MigrationInputDTO inputDTO, InfrastructureDefinition infrastructureDefinition,
      Map<CgEntityId, NGYamlFile> migratedEntities, Map<CgEntityId, CgEntityNode> entities,
      List<ElastigroupConfiguration> elastigroupConfigurations);

  default List<String> getConnectorIds(InfrastructureDefinition infrastructureDefinition) {
    return Collections.emptyList();
  }
}
