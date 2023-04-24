/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EcsNGUtils {
  public static final String ECS_SERVICE_NOT_FOUND_ERROR_MESSAGE =
      "ECS Service doesn't exist, so not able to delete it";
}
