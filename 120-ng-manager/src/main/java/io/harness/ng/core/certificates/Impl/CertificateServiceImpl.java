package io.harness.ng.core.certificates.Impl;

import io.harness.ng.core.certificates.entities.Certificate;
import io.harness.ng.core.certificates.repositories.CertificateRepository;
import io.harness.ng.core.certificates.service.CertificateService;
import com.google.inject.Inject;
public class CertificateServiceImpl implements CertificateService {
  @Inject private  CertificateRepository certificateRepository;

  @Override
  public Certificate create(Certificate certificate) {
    return certificateRepository.save(certificate);
  }
}
