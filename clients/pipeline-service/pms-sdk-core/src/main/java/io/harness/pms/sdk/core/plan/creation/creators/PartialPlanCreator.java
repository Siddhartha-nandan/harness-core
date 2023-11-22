/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YamlField;

import java.util.Map;
import java.util.Set;

public interface PartialPlanCreator<T> {
  default Class<T> getFieldClass() {
    throw new InvalidRequestException(
        String.format("The planCreator %s must implement the getFieldClass method.", this.getClass().getSimpleName()));
  }

  default T getFieldObject(YamlField field) {
    throw new InvalidRequestException(
        String.format("The planCreator %s must implement the getFieldObject method.", this.getClass().getSimpleName()));
  }

  Map<String, Set<String>> getSupportedTypes();

  PlanCreationResponse createPlanForField(PlanCreationContext ctx, T field);

  default String getExecutionInputTemplateAndModifyYamlField(YamlField yamlField) {
    return "";
  }

  default Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V0, HarnessYamlVersion.V1);
  }
}
