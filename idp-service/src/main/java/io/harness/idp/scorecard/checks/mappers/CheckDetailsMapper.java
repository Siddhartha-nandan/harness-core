/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.checks.mappers;

import static io.harness.idp.common.Constants.DOT_SEPARATOR;
import static io.harness.idp.common.Constants.SPACE_SEPARATOR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.checks.entity.CheckEntity;
import io.harness.idp.scorecard.checks.entity.CheckStatusEntity;
import io.harness.spec.server.idp.v1.model.CheckDetails;
import io.harness.spec.server.idp.v1.model.Rule;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.IDP)
@UtilityClass
public class CheckDetailsMapper {
  private static final String IN_OR_MATCH_OPERATOR = "=~";
  private static final String NOT_IN_OR_NOT_MATCH_OPERATOR = "!~";
  public static final List<String> OPERATORS_WITH_ARRAYS = List.of(IN_OR_MATCH_OPERATOR, NOT_IN_OR_NOT_MATCH_OPERATOR);

  public CheckDetails toDTO(CheckEntity checkEntity, CheckStatusEntity checkStatusEntity) {
    CheckDetails checkDetails = new CheckDetails();
    checkDetails.setName(checkEntity.getName());
    checkDetails.setIdentifier(checkEntity.getIdentifier());
    checkDetails.setDescription(checkEntity.getDescription());
    checkDetails.setExpression(checkEntity.getExpression());
    checkDetails.setCustom(checkEntity.isCustom());
    checkDetails.defaultBehaviour(checkEntity.getDefaultBehaviour());
    checkDetails.setFailMessage(checkEntity.getFailMessage());
    checkDetails.setRuleStrategy(checkEntity.getRuleStrategy());
    checkDetails.setRules(checkEntity.getRules());
    checkDetails.setTags(checkEntity.getTags());
    checkDetails.setPercentage(CheckMapper.calculatePercentage(checkStatusEntity));
    return checkDetails;
  }

  public CheckEntity fromDTO(CheckDetails checkDetails, String accountIdentifier) {
    String expression = constructDisplayExpression(checkDetails.getRules(), checkDetails.getRuleStrategy());
    return CheckEntity.builder()
        .accountIdentifier(accountIdentifier)
        .identifier(checkDetails.getIdentifier())
        .name(checkDetails.getName())
        .description(checkDetails.getDescription())
        .expression(expression)
        .isCustom(checkDetails.isCustom())
        .defaultBehaviour(checkDetails.getDefaultBehaviour())
        .failMessage(checkDetails.getFailMessage())
        .ruleStrategy(checkDetails.getRuleStrategy())
        .rules(checkDetails.getRules())
        .tags(checkDetails.getTags())
        .build();
  }

  public static String constructExpressionFromRules(
      List<Rule> rules, CheckDetails.RuleStrategyEnum ruleStrategy, String dpValueSuffix, boolean getLhsOnly) {
    return rules.stream()
        .map(rule -> CheckDetailsMapper.getExpression(rule, dpValueSuffix, getLhsOnly))
        .collect(Collectors.joining(SPACE_SEPARATOR
            + (ruleStrategy.equals(CheckDetails.RuleStrategyEnum.ALL_OF) ? "&&" : "||") + SPACE_SEPARATOR));
  }

  String getExpression(Rule rule, String dpValueSuffix, boolean getLhsOnly) {
    StringBuilder expressionBuilder = new StringBuilder(rule.getDataSourceIdentifier())
                                          .append(DOT_SEPARATOR)
                                          .append("\"")
                                          .append(rule.getIdentifier())
                                          .append("\"");

    if (StringUtils.isNotBlank(dpValueSuffix)) {
      expressionBuilder.append(DOT_SEPARATOR);
      expressionBuilder.append(dpValueSuffix);
    }

    if (!getLhsOnly) {
      expressionBuilder.append(rule.getOperator());

      // Do not escape value with quotes for IN/NOT_IN operator as the items inside values will be escaped.
      // Example value : [\"3.0.0\",\"3.0.1\",\"3.0.3\"]
      if (OPERATORS_WITH_ARRAYS.contains(rule.getOperator()) && rule.getValue().startsWith("[")
          && rule.getValue().endsWith("]")) {
        expressionBuilder.append(rule.getValue());
      } else {
        expressionBuilder.append("\"");
        expressionBuilder.append(rule.getValue());
        expressionBuilder.append("\"");
      }
    }

    return expressionBuilder.toString();
  }

  private static String constructDisplayExpression(List<Rule> rules, CheckDetails.RuleStrategyEnum ruleStrategy) {
    return rules.stream()
        .map(CheckDetailsMapper::getDisplayExpression)
        .collect(Collectors.joining(SPACE_SEPARATOR
            + (ruleStrategy.equals(CheckDetails.RuleStrategyEnum.ALL_OF) ? "&&" : "||") + SPACE_SEPARATOR));
  }

  private static String getDisplayExpression(Rule rule) {
    StringBuilder expressionBuilder =
        new StringBuilder(rule.getDataSourceIdentifier()).append(DOT_SEPARATOR).append(rule.getDataPointIdentifier());

    rule.getInputValues().forEach(inputValue -> {
      String inputValueReplaced = inputValue.getValue().replace("\"", "");
      expressionBuilder.append(DOT_SEPARATOR);
      expressionBuilder.append(inputValueReplaced);
    });

    expressionBuilder.append(rule.getOperator());
    expressionBuilder.append(rule.getValue());
    return expressionBuilder.toString();
  }
}
