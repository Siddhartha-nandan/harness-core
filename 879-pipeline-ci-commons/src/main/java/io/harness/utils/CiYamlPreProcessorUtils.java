package io.harness.utils;

import io.harness.common.NGExpressionUtils;
import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CiYamlPreProcessorUtils {
  public static final String INPUT_DEFAULT_CONSTANT = "default";
  public void preProcessYaml(YamlField yamlField) {
    JsonNode currentJsonNode = yamlField.getNode().getCurrJsonNode();
    replaceInputWithDefaultValue(currentJsonNode);
  }

  public String getDefaultValueIfPatternMatch(String input) {
    if (NGExpressionUtils.matchesInputSetPattern(input)) {
      String toBeFoundInputDefaultString = "." + INPUT_DEFAULT_CONSTANT + "(";
      int foundIndex = input.indexOf(toBeFoundInputDefaultString);
      if (foundIndex != -1) {
        int startIndex = foundIndex + toBeFoundInputDefaultString.length();
        int lastIndex = input.indexOf(')', startIndex);
        if (lastIndex != 1) {
          return input.substring(startIndex, lastIndex);
        }
      }
    }
    return input;
  }

  public void replaceInputWithDefaultValue(JsonNode jsonNode) {
    if (jsonNode != null) {
      if (jsonNode.isArray()) {
        replaceInputWithDefaultValueInArrayField(jsonNode);
      } else if (jsonNode.isObject()) {
        replaceInputWithDefaultValueInObject(jsonNode);
      }
    }
  }

  public void replaceInputWithDefaultValueInObject(JsonNode jsonNode) {
    ObjectNode objectNode = (ObjectNode) jsonNode;
    for (Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext();) {
      Map.Entry<String, JsonNode> field = it.next();
      if (field.getValue().isTextual()) {
        String originalValue = field.getValue().asText();
        String defaultValue = getDefaultValueIfPatternMatch(originalValue);
        objectNode.put(field.getKey(), defaultValue);
      } else {
        replaceInputWithDefaultValue(field.getValue());
      }
    }
  }

  public void replaceInputWithDefaultValueInArrayField(JsonNode jsonNode) {
    ArrayNode arrayNodes = (ArrayNode) jsonNode;
    for (int i = 0; i < arrayNodes.size(); i++) {
      JsonNode currentJsonNode = arrayNodes.get(i);
      if (currentJsonNode.isTextual()) {
        String defaultValue = getDefaultValueIfPatternMatch(currentJsonNode.asText());
        arrayNodes.set(i, new TextNode(defaultValue));
      } else {
        replaceInputWithDefaultValue(currentJsonNode);
      }
    }
  }
}
