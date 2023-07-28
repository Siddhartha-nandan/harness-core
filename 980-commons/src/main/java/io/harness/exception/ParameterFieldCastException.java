/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;
import static io.harness.eraro.ErrorCode.PARAMETER_FIELD_CAST_ERROR;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.eraro.Level;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldCastException extends WingsException {
  // This exception can be used when there's a class cast error while converting ParameterDocumentField to
  // ParameterField
  private static final String MESSAGE_ARG = "message";

  public ParameterFieldCastException(String message) {
    super(message, null, PARAMETER_FIELD_CAST_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
