/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.gitintegration.service;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY_TYPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.idp.common.delegateselectors.cache.DelegateSelectorsCache;
import io.harness.idp.common.delegateselectors.utils.DelegateSelectorsUtils;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.idp.envvariable.service.BackstageEnvVariableService;
import io.harness.idp.gitintegration.beans.CatalogInfraConnectorType;
import io.harness.idp.gitintegration.entities.CatalogConnectorEntity;
import io.harness.idp.gitintegration.mappers.ConnectorDetailsMapper;
import io.harness.idp.gitintegration.processor.base.ConnectorProcessor;
import io.harness.idp.gitintegration.processor.factory.ConnectorProcessorFactory;
import io.harness.idp.gitintegration.repositories.CatalogConnectorRepository;
import io.harness.idp.gitintegration.utils.GitIntegrationUtils;
import io.harness.idp.proxy.envvariable.ProxyEnvVariableUtils;
import io.harness.spec.server.idp.v1.model.BackstageEnvVariable;
import io.harness.spec.server.idp.v1.model.ConnectorDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @com.google.inject.Inject }))
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class GitIntegrationServiceImpl implements GitIntegrationService {
  ConnectorProcessorFactory connectorProcessorFactory;
  BackstageEnvVariableService backstageEnvVariableService;
  CatalogConnectorRepository catalogConnectorRepository;
  ConfigManagerService configManagerService;
  DelegateSelectorsCache delegateSelectorsCache;
  ProxyEnvVariableUtils proxyEnvVariableUtils;

  @Override
  public void createConnectorSecretsEnvVariable(String accountIdentifier, ConnectorInfoDTO connectorInfoDTO) {
    ConnectorProcessor connectorProcessor =
        connectorProcessorFactory.getConnectorProcessor(connectorInfoDTO.getConnectorType());
    Map<String, BackstageEnvVariable> connectorEnvSecrets =
        connectorProcessor.getConnectorAndSecretsInfo(accountIdentifier, connectorInfoDTO);
    backstageEnvVariableService.createOrUpdate(new ArrayList<>(connectorEnvSecrets.values()), accountIdentifier);
  }

  @Override
  public void processConnectorUpdate(Message message, EntityChangeDTO entityChangeDTO) throws Exception {
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String connectorIdentifier = entityChangeDTO.getIdentifier().getValue();
    Optional<CatalogConnectorEntity> catalogConnector =
        getCatalogConnectorEntity(accountIdentifier, connectorIdentifier);
    if (catalogConnector.isEmpty()) {
      return;
    }
    String infraConnectorId = catalogConnector.get().getConnectorIdentifier();
    if (connectorIdentifier.equals(infraConnectorId)) {
      log.info("Connector with id - {} is getting processed in IDP Service for git integration for account {}",
          connectorIdentifier, accountIdentifier);
      ConnectorType connectorType =
          ConnectorType.fromString(message.getMessage().getMetadataMap().get(CONNECTOR_ENTITY_TYPE));
      ConnectorProcessor connectorProcessor = connectorProcessorFactory.getConnectorProcessor(connectorType);
      ConnectorInfoDTO connectorInfoDTO = connectorProcessor.getConnectorInfo(accountIdentifier, connectorIdentifier);
      String catalogInfraConnectorType = connectorProcessor.getInfraConnectorType(connectorInfoDTO);

      saveOrUpdateConnector(connectorInfoDTO, accountIdentifier, catalogInfraConnectorType);
    }
  }

  @Override
  public void createOrUpdateConnectorInBackstage(String accountIdentifier, ConnectorInfoDTO connectorInfoDTO,
      CatalogInfraConnectorType catalogConnectorEntityType, String connectorIdentifier) throws Exception {
    createConnectorSecretsEnvVariable(accountIdentifier, connectorInfoDTO);
    String host = GitIntegrationUtils.getHostForConnector(connectorInfoDTO);
    Map<String, Boolean> hostProxyMap = new HashMap<>();
    hostProxyMap.put(host, catalogConnectorEntityType == CatalogInfraConnectorType.PROXY);
    proxyEnvVariableUtils.createOrUpdateHostProxyEnvVariable(accountIdentifier, hostProxyMap);
  }

  @Override
  public List<CatalogConnectorEntity> getAllConnectorDetails(String accountIdentifier) {
    return catalogConnectorRepository.findAllByAccountIdentifier(accountIdentifier);
  }

  @Override
  public Optional<CatalogConnectorEntity> findByAccountIdAndProviderType(
      String accountIdentifier, String providerType) {
    return catalogConnectorRepository.findByAccountIdentifierAndConnectorProviderType(accountIdentifier, providerType);
  }

  @Override
  public CatalogConnectorEntity saveConnectorDetails(String accountIdentifier, ConnectorDetails connectorDetails)
      throws Exception {
    connectorDetails.setIdentifier(
        GitIntegrationUtils.replaceAccountScopeFromConnectorId(connectorDetails.getIdentifier()));
    ConnectorProcessor connectorProcessor = connectorProcessorFactory.getConnectorProcessor(
        ConnectorType.fromString(connectorDetails.getType().toString()));
    ConnectorInfoDTO connectorInfoDTO =
        connectorProcessor.getConnectorInfo(accountIdentifier, connectorDetails.getIdentifier());
    String infraConnectorType = connectorProcessor.getInfraConnectorType(connectorInfoDTO);

    return saveOrUpdateConnector(connectorInfoDTO, accountIdentifier, infraConnectorType);
  }

  @Override
  public CatalogConnectorEntity findDefaultConnectorDetails(String accountIdentifier) {
    return catalogConnectorRepository.findLastUpdated(accountIdentifier);
  }

  private Optional<CatalogConnectorEntity> getCatalogConnectorEntity(
      String accountIdentifier, String connectorIdentifier) {
    return catalogConnectorRepository.findByAccountIdentifierAndConnectorIdentifier(
        accountIdentifier, connectorIdentifier);
  }

  private CatalogConnectorEntity saveOrUpdateConnector(
      ConnectorInfoDTO connectorInfoDTO, String accountIdentifier, String catalogInfraConnectorType) throws Exception {
    Set<String> delegateSelectors = DelegateSelectorsUtils.extractDelegateSelectors(connectorInfoDTO);
    String host = GitIntegrationUtils.getHostForConnector(connectorInfoDTO);
    CatalogConnectorEntity catalogConnectorEntity =
        ConnectorDetailsMapper.fromDTO(connectorInfoDTO.getIdentifier(), accountIdentifier,
            connectorInfoDTO.getConnectorType().toString(), delegateSelectors, host, catalogInfraConnectorType);

    Optional<CatalogConnectorEntity> existingCatalogConnectorOpt =
        catalogConnectorRepository.findByAccountIdentifierAndConnectorProviderType(
            accountIdentifier, connectorInfoDTO.getConnectorType().toString());
    if (existingCatalogConnectorOpt.isPresent()) {
      Set<String> hostsToBeRemoved = Collections.singleton(existingCatalogConnectorOpt.get().getHost());
      delegateSelectorsCache.remove(accountIdentifier, hostsToBeRemoved);
      proxyEnvVariableUtils.removeFromHostProxyEnvVariable(accountIdentifier, hostsToBeRemoved);
    }

    CatalogConnectorEntity savedCatalogConnectorEntity =
        catalogConnectorRepository.saveOrUpdate(catalogConnectorEntity);
    if (!delegateSelectors.isEmpty()) {
      delegateSelectorsCache.put(accountIdentifier, host, delegateSelectors);
    }
    createOrUpdateConnectorInBackstage(accountIdentifier, connectorInfoDTO, catalogConnectorEntity.getType(),
        catalogConnectorEntity.getConnectorIdentifier());
    return savedCatalogConnectorEntity;
  }
}
