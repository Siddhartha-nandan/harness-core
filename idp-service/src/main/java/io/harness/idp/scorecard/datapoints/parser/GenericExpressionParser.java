/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datapoints.parser;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.Constants.DATA_POINT_VALUE_KEY;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;
import static io.harness.idp.common.Constants.DOT_SEPARATOR;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.INVALID_CONDITIONAL_INPUT;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.INVALID_EXPRESSION;
import static io.harness.idp.scorecard.datapoints.constants.Inputs.EXPRESSION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.common.ExpressionMode;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.expression.IdpExpressionEvaluator;
import io.harness.idp.scorecard.scores.beans.DataFetchDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.harness.spec.server.idp.v1.model.InputValue;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
public class GenericExpressionParser implements DataPointParser {
  @Override
  public Object parseDataPoint(Map<String, Object> data, DataFetchDTO dataFetchDTO) {
    DataPointEntity dataPoint = dataFetchDTO.getDataPoint();
    String outcomeExpression = dataPoint.getOutcomeExpression();
    Map<String, Object> dataPointResponse = new HashMap<>();
    if (outcomeExpression == null) {
      List<InputValue> inputValues = dataFetchDTO.getInputValues();
      if (isEmpty(inputValues)) {
        dataPointResponse.putAll(constructDataPointInfo(dataFetchDTO, null, INVALID_CONDITIONAL_INPUT));
        return dataPointResponse;
      }
      Optional<InputValue> jexl =
              inputValues.stream().filter(inputValue -> inputValue.getKey().equals(EXPRESSION)).findFirst();
      if (jexl.isEmpty()) {
        dataPointResponse.putAll(constructDataPointInfo(dataFetchDTO, null, INVALID_EXPRESSION));
        return dataPointResponse;
      }
      outcomeExpression = dataPoint.getDataSourceIdentifier() + DOT_SEPARATOR + jexl.get().getValue();
    }
    Map<String, Map<String, Object>> expressionData = new HashMap<>();
    expressionData.put(dataPoint.getDataSourceIdentifier(), data);
    IdpExpressionEvaluator evaluator = new IdpExpressionEvaluator(expressionData);

    Object value = null;
    dataPointResponse.put(ERROR_MESSAGE_KEY, "");
    try {
      value = evaluator.evaluateExpression(outcomeExpression, ExpressionMode.RETURN_NULL_IF_UNRESOLVED);
      if (value == null) {
        log.warn(
            "Could not find the required data by evaluating expression for data point {}", dataPoint.getIdentifier());
        dataPointResponse.put(ERROR_MESSAGE_KEY, "Missing Data");
      }
    } catch (Exception e) {
      log.warn("Datapoint expression evaluation failed for data point {}", dataPoint.getIdentifier(), e);
      dataPointResponse.put(ERROR_MESSAGE_KEY, "Datapoint extraction expression evaluation failed");
    }
    dataPointResponse.put(DATA_POINT_VALUE_KEY, value);
    return Map.of(dataFetchDTO.getRuleIdentifier(), dataPointResponse);
  }
}
