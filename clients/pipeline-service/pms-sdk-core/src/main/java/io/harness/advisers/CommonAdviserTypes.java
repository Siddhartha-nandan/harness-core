/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum CommonAdviserTypes {
  RETRY_WITH_ROLLBACK,
  MANUAL_INTERVENTION_WITH_ROLLBACK,
  ON_FAIL_ROLLBACK,
  ON_FAIL_PIPELINE_ROLLBACK,
  RETRY_STEPGROUP;
}
