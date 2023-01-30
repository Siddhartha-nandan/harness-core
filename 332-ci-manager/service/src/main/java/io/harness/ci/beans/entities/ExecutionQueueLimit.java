/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.app.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.StoreIn;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CIExecutionConfigKeys")
@StoreIn(DbAliases.CIMANAGER)
@Entity(value = "executionqueuelimit", noClassnameStored = true)
@Document("executionqueuelimit")
@HarnessEntity(exportable = true)
@TypeAlias("executionqueuelimit")
@RecasterAlias("io.harness.app.beans.entities.ExecutionQueueLimit")
public class ExecutionQueueLimit {
  @Id @dev.morphia.annotations.Id String uuid;
  @NotBlank String accountIdentifier;
  String macExecLimit;
  String totalExecLimit;
}
