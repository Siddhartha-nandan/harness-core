/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.pms.yaml.YAMLFieldNameConstants.FAILURE_STRATEGIES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.ROLLBACK_STEPS;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackParameters.OnFailRollbackParametersBuilder;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.NGCommonUtilPlanCreationConstants;
import io.harness.plancreator.steps.GenericPlanCreatorUtils;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorUtilsCommon {
  public List<FailureStrategyConfig> getFailureStrategies(YamlNode node) {
    YamlField failureStrategy = node.getField(FAILURE_STRATEGIES);
    ParameterField<List<FailureStrategyConfig>> failureStrategyConfigs = null;

    try {
      if (failureStrategy != null) {
        failureStrategyConfigs = YamlUtils.read(
            failureStrategy.getNode().toString(), new TypeReference<ParameterField<List<FailureStrategyConfig>>>() {});
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    // If failureStrategies configured as <+input> and no value is given, failureStrategyConfigs.getValue() will still
    // be null and handled as empty list
    if (ParameterField.isNotNull(failureStrategyConfigs)) {
      return failureStrategyConfigs.getValue();
    } else {
      return null;
    }
  }

  public List<FailureStrategyConfig> getFieldFailureStrategies(
      YamlField currentField, String fieldName, boolean isStepInsideRollback) {
    YamlNode fieldNode = YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), fieldName);
    if (isStepInsideRollback && fieldNode != null) {
      // Check if found fieldNode is within rollbackSteps section
      YamlNode rollbackNode = YamlUtils.findParentNode(fieldNode, ROLLBACK_STEPS);
      if (rollbackNode == null) {
        return Collections.emptyList();
      }
    }
    if (fieldNode != null) {
      return getFailureStrategies(fieldNode);
    }
    return Collections.emptyList();
  }

  public OnFailRollbackParameters getRollbackParameters(
      YamlField currentField, Set<FailureType> failureTypes, RollbackStrategy rollbackStrategy) {
    OnFailRollbackParametersBuilder rollbackParametersBuilder = OnFailRollbackParameters.builder();
    rollbackParametersBuilder.applicableFailureTypes(failureTypes);
    rollbackParametersBuilder.strategy(rollbackStrategy);
    rollbackParametersBuilder.strategyToUuid(getRollbackStrategyMap(currentField));
    return rollbackParametersBuilder.build();
  }

  public Map<RollbackStrategy, String> getRollbackStrategyMap(YamlField currentField) {
    String stageNodeId = GenericPlanCreatorUtils.getStageNodeId(currentField);
    Map<RollbackStrategy, String> rollbackStrategyStringMap = new HashMap<>();
    rollbackStrategyStringMap.put(
        RollbackStrategy.STAGE_ROLLBACK, stageNodeId + NGCommonUtilPlanCreationConstants.COMBINED_ROLLBACK_ID_SUFFIX);
    rollbackStrategyStringMap.put(
        RollbackStrategy.STEP_GROUP_ROLLBACK, GenericPlanCreatorUtils.getStepGroupRollbackStepsNodeId(currentField));
    rollbackStrategyStringMap.put(
        RollbackStrategy.PIPELINE_ROLLBACK, GenericPlanCreatorUtils.getRollbackStageNodeId(currentField));
    return rollbackStrategyStringMap;
  }
}
