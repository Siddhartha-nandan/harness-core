/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public class OrchestrationStepTypes {
  private OrchestrationStepTypes() {}

  public static final String DUMMY = "DUMMY";
  public static final String PIPELINE_SECTION = "PIPELINE_SECTION";
  public static final String NG_STAGES_STEP = "STAGES_STEP";
  public static final String APPROVAL_STAGE = "APPROVAL_STAGE";
  public static final String CUSTOM_STAGE = "CUSTOM_STAGE";
  public static final String CUSTOM_STAGE_V1 = "CUSTOM_STAGE_V1";
  public static final String PIPELINE_STAGE = "PIPELINE_STAGE";
  public static final String PIPELINE_ROLLBACK_STAGE = "PIPELINE_ROLLBACK_STAGE";
  public static final String FLAG_CONFIGURATION = "FLAG_CONFIGURATION";
  public static final String FLAG_STAGE = "FLAG_STAGE";

  public static final String IDENTITY_STEP = "IDENTITY_STEP";
}
