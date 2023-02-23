/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.secret.service;

import static io.harness.k8s.constants.K8sConstants.BACKSTAGE_SECRET;
import static io.harness.k8s.constants.K8sConstants.DEFAULT_NAMESPACE;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptedSecretValue;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.secret.beans.entity.EnvironmentSecretEntity;
import io.harness.idp.secret.mappers.EnvironmentSecretMapper;
import io.harness.idp.secret.repositories.EnvironmentSecretRepository;
import io.harness.k8s.client.K8sClient;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.spec.server.idp.v1.model.EnvironmentSecret;
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
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class EnvironmentSecretServiceImpl implements EnvironmentSecretService {
  private static final String SUCCEEDED = "succeeded";
  private static final String FAILED = "failed";
  private static final String IDP_NOT_ENABLED = "IDP has not been set up for account [%s]";
  private EnvironmentSecretRepository environmentSecretRepository;
  private K8sClient k8sClient;
  @Named("PRIVILEGED") private SecretManagerClientService ngSecretService;
  private NamespaceService namespaceService;

  @Override
  public Optional<EnvironmentSecret> findByIdAndAccountIdentifier(String identifier, String accountIdentifier) {
    Optional<EnvironmentSecretEntity> secretOpt =
        environmentSecretRepository.findByIdAndAccountIdentifier(identifier, accountIdentifier);
    return secretOpt.map(EnvironmentSecretMapper::toDTO);
  }

  @Override
  public EnvironmentSecret saveAndSyncK8sSecret(EnvironmentSecret environmentSecret, String accountIdentifier)
      throws Exception {
    boolean success = syncK8sSecret(Collections.singletonList(environmentSecret), accountIdentifier);
    log.info("Secret [{}] insert {} for the account [{}]", environmentSecret.getSecretIdentifier(),
        success ? SUCCEEDED : FAILED, accountIdentifier);
    EnvironmentSecretEntity environmentSecretEntity =
        EnvironmentSecretMapper.fromDTO(environmentSecret, accountIdentifier);
    return EnvironmentSecretMapper.toDTO(environmentSecretRepository.save(environmentSecretEntity));
  }

  @Override
  public List<EnvironmentSecret> saveAndSyncK8sSecrets(List<EnvironmentSecret> requestSecrets, String accountIdentifier)
      throws Exception {
    boolean success = syncK8sSecret(requestSecrets, accountIdentifier);
    if (success) {
      log.info("Successfully synced secret {} in the namespace {}", BACKSTAGE_SECRET, DEFAULT_NAMESPACE);
    }
    List<EnvironmentSecretEntity> entities =
        requestSecrets.stream()
            .map(requestSecret -> EnvironmentSecretMapper.fromDTO(requestSecret, accountIdentifier))
            .collect(Collectors.toList());
    List<EnvironmentSecret> responseSecrets = new ArrayList<>();
    environmentSecretRepository.saveAll(entities).forEach(
        responseSecret -> responseSecrets.add(EnvironmentSecretMapper.toDTO(responseSecret)));
    return responseSecrets;
  }

  @Override
  public EnvironmentSecret updateAndSyncK8sSecret(EnvironmentSecret environmentSecret, String accountIdentifier)
      throws Exception {
    boolean success = syncK8sSecret(Collections.singletonList(environmentSecret), accountIdentifier);
    log.info("Secret [{}] update {} for the account [{}]", environmentSecret.getSecretIdentifier(),
        success ? SUCCEEDED : FAILED, accountIdentifier);
    EnvironmentSecretEntity environmentSecretEntity =
        EnvironmentSecretMapper.fromDTO(environmentSecret, accountIdentifier);
    environmentSecretEntity.setAccountIdentifier(accountIdentifier);
    return EnvironmentSecretMapper.toDTO(environmentSecretRepository.update(environmentSecretEntity));
  }

  @Override
  public List<EnvironmentSecret> updateAndSyncK8sSecrets(
      List<EnvironmentSecret> requestSecrets, String accountIdentifier) throws Exception {
    boolean success = syncK8sSecret(requestSecrets, accountIdentifier);
    if (success) {
      log.info("Successfully synced secret {} in the namespace {}", BACKSTAGE_SECRET, DEFAULT_NAMESPACE);
    }
    List<EnvironmentSecretEntity> entities =
        requestSecrets.stream()
            .map(requestSecret -> EnvironmentSecretMapper.fromDTO(requestSecret, accountIdentifier))
            .collect(Collectors.toList());
    List<EnvironmentSecret> responseSecrets = new ArrayList<>();
    entities.forEach(
        entity -> responseSecrets.add(EnvironmentSecretMapper.toDTO(environmentSecretRepository.update(entity))));
    return responseSecrets;
  }

  @Override
  public List<EnvironmentSecret> findByAccountIdentifier(String accountIdentifier) {
    List<EnvironmentSecretEntity> secrets = environmentSecretRepository.findByAccountIdentifier(accountIdentifier);
    List<EnvironmentSecret> secretDTOs = new ArrayList<>();
    secrets.forEach(environmentSecretEntity -> secretDTOs.add(EnvironmentSecretMapper.toDTO(environmentSecretEntity)));
    return secretDTOs;
  }

  @Override
  public void delete(String secretIdentifier, String accountIdentifier) throws Exception {
    EnvironmentSecretEntity environmentSecretEntity = EnvironmentSecretEntity.builder().id(secretIdentifier).build();
    Optional<EnvironmentSecretEntity> envSecretOpt =
        environmentSecretRepository.findByAccountIdentifierAndSecretIdentifier(secretIdentifier, accountIdentifier);
    if (envSecretOpt.isEmpty()) {
      throw new InvalidRequestException(
          format("Environment secret [%s] not found in account [%s]", secretIdentifier, accountIdentifier));
    }
    k8sClient.removeSecretData(getNamespaceForAccount(accountIdentifier), BACKSTAGE_SECRET,
        Collections.singletonList(envSecretOpt.get().getEnvName()));
    environmentSecretRepository.delete(environmentSecretEntity);
  }

  @Override
  public void deleteMulti(List<String> secretIdentifiers, String accountIdentifier) throws Exception {
    Iterable<EnvironmentSecretEntity> secrets = environmentSecretRepository.findAllById(secretIdentifiers);
    List<String> envNames =
        Streams.stream(secrets).map(EnvironmentSecretEntity::getEnvName).collect(Collectors.toList());
    k8sClient.removeSecretData(getNamespaceForAccount(accountIdentifier), BACKSTAGE_SECRET, envNames);
    environmentSecretRepository.deleteAllById(secretIdentifiers);
  }

  @Override
  public void processSecretUpdate(EntityChangeDTO entityChangeDTO, String action) throws Exception {
    String secretIdentifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    Optional<EnvironmentSecretEntity> envSecretOpt =
        environmentSecretRepository.findByAccountIdentifierAndSecretIdentifier(accountIdentifier, secretIdentifier);
    if (envSecretOpt.isPresent()) {
      boolean success = syncK8sSecret(
          Collections.singletonList(EnvironmentSecretMapper.toDTO(envSecretOpt.get())), accountIdentifier);
      log.info("Secret [{}] update {} in the namespace [{}]", secretIdentifier, success ? "succeeded" : "failed",
          DEFAULT_NAMESPACE);
    } else {
      // TODO: There might be too many secrets overall. We might have to consider removing this log line in future
      log.info("Secret {} is not tracker by IDP, hence not processing it", secretIdentifier);
    }
  }

  private boolean syncK8sSecret(List<EnvironmentSecret> environmentSecrets, String accountIdentifier) throws Exception {
    // TODO: get the namespace for the given account. Currently assuming it to be default. Needs to be fixed.
    Map<String, byte[]> secretData = new HashMap<>();
    for (EnvironmentSecret environmentSecret : environmentSecrets) {
      String envName = environmentSecret.getEnvName();
      String secretIdentifier = environmentSecret.getSecretIdentifier();
      if (StringUtils.isBlank(environmentSecret.getDecryptedValue())) {
        DecryptedSecretValue decryptedValue =
            ngSecretService.getDecryptedSecretValue(accountIdentifier, null, null, secretIdentifier);
        secretData.put(envName, decryptedValue.getDecryptedValue().getBytes());
      } else {
        secretData.put(envName, environmentSecret.getDecryptedValue().getBytes());
      }
    }
    return k8sClient.updateSecretData(getNamespaceForAccount(accountIdentifier), BACKSTAGE_SECRET, secretData, false);
  }

  private String getNamespaceForAccount(String accountIdentifier) {
    Optional<NamespaceInfo> namespaceOpt = namespaceService.getNamespaceForAccountIdentifier(accountIdentifier);
    if (namespaceOpt.isEmpty()) {
      throw new InvalidRequestException(format(IDP_NOT_ENABLED, accountIdentifier));
    }
    return namespaceOpt.get().getNamespace();
  }
}
