/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.governance.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("AZURE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureRecommendationAdhocDTO implements RecommendationAdhocDTO {
  String tenantId;
  String subscriptionId;
  String clientId;
  String cloudConnectorId;

  @Override
  public String getRoleInfo() {
    return null;
  }

  @Override
  public String getRoleId() {
    return clientId;
  }

  @Override
  public String getTargetInfo() {
    return subscriptionId;
  }

  @Override
  public String getTenantInfo() {
    return tenantId;
  }
}
