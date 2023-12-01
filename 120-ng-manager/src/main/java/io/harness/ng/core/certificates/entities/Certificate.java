package io.harness.ng.core.certificates.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "CertificateKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "certificates", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("certificates")
public class Certificate {
    @Id
    @dev.morphia.annotations.Id String id;
    String accountIdentifier;
    String orgIdentifier;
    String projectIdentifier;
    String identifier;
    String certificate;
}
