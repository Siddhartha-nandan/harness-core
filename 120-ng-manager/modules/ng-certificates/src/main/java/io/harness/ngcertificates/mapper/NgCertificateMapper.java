/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.mapper;

import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;
import static io.harness.security.SimpleEncryption.CHARSET;

import io.harness.delegate.beans.FileUploadLimit;
import io.harness.exception.FileReadException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.exception.UnsupportedOperationException;
import io.harness.ngcertificates.entities.NgCertificate;
import io.harness.security.X509Utils;
import io.harness.spec.server.ng.v1.model.CertificateDTO;
import io.harness.spec.server.ng.v1.model.CertificateDetailsDTO;
import io.harness.spec.server.ng.v1.model.CertificateInputSpecDTO;
import io.harness.spec.server.ng.v1.model.CertificateInputSpecType;
import io.harness.spec.server.ng.v1.model.CertificateIssuanceInfo;
import io.harness.spec.server.ng.v1.model.CertificateResponseDTO;
import io.harness.spec.server.ng.v1.model.FileCertificateInputSpecDTO;
import io.harness.spec.server.ng.v1.model.TextCertificateInputSpecDTO;
import io.harness.stream.BoundedInputStream;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NgCertificateMapper {
  private final FileUploadLimit fileUploadLimit;

  public NgCertificate toNgCertificate(
      String accountIdentifier, CertificateDTO certificateDTO, @Nullable InputStream uploadedInputStream) {
    return NgCertificate.builder()
        .identifier(certificateDTO.getIdentifier())
        .name(certificateDTO.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(certificateDTO.getOrg())
        .projectIdentifier(certificateDTO.getProject())
        .tags(convertToList(certificateDTO.getTags()))
        .description(certificateDTO.getDescription())
        .inputSpecType(certificateDTO.getInputSpec().getType())
        .certificate(getCertificateContent(certificateDTO, uploadedInputStream))
        .build();
  }

  public CertificateDTO toCertificateDTO(NgCertificate ngCertificate) {
    CertificateDTO certificateDTO = new CertificateDTO().inputSpec(getCertificateInputSpecDTO(ngCertificate));
    certificateDTO.identifier(ngCertificate.getIdentifier())
        .name(ngCertificate.getName())
        .org(ngCertificate.getOrgIdentifier())
        .project(ngCertificate.getProjectIdentifier())
        .certificateValue(decodeBase64ToString(ngCertificate.getCertificate()))
        .description(ngCertificate.getDescription())
        .tags(convertToMap(ngCertificate.getTags()));
    return certificateDTO;
  }

  public CertificateResponseDTO toCertificateResponseDTO(NgCertificate certificate) throws CertificateException {
    CertificateDTO certificateDTO = toCertificateDTO(certificate);
    return new CertificateResponseDTO()
        .certificate(certificateDTO)
        .certificateDetails(getCertificateDetailsDTO(decodeBase64ToString(certificate.getCertificate())))
        .createdAt(certificate.getCreatedAt())
        .lastModifiedAt(certificate.getLastModifiedDate());
  }

  public CertificateDetailsDTO getCertificateDetailsDTO(String certificate) throws CertificateException {
    X509Certificate x509Certificate = X509Utils.getX509Certificate(certificate);
    return new CertificateDetailsDTO()
        .issuedTo(getIssueInfo(x509Certificate.getSubjectX500Principal()))
        .issuedBy(getIssueInfo(x509Certificate.getIssuerX500Principal()))
        .validFrom(x509Certificate.getNotBefore().getTime())
        .validTo(x509Certificate.getNotAfter().getTime())
        .signatureAlgo(x509Certificate.getSigAlgName());
  }

  private CertificateIssuanceInfo getIssueInfo(X500Principal principal) {
    Map<ASN1ObjectIdentifier, String> dnInfoMap = getDNInfo(principal, List.of(BCStyle.CN, BCStyle.O, BCStyle.OU));
    return new CertificateIssuanceInfo()
        .commonName(dnInfoMap.get(BCStyle.CN))
        .organisation(dnInfoMap.get(BCStyle.O))
        .organisationUnit(dnInfoMap.get(BCStyle.OU));
  }

  private Map<ASN1ObjectIdentifier, String> getDNInfo(X500Principal principal, List<ASN1ObjectIdentifier> bcStyles) {
    Map<ASN1ObjectIdentifier, String> dnInfoMap = new HashMap<>();
    X500Name x500Name = new X500Name(principal.getName());
    bcStyles.forEach(bcStyle -> {
      RDN[] rdns = x500Name.getRDNs(bcStyle);
      dnInfoMap.put(bcStyle, rdns.length > 0 ? IETFUtils.valueToString(rdns[0].getFirst().getValue()) : null);
    });
    return dnInfoMap;
  }

  private String getCertificateContent(CertificateDTO certificateDTO, @Nullable InputStream uploadedInputStream) {
    String certContent;
    switch (certificateDTO.getInputSpec().getType()) {
      case TEXT:
        certContent = ((TextCertificateInputSpecDTO) certificateDTO.getInputSpec()).getValue();
        break;
      case FILE:
        certContent = getCertificateContent(uploadedInputStream);
        break;
      default:
        throw new UnsupportedOperationException("This cannot be extracted from DTO");
    }
    validateCertificateIsX509Certificate(certContent);
    return encodeBase64(certContent);
  }

  private void validateCertificateIsX509Certificate(String certContent) {
    try {
      X509Utils.getX509Certificate(certContent);
    } catch (CertificateException e) {
      throw new InvalidRequestException("Certificate is not a valid X509Certificate");
    }
  }

  private String getCertificateContent(InputStream uploadedInputStream) {
    if (null == uploadedInputStream) {
      throw new InvalidRequestException("File content cannot be null");
    }
    InputStream inputStream = new BoundedInputStream(uploadedInputStream, fileUploadLimit.getCertificateFileLimit());
    try {
      byte[] inputBytes = ByteStreams.toByteArray(inputStream);
      return new String(CHARSET.decode(ByteBuffer.wrap(inputBytes)).array());
    } catch (IOException exception) {
      throw new FileReadException("Unable to read certificate file uploaded", exception);
    }
  }

  private CertificateInputSpecDTO getCertificateInputSpecDTO(NgCertificate ngCertificate) {
    CertificateInputSpecType inputSpecType = ngCertificate.getInputSpecType();
    switch (inputSpecType) {
      case TEXT:
        return new TextCertificateInputSpecDTO().value(ngCertificate.getCertificate()).type(inputSpecType);
      case FILE:
        return new FileCertificateInputSpecDTO().type(inputSpecType);
      default:
        throw new UnknownEnumTypeException(inputSpecType.name(), inputSpecType.toString());
    }
  }
}
