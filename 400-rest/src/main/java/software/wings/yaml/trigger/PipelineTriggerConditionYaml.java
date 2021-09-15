/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("PIPELINE_COMPLETION")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class PipelineTriggerConditionYaml extends TriggerConditionYaml {
  private String pipelineName;

  public PipelineTriggerConditionYaml() {
    super.setType("PIPELINE_COMPLETION");
  }
  @lombok.Builder
  PipelineTriggerConditionYaml(String pipelineName) {
    super.setType("PIPELINE_COMPLETION");
    this.pipelineName = pipelineName;
  }
}
