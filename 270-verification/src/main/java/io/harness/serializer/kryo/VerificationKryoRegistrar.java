/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.kryo.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CV)
public class VerificationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to register
  }
}
