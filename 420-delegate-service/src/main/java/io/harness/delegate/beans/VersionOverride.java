/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.SecondaryStoreIn;
import io.harness.annotations.StoreIn;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "VersionOverrideKeys")
@Data
@Builder
@StoreIn(DbAliases.HARNESS)
@SecondaryStoreIn(DbAliases.DMS)
@Entity(value = "versionOverride", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class VersionOverride implements PersistentEntity, YamlDTO {
  @Id @NotNull @Builder.Default private final String uuid = generateUuid();
  @NotEmpty private final String accountId;
  private final String version;
  private final VersionOverrideType overrideType;
  private boolean validTillNextRelease;
  @FdTtlIndex @Builder.Default private final Date validUntil = DateTime.now().toDate();

  public static VersionOverrideBuilder builder(final String accountId) {
    return new VersionOverrideBuilder().accountId(accountId);
  }
  public static VersionOverrideBuilder auditBuilder(final String version, final String accountId) {
    return new VersionOverrideBuilder().version(version);
  }
}
