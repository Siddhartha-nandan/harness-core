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
import io.harness.spec.server.ng.v1.OrgCertificateApi;
import io.harness.spec.server.ng.v1.model.CertificateDTO;

import com.google.inject.Inject;
import java.io.InputStream;
import java.util.Objects;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class OrgCertificateApiImpl implements OrgCertificateApi {
  private final NgCertificateService ngCertificateService;
  private final NgCertificateMapper ngCertificateMapper;
  @SneakyThrows
  @Override
  public Response createOrgScopedCertificates(String org, @Valid CertificateDTO certificateDTO, String harnessAccount) {
    if (!Objects.equals(org, certificateDTO.getOrg()) || nonNull(certificateDTO.getProject())) {
      throw new InvalidRequestException(
          "Organization scoped request is having different org in payload and param OR non null project", USER);
    }
    NgCertificate ngCertificate = ngCertificateService.create(harnessAccount, certificateDTO, null);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate))
        .build();
  }

  @SneakyThrows
  @Override
  public Response createOrgScopedCertificates(
      String org, CertificateDTO certificateDTO, InputStream fileInputStream, String harnessAccount) {
    if (!Objects.equals(org, certificateDTO.getOrg()) || nonNull(certificateDTO.getProject())) {
      throw new InvalidRequestException(
          "Organization scoped request is having different org in payload and param OR non null project", USER);
    }
    NgCertificate ngCertificate = ngCertificateService.create(harnessAccount, certificateDTO, fileInputStream);
    return Response.status(Response.Status.CREATED)
        .entity(ngCertificateMapper.toCertificateResponseDTO(ngCertificate))
        .build();
  }

  @Override
  public Response getOrgScopedCertificates(String org) {
    return null;
  }
}
