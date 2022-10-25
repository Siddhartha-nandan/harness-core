/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.stresstesting.script;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.kryo.KryoSerializer;
import io.harness.testing.DelegateTaskStressTest;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.DEL)
public abstract class StressTestGenerator {
  @Inject KryoSerializer kryoSerializer;

  public abstract DelegateTaskStressTest makeStressTest();
}
