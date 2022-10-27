/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azureconnector.outcome;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.azureconnector.AzureConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(HarnessTeam.CDP)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureManualDetailsOutcomeDTO.class, name = AzureConstants.MANUAL_CONFIG)
  ,
      @JsonSubTypes.Type(
          value = AzureInheritFromDelegateDetailsOutcomeDTO.class, name = AzureConstants.INHERIT_FROM_DELEGATE)
})
public interface AzureCredentialSpecOutcomeDTO {}
