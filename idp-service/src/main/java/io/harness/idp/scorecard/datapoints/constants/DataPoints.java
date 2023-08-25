/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.IDP)
public class DataPoints {
  // Github
  public static final String GITHUB_PULL_REQUEST_MEAN_TIME_TO_MERGE = "meanTimeToMerge";
  public static final String GITHUB_IS_BRANCH_PROTECTED = "isBranchProtected";
  public static final String STO_ADDED_IN_PIPELINE = "stoStageAdded";
  public static final String IS_POLICY_EVALUATION_SUCCESSFUL_IN_PIPELINE = "isPolicyEvaluationSuccessful";
  public static final String PERCENTAGE_OF_CI_PIPELINE_FAILING_IN_SEVEN_DAYS = "%ofCIPipelinePassingInPast7Days";
  public static final String PIPELINE_TEST_FAILING_IN_CI_IS_ZERO = "noTestsFailingInCiPipeline";
}
