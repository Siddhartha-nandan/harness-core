/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.yaml.YAMLFieldNameConstants.NAME;
import static io.harness.pms.yaml.YAMLFieldNameConstants.REQUIRED;
import static io.harness.pms.yaml.YAMLFieldNameConstants.VALUE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.VARIABLES;
import static io.harness.pms.yaml.validation.RuntimeInputValuesValidator.validateStaticValues;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsEngineExpressionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.ExecutionInputInstance;
import io.harness.execution.ExecutionInputInstance.ExecutionInputInstanceKeys;
import io.harness.execution.NodeExecution;
import io.harness.expression.common.ExpressionMode;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.NodeProjectionUtils;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.merger.helpers.YamlSubMapExtractor;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.ExecutionInputRepository;
import io.harness.waiter.WaitNotifyEngine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.serializer.utils.NGRuntimeInputUtils;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class ExecutionInputServiceImpl implements ExecutionInputService {
  @Inject WaitNotifyEngine waitNotifyEngine;
  @Inject ExecutionInputRepository executionInputRepository;
  @Inject ExecutionInputServiceHelper executionInputServiceHelper;
  @Inject NodeExecutionService nodeExecutionService;
  @Inject OrchestrationEngine engine;
  @Inject PmsEngineExpressionService pmsEngineExpressionService;
  @Override
  // TODO(BRIJESH): Use lock so that only one input can be processed and only one doneWith should be called.
  public boolean continueExecution(String nodeExecutionId, String executionInputYaml) {
    ExecutionInputInstance executionInputInstance;
    try {
      executionInputInstance = mergeUserInputInTemplate(nodeExecutionId, executionInputYaml, false);
    } catch (NoSuchElementException ex) {
      log.error("User input could not be processed for nodeExecutionId {}", nodeExecutionId, ex);
      return false;
    }
    waitNotifyEngine.doneWith(executionInputInstance.getInputInstanceId(),
        ExecutionInputData.builder().inputInstanceId(executionInputInstance.getInputInstanceId()).build());
    return true;
  }

  @Override
  public boolean continueWithDefault(String nodeExecutionId) {
    try {
      mergeUserInputInTemplate(nodeExecutionId, "", true);
    } catch (NoSuchElementException ex) {
      log.error("User input could not be processed for nodeExecutionId {}", nodeExecutionId, ex);
      return false;
    }

    NodeExecution nodeExecution = nodeExecutionService.updateStatusWithOps(
        nodeExecutionId, Status.QUEUED, null, EnumSet.of(Status.INPUT_WAITING));
    engine.startNodeExecution(nodeExecution.getAmbiance());
    return true;
  }

  @Override
  public ExecutionInputInstance getExecutionInputInstance(String nodeExecutionId) {
    Optional<ExecutionInputInstance> optional = executionInputRepository.findByNodeExecutionId(nodeExecutionId);
    if (optional.isPresent()) {
      return optional.get();
    }
    log.info("Execution Input template does not exist for input execution id: {}", nodeExecutionId);
    return null;
  }

  @Override
  public boolean isPresent(String nodeExecutionId) {
    Optional<ExecutionInputInstance> optional = executionInputRepository.findByNodeExecutionId(nodeExecutionId);
    return optional.isPresent();
  }

  @Override
  public ExecutionInputInstance save(ExecutionInputInstance executionInputInstance) {
    return executionInputRepository.save(executionInputInstance);
  }

  @Override
  public List<ExecutionInputInstance> getExecutionInputInstances(Set<String> nodeExecutionIds) {
    return executionInputRepository.findByNodeExecutionIds(nodeExecutionIds);
  }

  private ExecutionInputInstance mergeUserInputInTemplate(
      String nodeExecutionId, String executionInputYaml, boolean continueWithDefault) {
    Optional<ExecutionInputInstance> optional = executionInputRepository.findByNodeExecutionId(nodeExecutionId);
    if (optional.isPresent()) {
      ExecutionInputInstance executionInputInstance = optional.get();
      if (isEmpty(executionInputYaml)) {
        executionInputYaml = executionInputInstance.getTemplate();
      }
      JsonNode userInputJsonNode = YamlUtils.readAsJsonNode(executionInputYaml);
      checkValueForRequiredVariablesProvided(
          executionInputInstance.getFieldYaml(), userInputJsonNode, continueWithDefault);
      Ambiance ambiance =
          nodeExecutionService.getWithFieldsIncluded(nodeExecutionId, NodeProjectionUtils.withAmbianceAndStatus)
              .getAmbiance();
      userInputJsonNode = (JsonNode) pmsEngineExpressionService.resolve(
          ambiance, userInputJsonNode, ExpressionMode.RETURN_ORIGINAL_EXPRESSION_IF_UNRESOLVED);

      JsonNode templateJsonNode = YamlUtils.readAsJsonNode(executionInputInstance.getTemplate());
      Map<FQN, String> invalidFQNsInInputSet = getInvalidFQNsInInputSet(templateJsonNode, userInputJsonNode);
      if (!isEmpty(invalidFQNsInInputSet)) {
        throw new InvalidRequestException("Some fields are not valid: "
            + invalidFQNsInInputSet.keySet().stream().map(FQN::getExpressionFqn).collect(Collectors.toList()));
      }
      JsonNode mergedJsonNode =
          MergeHelper.mergeExecutionInputIntoOriginalJsonNode(templateJsonNode, userInputJsonNode, false);
      executionInputInstance.setMergedInputTemplate(
          executionInputServiceHelper.getExecutionInputMap(templateJsonNode, mergedJsonNode));
      executionInputInstance.setUserInput(YamlUtils.writeYamlString(userInputJsonNode));
      return executionInputRepository.save(executionInputInstance);
    } else {
      throw new InvalidRequestException(
          String.format("Execution Input template does not exist for input execution id : %s", nodeExecutionId));
    }
  }

  public void checkValueForRequiredVariablesProvided(
      String fieldYaml, JsonNode executionInputNode, boolean continueWithDefault) {
    Map<String, String> executionInputYamlVariables = new HashMap<>();
    // Storing the variables present in execution input yaml at first level and storing them in a map as key value pair
    // . Expressions are also present as string only since we dont want to throw error in case the expression is
    // resolved to empty value
    if (executionInputNode.fields() != null && executionInputNode.fields().hasNext()
        && executionInputNode.fields().next().getValue().get(VARIABLES) != null) {
      ArrayNode executionInputVariables = (ArrayNode) executionInputNode.fields().next().getValue().get(VARIABLES);
      executionInputVariables.forEach(
          jsonNode -> executionInputYamlVariables.put(jsonNode.get(NAME).asText(), jsonNode.get(VALUE).asText()));
    }

    // Fetching variables from the field yaml at first level
    JsonNode fieldYamlNode = YamlUtils.readAsJsonNode(fieldYaml);
    if (fieldYamlNode.fields() != null && fieldYamlNode.fields().hasNext()
        && fieldYamlNode.fields().next().getValue().get(VARIABLES) != null) {
      ArrayNode fieldYamlVariables = (ArrayNode) fieldYamlNode.fields().next().getValue().get(VARIABLES);
      fieldYamlVariables.forEach(jsonNode -> {
        // Checking if variable in field yaml is an execution input and required
        if (NGExpressionUtils.matchesExecutionInputPattern(jsonNode.get(VALUE).asText())
            && jsonNode.get(REQUIRED) != null && jsonNode.get(REQUIRED).asBoolean()) {
          String variableName = jsonNode.get(NAME).asText();
          // Throwing error if value of the variable in execution input yaml is empty
          if (isEmpty(executionInputYamlVariables.get(variableName))) {
            throw new InvalidRequestException(
                String.format("%s is a required variable .Value or expression not provided for the variable : %s",
                    variableName, variableName));
          }

          // Checking if value of required variable in execution input yaml is of form <+input>.executionInput().
          if (NGExpressionUtils.matchesExecutionInputPattern(executionInputYamlVariables.get(variableName))) {
            String defaultValue =
                NGRuntimeInputUtils.extractParameters(executionInputYamlVariables.get(variableName), "default");
            // This should happen only in case when we come to this flow via proceed with default values failure
            // strategy where execution input yam is empty and we fetch it from the execution input instance in the db.
            // In case execution input yaml is provided empty by the user we will throw an error. We will also throw an
            // error if the default value of a required variable is empty in case of proceed with default values.
            if (!continueWithDefault || isEmpty(defaultValue)) {
              throw new InvalidRequestException(String.format(
                  "%s is a required variable .Default value is empty or not provided for the variable : %s or the execution input yaml provided by user is empty",
                  variableName, variableName));
            }
          }
        }
      });
    }
  }

  // Using duplicate method of InputSetErrorHelper in pipeline-service. Will refactor the method and use the bring
  // down to 870.
  public Map<FQN, String> getInvalidFQNsInInputSet(JsonNode templateJsonNode, JsonNode inputSetPipelineCompJsonNode) {
    Map<FQN, String> errorMap = new LinkedHashMap<>();
    Map<FQN, Object> inputSetFqnToValueMap = FQNMapGenerator.generateFQNMap(inputSetPipelineCompJsonNode);
    Set<FQN> inputSetFQNs = new LinkedHashSet<>(inputSetFqnToValueMap.keySet());
    if (isEmpty(templateJsonNode)) {
      inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Pipeline no longer contains any runtime input"));
      return errorMap;
    }
    Map<FQN, Object> templateFqnToValueMap = FQNMapGenerator.generateFQNMap(templateJsonNode);
    templateFqnToValueMap.keySet().forEach(key -> {
      if (inputSetFQNs.contains(key)) {
        Object templateValue = templateFqnToValueMap.get(key);
        Object value = inputSetFqnToValueMap.get(key);
        if (key.isType() || key.isIdentifierOrVariableName()) {
          if (!value.toString().equals(templateValue.toString())) {
            errorMap.put(key,
                "The value for " + key.getExpressionFqn() + " is " + templateValue
                    + "in the pipeline yaml, but the input set has it as " + value);
          }
        } else {
          String error = validateStaticValues(templateValue, value, key.getExpressionFqn());
          if (EmptyPredicate.isNotEmpty(error)) {
            errorMap.put(key, error);
          }
        }

        inputSetFQNs.remove(key);
      } else {
        Map<FQN, Object> subMap = YamlSubMapExtractor.getFQNToObjectSubMap(inputSetFqnToValueMap, key);
        subMap.keySet().forEach(inputSetFQNs::remove);
      }
    });
    inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Field either not present in pipeline or not a runtime input"));
    return errorMap;
  }

  @Override
  public void updateTTLForNodesForGivenIds(Set<String> nodeExecutionIds, Date ttlDate) {
    if (isEmpty(nodeExecutionIds)) {
      return;
    }
    // using unique nodeExecution idx
    Criteria criteria = Criteria.where(ExecutionInputInstanceKeys.nodeExecutionId).in(nodeExecutionIds);
    Query query = new Query(criteria);
    Update ops = new Update();
    ops.set(ExecutionInputInstanceKeys.validUntil, ttlDate);
    Failsafe.with(DEFAULT_RETRY_POLICY).get(() -> {
      executionInputRepository.multiUpdate(query, ops);
      return true;
    });
  }
}
