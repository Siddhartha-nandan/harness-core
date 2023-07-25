/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.common.ExpressionMode;
import io.harness.pms.expression.ProcessorResult;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorFactory;
import io.harness.pms.yaml.validation.RuntimeValidator;
import io.harness.pms.yaml.validation.RuntimeValidatorResponse;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldProcessor {
  private final EngineExpressionEvaluator engineExpressionEvaluator;
  private final InputSetValidatorFactory inputSetValidatorFactory;
  private final ExpressionMode expressionMode;

  public ParameterFieldProcessor(EngineExpressionEvaluator engineExpressionEvaluator,
      InputSetValidatorFactory inputSetValidatorFactory, ExpressionMode expressionMode) {
    this.engineExpressionEvaluator = engineExpressionEvaluator;
    this.inputSetValidatorFactory = inputSetValidatorFactory;
    this.expressionMode = expressionMode;
  }

  public ProcessorResult process(ParameterDocumentField field) {
    if (field == null || field.isSkipAutoEvaluation()) {
      return ProcessorResult.builder().build();
    }

    Object newValue;
    InputSetValidator inputSetValidator = field.getInputSetValidator();
    if (field.isExpression()) {
      if (field.isTypeString()) {
        newValue = engineExpressionEvaluator.renderExpression(field.getExpressionValue(), expressionMode);
      } else {
        newValue = engineExpressionEvaluator.evaluateExpression(field.getExpressionValue(), expressionMode);
      }

      if (newValue instanceof String && EngineExpressionEvaluator.hasExpressions((String) newValue)) {
        String newExpression = (String) newValue;
        if (newExpression.equals(field.getExpressionValue())) {
          return ProcessorResult.builder().build();
        }

        field.updateWithExpression(newExpression);
        return validateUsingValidator(newValue, inputSetValidator);
      }

      field.updateWithValue(newValue);
    }

    if (field.getValueDoc() == null) {
      return ProcessorResult.builder().build();
    }

    Map<String, Object> map = field.getValueDoc();
    Object valueField = map.get(ParameterFieldValueWrapper.VALUE_FIELD);
    if (valueField != null) {
      Object finalValue = engineExpressionEvaluator.resolve(valueField, expressionMode);
      if (finalValue != null) {
        finalValue = getCastedFinalValueForPrimitiveTypesAndWrappers(finalValue, field);
        field.updateWithValue(finalValue);
        ProcessorResult processorResult = validateUsingValidator(finalValue, inputSetValidator);
        if (processorResult.isError()) {
          return processorResult;
        }
      }
    }

    return ProcessorResult.builder().build();
  }

  private ProcessorResult validateUsingValidator(Object value, InputSetValidator inputSetValidator) {
    if (inputSetValidator == null) {
      return ProcessorResult.builder().build();
    }

    RuntimeValidator runtimeValidator =
        inputSetValidatorFactory.obtainValidator(inputSetValidator, engineExpressionEvaluator, expressionMode);
    RuntimeValidatorResponse validatorResponse =
        runtimeValidator.isValidValue(value, inputSetValidator.getParameters());
    if (!validatorResponse.isValid()) {
      return ProcessorResult.builder()
          .error(true)
          .expression(String.format(
              "<+input>.%s(%s)", inputSetValidator.getValidatorType().getYamlName(), inputSetValidator.getParameters()))
          .message(validatorResponse.getErrorMessage())
          .build();
    }
    return ProcessorResult.builder().build();
  }

  // Handling primitive types and wrappers when value class of document field doesn't match the class of finalValue
  private Object getCastedFinalValueForPrimitiveTypesAndWrappers(Object finalValue, ParameterDocumentField field) {
    if (field.getValueClass() == null) {
      return finalValue;
    }
    try {
      Class<?> fieldClass = Class.forName(field.getValueClass());
      if (ClassUtils.isPrimitiveOrWrapper(fieldClass) && !fieldClass.isAssignableFrom(finalValue.getClass())) {
        if (fieldClass.equals(Integer.class)) {
          if (finalValue.getClass().equals(String.class)) {
            finalValue = Integer.parseInt(finalValue.toString());
          } else if (finalValue.getClass().equals(Double.class)) {
            finalValue = ((Double) finalValue).intValue();
          }
        } else if (fieldClass.equals(Double.class)) {
          if (finalValue.getClass().equals(String.class)) {
            finalValue = Double.valueOf((String) finalValue);
          } else if (finalValue.getClass().equals(Integer.class)) {
            finalValue = new Double((Integer) finalValue);
          }
        }
      }
    } catch (Exception ex) {
      log.warn(String.format(
                   "[ParameterFieldProcessor] Exception in casting newValue of type %s into parameter field of type %s",
                   finalValue.getClass().toString(), field.getValueClass()),
          ex);
    }
    return finalValue;
  }
}
