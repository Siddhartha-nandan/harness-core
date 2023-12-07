/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.services.impl;

import io.harness.beans.Scope;
import io.harness.errorhandling.ErrorMessageUtils;
import io.harness.exception.DuplicateEntityException;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.ngcertificates.mapper.NgCertificateMapper;
import io.harness.ngcertificates.services.NgCertificateService;
import io.harness.repositories.ngcertificates.spring.NgCertificateRepository;
import io.harness.spec.server.ng.v1.model.CertificateDTO;

import com.google.inject.Inject;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NgCertificateServiceImpl implements NgCertificateService {
  private final NgCertificateMapper ngCertificateMapper;
  private final NgCertificateRepository ngCertificateRepository;
  @Override
  public NgCertificate create(
      String accountIdentifier, CertificateDTO certificateDTO, @Nullable InputStream uploadedInputStream) {
    NgCertificate ngCertificate =
        ngCertificateMapper.toNgCertificate(accountIdentifier, certificateDTO, uploadedInputStream);
    try {
      return ngCertificateRepository.save(ngCertificate);
    } catch (DuplicateKeyException ex) {
      String errorMessage =
          String.format("Certificate with identifier [%s] exists in [%s]", certificateDTO.getIdentifier(),
              ErrorMessageUtils.getScopeLogString(
                  Scope.of(accountIdentifier, certificateDTO.getOrg(), certificateDTO.getProject()), false));
      throw new DuplicateEntityException(errorMessage);
    }
  }
}
