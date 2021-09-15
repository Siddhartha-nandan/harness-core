/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@Data
@Builder
public class ServiceInfraWorkflow {
  private String infraMappingId;
  private String infraMappingName;
  private String workflowId;
  private String workflowName;
  private WorkflowType workflowType;
}
