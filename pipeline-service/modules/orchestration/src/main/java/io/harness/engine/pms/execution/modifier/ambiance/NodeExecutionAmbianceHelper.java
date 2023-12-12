/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.modifier.ambiance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class NodeExecutionAmbianceHelper {
  // We are planning to remove ambiance object from nodeExecution collection and eventually replacing it with a new
  // object named Locale . This object will have all the fields same as ambiance except for the execution metadata.
  //
  // For that we have divided the entire process in 2 steps .
  //
  // We will remove usage of execution metadata from node executions ambiance. We will be doing that by replacing
  // nodeExecution.getAmbiance() in all the places where we use metadata from the nodeExecutionAmbiance with a new
  // execution ambiance which is formed by combination of Locale and setting the metadata using metadata from plan
  // execution. Currently, we are returning the ambiance directly from nodeExecution.
  //
  // We will later create another method that takes nodeExecution and planExecution as parameters and uses locale and
  // metadata from planExecution to form and return ambiance
  public Ambiance getExecutionAmbiance(NodeExecution nodeExecution) {
    return nodeExecution.getAmbiance();
  }
}
