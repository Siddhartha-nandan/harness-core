/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.kryo.ProtobufKryoSerializer;
import io.harness.pms.contracts.data.StepOutcomeRef;

@OwnedBy(HarnessTeam.PIPELINE)
public class StepOutcomeRefKryoSerializer extends ProtobufKryoSerializer<StepOutcomeRef> {
  private static StepOutcomeRefKryoSerializer instance;

  private StepOutcomeRefKryoSerializer() {}

  public static synchronized StepOutcomeRefKryoSerializer getInstance() {
    if (instance == null) {
      instance = new StepOutcomeRefKryoSerializer();
    }
    return instance;
  }
}
