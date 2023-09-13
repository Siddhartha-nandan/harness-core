/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.serviceoverridesv2.spring;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.repositories.serviceoverridesv2.custom.ServiceOverrideRepositoryCustomV2;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@HarnessRepo
public interface ServiceOverridesRepositoryV2
    extends CrudRepository<NGServiceOverridesEntity, String>, ServiceOverrideRepositoryCustomV2 {
  @Query(
      value =
          "{ 'accountId': ?0, 'orgIdentifier': ?1, 'projectIdentifier': ?2, 'identifier': ?3,'spec': { '$exists': true, '$ne': null } }")
  Optional<NGServiceOverridesEntity>
  getNGServiceOverridesEntityByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndSpecExistsAndSpecNotNull(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  @Query(
      value =
          "{ 'accountId': ?0, 'orgIdentifier': ?1, 'projectIdentifier': ?2, 'identifier': ?3, 'type': ?4, 'yaml': { '$exists': true, '$ne': null } }")
  Optional<NGServiceOverridesEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndTypeAndYamlExistsAndYamlNotNull(
      @NotEmpty String accountId, String orgIdentifier, String projectIdentifier, @NotNull String identifier,
      ServiceOverridesType type);

  Optional<NGServiceOverridesEntity>
  getNGServiceOverridesEntityByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
