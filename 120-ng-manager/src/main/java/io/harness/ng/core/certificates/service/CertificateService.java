package io.harness.ng.core.certificates.service;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.certificates.entities.Certificate;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface CertificateService {

    Certificate create( Certificate certificate);
}
