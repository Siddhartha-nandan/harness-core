/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.event;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.StoreIn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn("harnessci")
@Entity(value = "ciAccountDataStatus", noClassnameStored = true)
@Document("ciAccountDataStatus")
@HarnessEntity(exportable = true)
@TypeAlias("ciAccountDataStatus")
public class CIAccountDataStatus {
  @Id @dev.morphia.annotations.Id String uuid;
  String accountId;
  Boolean deleted;
  Long lastSent;
}