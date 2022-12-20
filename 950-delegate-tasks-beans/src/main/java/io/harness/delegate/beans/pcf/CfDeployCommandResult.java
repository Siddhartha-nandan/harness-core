/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.pcf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class CfDeployCommandResult {
  /**
   * This list represents apps updated by deploy state,
   * AppName : previousCount : DesiredCount (one updated by deploy)
   * Rollback will use this data but will reverse counts
   */
  private List<CfServiceData> instanceDataUpdated;
  private List<CfInternalInstanceElement> oldAppInstances;
  private CfInBuiltVariablesUpdateValues updatedValues;
  private List<CfInternalInstanceElement> newAppInstances;
}
