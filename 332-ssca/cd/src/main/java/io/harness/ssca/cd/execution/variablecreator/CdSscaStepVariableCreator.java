/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.cd.execution.variablecreator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.ssca.beans.SscaConstants;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.SSCA)
public class CdSscaStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(SscaConstants.CD_SSCA_ORCHESTRATION, SscaConstants.CD_SSCA_ENFORCEMENT);
  }
}
