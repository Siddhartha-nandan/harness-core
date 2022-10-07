/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.facilitation.facilitator.child;

import static io.harness.pms.contracts.execution.ExecutionMode.CHILD;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.execution.facilitator.DefaultFacilitatorParams;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializerWrapper;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ChildFacilitatorTest extends OrchestrationTestBase {
  @Inject private ChildFacilitator childFacilitator;
  @Inject private KryoSerializerWrapper kryoSerializerWrapper;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    byte[] parameters = kryoSerializerWrapper.asBytes(DefaultFacilitatorParams.builder().build());
    FacilitatorResponseProto response = childFacilitator.facilitate(ambiance, parameters);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(CHILD);
    assertThat(response.getInitialWait().getSeconds()).isEqualTo(0);
  }
}
