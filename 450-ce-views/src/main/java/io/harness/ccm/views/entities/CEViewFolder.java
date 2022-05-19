/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import com.google.common.collect.ImmutableList;
import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "CEViewFolderKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceViewFolder", noClassnameStored = true)
@Schema(description = "This object will contain the complete definition of a Cloud Cost Perspective")
public final class CEViewFolder implements PersistentEntity, CreatedAtAware, UpdatedAtAware, AccountAccess,
                                     CreatedByAware, UpdatedByAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
            .name("accountId")
            .field(CEViewFolderKeys.accountId)
            .build())
        .build();
  }
  @Id ObjectId uuid;
  String accountId;
  @Size(min = 1, max = 80, message = "for view must be between 1 and 80 characters long") @NotBlank String name;
  boolean pinned;
  List<String> tags;
  String description;
  ViewType viewType = ViewType.CUSTOMER;

  long createdAt;
  long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
