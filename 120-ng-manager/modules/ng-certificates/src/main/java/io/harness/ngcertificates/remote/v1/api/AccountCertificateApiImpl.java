/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.remote.v1.api;

import static io.harness.exception.WingsException.USER;

import static java.util.Objects.nonNull;

import io.harness.exception.InvalidRequestException;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.ngcertificates.mapper.NgCertificateMapper;
import io.harness.ngcertificates.services.NgCertificateService;
import io.harness.spec.server.ng.v1.AccountCertificateApi;
import io.harness.spec.server.ng.v1.model.CertificateDTO;

import com.google.inject.Inject;
import java.io.InputStream;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AccountCertificateApiImpl implements AccountCertificateApi {
  private final NgCertificateService ngCertificateService;
  private final NgCertificateMapper ngCertificateMapper;

  @SneakyThrows
  @Override
  public Response createAccountScopedCertificates(@Valid CertificateDTO certificateDTO, String harnessAccount) {
    if (nonNull(certificateDTO.getOrg()) || nonNull(certificateDTO.getProject())) {
      throw new InvalidRequestException("Account scoped request is having non null org or project", USER);
    }
    NgCertificate ngCertificate = ngCertificateService.create(harnessAccount, certificateDTO, null);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate))
        .build();
  }

  @SneakyThrows
  @Override
  public Response createAccountScopedCertificates(
      CertificateDTO certificateDTO, InputStream fileInputStream, String harnessAccount) {
    if (nonNull(certificateDTO.getOrg()) || nonNull(certificateDTO.getProject())) {
      throw new InvalidRequestException("Account scoped request is having non null org or project", USER);
    }
    NgCertificate ngCertificate = ngCertificateService.create(harnessAccount, certificateDTO, fileInputStream);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate))
        .build();
  }

  @Override
  public Response getAccountScopedCertificates() {
    return null;
  }
}
