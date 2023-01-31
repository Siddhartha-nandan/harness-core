/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.IACM)
public class IACMCommonEndpointConstants {
  public static final String IACM_SERVICE_TOKEN_ENDPOINT = "api/v2/token";

  public static final String IACM_SERVICE_GET_STACK_ENDPOINT = "/api/orgs/{org}/projects/{project}/stacks/{stackId}";
  public static final String IACM_SERVICE_GET_STACK_VARIABLES_ENDPOINT =
      "/api/orgs/{org}/projects/{project}/stacks/{stackId}/variables";
  public static final String IACM_SERVICE_POST_EXECUTION = "/api/orgs/{org}/projects/{project}/executions";
}
