package io.harness.ng.core.certificates.repositories;

import io.harness.ng.core.certificates.entities.Certificate;

public interface CertificateRepository {

    Certificate save (Certificate certificate);

}
