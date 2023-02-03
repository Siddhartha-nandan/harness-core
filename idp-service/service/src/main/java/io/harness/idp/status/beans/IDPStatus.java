/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.status.beans;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@StoreIn(DbAliases.IDP)
@Entity(value = "idpStatus", noClassnameStored = true)
@Document("idpStatus")
@Persistent
@OwnedBy(HarnessTeam.IDP)
public class IDPStatus implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  @FdUniqueIndex private String accountIdentifier;
  private StatusEnum status;
  private String comments;
  @LastModifiedDate Long lastModifiedAt;
  private boolean isDeleted;
  private long deletedAt;
}
