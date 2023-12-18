/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.beans.modules.CODEModuleLicenseDTO;
import io.harness.licensing.entities.modules.CodeModuleLicense;
import io.harness.licensing.helpers.ModuleLicenseHelper;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CODE)
@Singleton
public class CodeLicenseObjectMapper implements LicenseObjectMapper<CodeModuleLicense, CODEModuleLicenseDTO> {
  @Inject private ModuleLicenseHelper moduleLicenseHelper;

  @Override
  public CODEModuleLicenseDTO toDTO(CodeModuleLicense moduleLicense) {
    return CODEModuleLicenseDTO.builder().numberOfDevelopers(moduleLicense.getNumberOfDevelopers()).build();
  }

  @Override
  public CodeModuleLicense toEntity(CODEModuleLicenseDTO codeModuleLicenseDTO) {
    validateModuleLicenseDTO(codeModuleLicenseDTO);

    return CodeModuleLicense.builder().numberOfDevelopers(codeModuleLicenseDTO.getNumberOfDevelopers()).build();
  }

  @Override
  public void validateModuleLicenseDTO(CODEModuleLicenseDTO codeModuleLicenseDTO) {
    if (!moduleLicenseHelper.isDeveloperLicensingFeatureEnabled(codeModuleLicenseDTO.getAccountIdentifier())) {
      if (codeModuleLicenseDTO.getDeveloperLicenseCount() != null) {
        throw new InvalidRequestException("New Developer Licensing feature is not enabled for this account!");
      }
    }

    if (codeModuleLicenseDTO.getDeveloperLicenseCount() != null
        && codeModuleLicenseDTO.getNumberOfDevelopers() == null) {
      // TODO: fetch mapping ratio from DeveloperMapping collection, once that work is complete
      Integer mappingRatio = 1;
      codeModuleLicenseDTO.setNumberOfDevelopers(mappingRatio * codeModuleLicenseDTO.getDeveloperLicenseCount());
    }
  }
}
