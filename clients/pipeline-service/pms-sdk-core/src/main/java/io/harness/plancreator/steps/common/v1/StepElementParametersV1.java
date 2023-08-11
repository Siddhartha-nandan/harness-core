/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps.common.v1;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.policy.PolicyConfig;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.sdk.core.steps.io.v1.StepParametersV1;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("StepElementParametersV1")
@RecasterAlias("io.harness.plancreator.steps.common.v1.StepElementParametersV1")
public class StepElementParametersV1 implements StepParametersV1 {
  String uuid;
  String identifier;
  String name;
  String description;
  ParameterField<String> timeout;
  List<FailureStrategyConfig> failureStrategies;

  ParameterField<String> skipCondition;
  String when;

  String type;
  SpecParameters spec;
  PolicyConfig enforce;

  ParameterField<List<String>> delegateSelectors;

  // Only for rollback failures
  OnFailRollbackParameters rollbackParameters;

  @Override
  public String toViewJson() {
    StepElementParametersV1 stepElementParameters = cloneParameters(false, false);
    if (spec != null) {
      stepElementParameters.setSpec(spec.getViewJsonObject());
    }
    return RecastOrchestrationUtils.toJson(stepElementParameters);
  }

  public StepElementParametersV1 cloneParameters(boolean includeUuid, boolean includeSpec) {
    return StepElementParametersV1.builder()
        .uuid(includeUuid ? this.uuid : null)
        .type(this.type)
        .name(this.name)
        .spec(includeSpec ? this.spec : null)
        .description(this.description)
        .identifier(this.identifier)
        .timeout(this.timeout)
        .enforce(this.enforce)
        .failureStrategies(this.failureStrategies)
        .when(this.when)
        .skipCondition(this.skipCondition)
        .delegateSelectors(this.delegateSelectors)
        .build();
  }
}
