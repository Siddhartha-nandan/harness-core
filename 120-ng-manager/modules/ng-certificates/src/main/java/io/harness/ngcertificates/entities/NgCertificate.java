/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngcertificates.entities;

import io.harness.annotations.StoreIn;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.spec.server.ng.v1.model.CertificateInputSpecType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import dev.morphia.annotations.Entity;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Persistent
@FieldNameConstants(innerTypeName = "NgCertificateKeys")
@StoreIn(DbAliases.NG_MANAGER)
@Entity(value = "ngCertificates", noClassnameStored = true)
@Document("ngCertificates")
@TypeAlias("ngCertificates")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NgCertificate {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("account_org_project_identifier_unique_index")
                 .unique(true)
                 .field(NgCertificateKeys.accountIdentifier)
                 .field(NgCertificateKeys.orgIdentifier)
                 .field(NgCertificateKeys.projectIdentifier)
                 .field(NgCertificateKeys.identifier)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("account_org_project_createdAt_desc_sort_index")
                 .field(NgCertificateKeys.accountIdentifier)
                 .field(NgCertificateKeys.orgIdentifier)
                 .field(NgCertificateKeys.projectIdentifier)
                 .descSortField(NgCertificateKeys.createdAt)
                 .build())
        .build();
  }
  @Id @dev.morphia.annotations.Id String id;
  @NotEmpty String name;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String certificate;
  String description;
  List<NGTag> tags;
  @NotNull CertificateInputSpecType inputSpecType;
  InputSpec inputSpec;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedDate;
}
