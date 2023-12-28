/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.retrypolicy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSdkRetryPolicySpec {
  private String backOffStrategyType;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "backOffStrategyType",
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  private AwsSdkBackoffStrategySpec backOffStrategy;
}
