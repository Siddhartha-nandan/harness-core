/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.serializer.kryo.serializers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.kryo.ProtobufKryoSerializer;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;

@OwnedBy(PIPELINE)
public class InterruptEffectKryoSerializer extends ProtobufKryoSerializer<InterruptEffectProto> {
  private static InterruptEffectKryoSerializer instance;

  public InterruptEffectKryoSerializer() {}

  public static synchronized InterruptEffectKryoSerializer getInstance() {
    if (instance == null) {
      instance = new InterruptEffectKryoSerializer();
    }
    return instance;
  }
}
