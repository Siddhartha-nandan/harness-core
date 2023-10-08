/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.connector;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.elkconnector.ELKAuthType;
import io.harness.delegate.beans.connector.elkconnector.ELKConnectorDTO;
import io.harness.delegate.beans.connector.elkconnector.ELKConnectorDTO.ELKConnectorDTOBuilder;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.utils.MigratorUtility;

import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.ngmigration.CgEntityId;
import software.wings.service.impl.analysis.ElkValidationType;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class ElkConnectorImpl implements BaseConnector {
  @Override
  public List<String> getSecretIds(SettingAttribute settingAttribute) {
    ElkConfig config = (ElkConfig) settingAttribute.getValue();
    return Lists.newArrayList(config.getEncryptedPassword());
  }

  @Override
  public ConnectorType getConnectorType(SettingAttribute settingAttribute) {
    return ConnectorType.ELASTICSEARCH;
  }

  @Override
  public ConnectorConfigDTO getConfigDTO(
      SettingAttribute settingAttribute, Set<CgEntityId> childEntities, Map<CgEntityId, NGYamlFile> migratedEntities) {
    ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    ELKConnectorDTOBuilder dtoBuilder =
        ELKConnectorDTO.builder().url(elkConfig.getElkUrl()).delegateSelectors(new HashSet<>());
    if (ElkValidationType.TOKEN.equals(elkConfig.getValidationType())) {
      dtoBuilder.authType(ELKAuthType.API_CLIENT_TOKEN);
      dtoBuilder.apiKeyId(elkConfig.getUsername());
      dtoBuilder.apiKeyRef(MigratorUtility.getSecretRef(migratedEntities, elkConfig.getEncryptedPassword()));
    } else if (StringUtils.isNotEmpty(elkConfig.getEncryptedPassword())) {
      dtoBuilder.authType(ELKAuthType.USERNAME_PASSWORD);
      dtoBuilder.passwordRef(MigratorUtility.getSecretRef(migratedEntities, elkConfig.getEncryptedPassword()));
      dtoBuilder.username(elkConfig.getUsername());
    } else {
      dtoBuilder.authType(ELKAuthType.NONE);
    }
    return dtoBuilder.build();
  }
}
