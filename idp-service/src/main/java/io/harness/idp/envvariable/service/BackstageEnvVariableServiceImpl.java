/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.envvariable.service;

import static io.harness.idp.k8s.constants.K8sConstants.BACKSTAGE_SECRET;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptedSecretValue;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.envvariable.beans.entity.BackstageEnvVariableEntity;
import io.harness.idp.envvariable.beans.entity.BackstageEnvVariableEntity.BackstageEnvVariableMapper;
import io.harness.idp.envvariable.beans.entity.BackstageEnvVariableType;
import io.harness.idp.envvariable.repositories.BackstageEnvVariableRepository;
import io.harness.idp.k8s.client.K8sClient;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.spec.server.idp.v1.model.BackstageEnvConfigVariable;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;
import io.harness.spec.server.idp.v1.model.BackstageEnvVariable;
import io.harness.spec.server.idp.v1.model.NamespaceInfo;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class BackstageEnvVariableServiceImpl implements BackstageEnvVariableService {
  private BackstageEnvVariableRepository backstageEnvVariableRepository;
  private K8sClient k8sClient;
  @Named("PRIVILEGED") private SecretManagerClientService ngSecretService;
  private NamespaceService namespaceService;
  private Map<BackstageEnvVariableType, BackstageEnvVariableMapper> envVariableMap;

  @Override
  public Optional<BackstageEnvVariable> findByIdAndAccountIdentifier(String identifier, String accountIdentifier) {
    Optional<BackstageEnvVariableEntity> envVariableEntityOpt =
        backstageEnvVariableRepository.findByIdAndAccountIdentifier(identifier, accountIdentifier);
    if (envVariableEntityOpt.isEmpty()) {
      return Optional.empty();
    }
    BackstageEnvVariableMapper envVariableMapper = getEnvVariableMapper((envVariableEntityOpt.get().getType()));
    return Optional.of(envVariableMapper.toDto(envVariableEntityOpt.get()));
  }

  @Override
  public BackstageEnvVariable create(BackstageEnvVariable envVariable, String accountIdentifier) {
    sync(Collections.singletonList(envVariable), accountIdentifier);
    BackstageEnvVariableMapper envVariableMapper =
        getEnvVariableMapper(BackstageEnvVariableType.valueOf(envVariable.getType().name()));
    BackstageEnvVariableEntity backstageEnvVariableEntity = envVariableMapper.fromDto(envVariable, accountIdentifier);
    return envVariableMapper.toDto(backstageEnvVariableRepository.save(backstageEnvVariableEntity));
  }

  @Override
  public List<BackstageEnvVariable> createMulti(
      List<BackstageEnvVariable> requestEnvVariables, String accountIdentifier) {
    sync(requestEnvVariables, accountIdentifier);
    List<BackstageEnvVariableEntity> entities = getEntitiesFromDtos(requestEnvVariables, accountIdentifier);
    List<BackstageEnvVariable> responseEnvVariables = new ArrayList<>();
    backstageEnvVariableRepository.saveAll(entities).forEach(envVariableEntity -> {
      BackstageEnvVariableMapper envVariableMapper = getEnvVariableMapper(envVariableEntity.getType());
      responseEnvVariables.add(envVariableMapper.toDto(envVariableEntity));
    });
    return responseEnvVariables;
  }

  @Override
  public BackstageEnvVariable update(BackstageEnvVariable envVariable, String accountIdentifier) {
    sync(Collections.singletonList(envVariable), accountIdentifier);
    BackstageEnvVariableMapper envVariableMapper =
        getEnvVariableMapper(BackstageEnvVariableType.valueOf(envVariable.getType().name()));
    BackstageEnvVariableEntity backstageEnvVariableEntity = envVariableMapper.fromDto(envVariable, accountIdentifier);
    backstageEnvVariableEntity.setAccountIdentifier(accountIdentifier);
    return envVariableMapper.toDto(backstageEnvVariableRepository.update(backstageEnvVariableEntity));
  }

  @Override
  public List<BackstageEnvVariable> updateMulti(
      List<BackstageEnvVariable> requestEnvVariables, String accountIdentifier) {
    sync(requestEnvVariables, accountIdentifier);
    List<BackstageEnvVariableEntity> entities = getEntitiesFromDtos(requestEnvVariables, accountIdentifier);
    List<BackstageEnvVariable> responseSecrets = new ArrayList<>();
    entities.forEach(entity -> {
      BackstageEnvVariableMapper envVariableMapper = getEnvVariableMapper((entity.getType()));
      responseSecrets.add(envVariableMapper.toDto(backstageEnvVariableRepository.update(entity)));
    });
    return responseSecrets;
  }

  @Override
  public List<BackstageEnvVariable> findByAccountIdentifier(String accountIdentifier) {
    List<BackstageEnvVariableEntity> entities =
        backstageEnvVariableRepository.findByAccountIdentifier(accountIdentifier);
    List<BackstageEnvVariable> secretDTOs = new ArrayList<>();
    entities.forEach(entity -> {
      BackstageEnvVariableMapper envVariableMapper = getEnvVariableMapper((entity.getType()));
      secretDTOs.add(envVariableMapper.toDto(entity));
    });
    return secretDTOs;
  }

  @Override
  public void delete(String identifier, String accountIdentifier) {
    Optional<BackstageEnvVariableEntity> envSecretOpt =
        backstageEnvVariableRepository.findByIdAndAccountIdentifier(identifier, accountIdentifier);
    if (envSecretOpt.isEmpty()) {
      throw new InvalidRequestException(
          format("Environment secret [%s] not found in account [%s]", identifier, accountIdentifier));
    }
    k8sClient.removeSecretData(getNamespaceForAccount(accountIdentifier), BACKSTAGE_SECRET,
        Collections.singletonList(envSecretOpt.get().getEnvName()));
    backstageEnvVariableRepository.delete(envSecretOpt.get());
  }

  @Override
  public void deleteMulti(List<String> secretIdentifiers, String accountIdentifier) {
    Iterable<BackstageEnvVariableEntity> secrets = backstageEnvVariableRepository.findAllById(secretIdentifiers);
    List<String> envNames =
        Streams.stream(secrets).map(BackstageEnvVariableEntity::getEnvName).collect(Collectors.toList());
    k8sClient.removeSecretData(getNamespaceForAccount(accountIdentifier), BACKSTAGE_SECRET, envNames);
    backstageEnvVariableRepository.deleteAllById(secretIdentifiers);
  }

  @Override
  public void processSecretUpdate(EntityChangeDTO entityChangeDTO) {
    String secretIdentifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    Optional<BackstageEnvVariableEntity> envVariableEntityOpt =
        backstageEnvVariableRepository.findByAccountIdentifierAndHarnessSecretIdentifier(
            accountIdentifier, secretIdentifier);
    if (envVariableEntityOpt.isPresent()) {
      BackstageEnvVariableMapper envVariableMapper = getEnvVariableMapper((envVariableEntityOpt.get().getType()));
      sync(Collections.singletonList(envVariableMapper.toDto(envVariableEntityOpt.get())), accountIdentifier);
    } else {
      // TODO: There might be too many secrets overall. We might have to consider removing this log line in future
      log.info("Secret {} is not tracker by IDP, hence not processing it", secretIdentifier);
    }
  }

  @Override
  public void sync(List<BackstageEnvVariable> envVariables, String accountIdentifier) {
    if (envVariables.isEmpty()) {
      return;
    }
    Map<String, byte[]> secretData = new HashMap<>();
    for (BackstageEnvVariable envVariable : envVariables) {
      String envName = envVariable.getEnvName();

      if (envVariable.getType().name().equals(BackstageEnvVariableType.SECRET.name())) {
        String secretIdentifier = ((BackstageEnvSecretVariable) envVariable).getHarnessSecretIdentifier();
        DecryptedSecretValue decryptedValue =
            ngSecretService.getDecryptedSecretValue(accountIdentifier, null, null, secretIdentifier);
        secretData.put(envName, decryptedValue.getDecryptedValue().getBytes());
      } else {
        secretData.put(envName, ((BackstageEnvConfigVariable) envVariable).getValue().getBytes());
      }
    }
    String namespace = getNamespaceForAccount(accountIdentifier);
    k8sClient.updateSecretData(namespace, BACKSTAGE_SECRET, secretData, false);
    log.info("Successfully updated secret {} in the namespace {}", BACKSTAGE_SECRET, namespace);
  }

  private String getNamespaceForAccount(String accountIdentifier) {
    NamespaceInfo namespaceInfo = namespaceService.getNamespaceForAccountIdentifier(accountIdentifier);
    return namespaceInfo.getNamespace();
  }

  private BackstageEnvVariableMapper getEnvVariableMapper(BackstageEnvVariableType envVariableType) {
    BackstageEnvVariableMapper envVariableMapper = envVariableMap.get(envVariableType);
    if (envVariableMapper == null) {
      throw new InvalidRequestException("Backstage env variable type not set");
    }
    return envVariableMapper;
  }

  private List<BackstageEnvVariableEntity> getEntitiesFromDtos(
      List<BackstageEnvVariable> requestEnvVariables, String accountIdentifier) {
    return requestEnvVariables.stream()
        .map(envVariable -> {
          BackstageEnvVariableMapper envVariableMapper =
              getEnvVariableMapper(BackstageEnvVariableType.valueOf(envVariable.getType().name()));
          return envVariableMapper.fromDto(envVariable, accountIdentifier);
        })
        .collect(Collectors.toList());
  }
}
