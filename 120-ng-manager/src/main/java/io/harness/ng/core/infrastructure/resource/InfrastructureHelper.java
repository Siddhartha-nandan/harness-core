/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.infrastructure.resource;

import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.mapper.InfrastructureEntityConfigMapper;
import io.harness.cdng.infra.yaml.InfrastructureDefinitionConfig;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
@UtilityClass
public class InfrastructureHelper {
  @Inject InfrastructureEntityService infrastructureEntityService;
  public IdentifierRef getConnectorRef(
      String accountId, String orgId, String projectId, String environmentId, String infrastructureDefinitionId) {
    notEmptyCheck("EnvironmentId should be provided.", environmentId);
    notEmptyCheck("InfrastructureDefinitionId should be provided.", infrastructureDefinitionId);

    InfrastructureEntity infraEntity =
        infrastructureEntityService.get(accountId, orgId, projectId, environmentId, infrastructureDefinitionId)
            .orElse(null);
    notNullCheck(format("No infrastructure definition [%s] exists in the environment [%s].", environmentId,
                     infrastructureDefinitionId),
        infraEntity);

    InfrastructureDefinitionConfig infrastructureConfig =
        InfrastructureEntityConfigMapper.toInfrastructureConfig(infraEntity).getInfrastructureDefinitionConfig();
    String connectorRef = infrastructureConfig.getSpec().getConnectorReference().getValue();

    notEmptyCheck(
        format("Connector in the infrastructure definition [%s] is empty", infrastructureDefinitionId), connectorRef);
    return IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgId, projectId);
  }
}
