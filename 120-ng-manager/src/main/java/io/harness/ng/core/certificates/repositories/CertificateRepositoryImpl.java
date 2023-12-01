package io.harness.ng.core.certificates.repositories;

import io.harness.ng.core.certificates.entities.Certificate;

import com.google.inject.Inject;
import org.springframework.data.mongodb.core.MongoTemplate;

public class CertificateRepositoryImpl implements CertificateRepository {
  @Inject MongoTemplate mongoTemplate;
  private final String CERTIFICATE_COLLECTION_NAME = "certificates";
  @Override
  public Certificate save(Certificate certificate) {
      return mongoTemplate.save(certificate, getCollectionName());
  }

  private String getCollectionName() {
    return CERTIFICATE_COLLECTION_NAME;
  }
}
