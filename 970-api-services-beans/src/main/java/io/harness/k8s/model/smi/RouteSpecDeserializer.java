/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.smi;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.k8s.model.smi.RouteSpec.RouteSpecBuilder;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class RouteSpecDeserializer extends StdDeserializer<RouteSpec> {
  public RouteSpecDeserializer() {
    super(RouteSpec.class);
  }

  protected RouteSpecDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public RouteSpec deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JacksonException {
    JsonNode parentJsonNode = jsonParser.getCodec().readTree(jsonParser);
    JsonNode matchNode = parentJsonNode.get("matches");

    RouteSpecBuilder routeSpecBuilder = RouteSpec.builder();

    List<Match> matches = new ArrayList<>();
    if (matchNode != null && matchNode.isArray()) {
      for (JsonNode match : matchNode) {
        matches.add(parseMatch(jsonParser, match));
      }
    }
    routeSpecBuilder.matches(matches);

    return routeSpecBuilder.build();
  }

  private Match parseMatch(JsonParser jsonParser, JsonNode match) throws IOException {
    if (match.get("headers") != null) {
      return jsonParser.getCodec().treeToValue(match, HeaderMatch.class);
    }

    if (match.get("methods") != null) {
      return jsonParser.getCodec().treeToValue(match, MethodMatch.class);
    }

    if (match.get("ports") != null) {
      return jsonParser.getCodec().treeToValue(match, PortMatch.class);
    }

    if (match.get("pathRegex") != null) {
      return jsonParser.getCodec().treeToValue(match, URIMatch.class);
    }

    throw new IOException("Failed to parse RouteSpec object from Traffic Routing config");
  }
}
