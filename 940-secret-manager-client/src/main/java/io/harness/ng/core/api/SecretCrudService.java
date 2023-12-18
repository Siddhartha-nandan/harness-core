/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ScopeInfo;
import io.harness.connector.ConnectorCategory;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface SecretCrudService {
  Boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretResponseWrapper create(String accountIdentifier, ScopeInfo scopeInfo, SecretDTOV2 dto);

  SecretResponseWrapper createViaYaml(String accountIdentifier, ScopeInfo scopeInfo, SecretDTOV2 dto);

  Optional<SecretResponseWrapper> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  Optional<SecretResponseWrapper> get(ScopeInfo scopeInfo, String identifier);

  Page<SecretResponseWrapper> list(String accountIdentifier, ScopeInfo scopeInfo, String orgIdentifier,
      String projectIdentifier, List<String> identifiers, List<SecretType> secretTypes,
      boolean includeSecretsFromEverySubScope, String searchTerm, ConnectorCategory sourceCategory,
      boolean includeAllSecretsAccessibleAtScope, PageRequest pageRequest, Set<String> secretManagerIdentifiers);

  boolean delete(ScopeInfo scopeInfo, String identifier, boolean forceDelete);

  void deleteBatch(ScopeInfo scopeInfo, List<String> secretIdentifiersList);

  SecretResponseWrapper createFile(
      String accountIdentifier, ScopeInfo scopeInfo, SecretDTOV2 dto, InputStream inputStream);

  SecretResponseWrapper createFile(
      String accountIdentifier, ScopeInfo scopeInfo, SecretDTOV2 dto, String encryptionKey, String encryptedValue);

  SecretResponseWrapper updateFile(
      ScopeInfo scopeInfo, String identifier, SecretDTOV2 updateDTO, InputStream inputStream);

  SecretResponseWrapper update(ScopeInfo scopeInfo, String identifier, SecretDTOV2 updateDTO);

  SecretResponseWrapper updateViaYaml(ScopeInfo scopeInfo, String identifier, SecretDTOV2 updateDTO);

  SecretValidationResultDTO validateSecret(ScopeInfo scopeInfo, String identifier, SecretValidationMetaData metadata);

  void validateSshWinRmSecretRef(ScopeInfo scopeInfo, SecretDTOV2 secretDTO);

  void validateSecretDtoSpec(SecretDTOV2 secretDTO);

  Long countSecrets(String accountIdentifier);
}
