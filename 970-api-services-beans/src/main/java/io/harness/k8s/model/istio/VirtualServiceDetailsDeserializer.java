/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.istio;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.k8s.model.istio.VirtualServiceDetails.VirtualServiceDetailsBuilder;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class VirtualServiceDetailsDeserializer extends StdDeserializer<VirtualServiceDetails> {
  public VirtualServiceDetailsDeserializer() {
    super(Match.class);
  }

  protected VirtualServiceDetailsDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public VirtualServiceDetails deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JacksonException {
    JsonNode parentJsonNode = jsonParser.getCodec().readTree(jsonParser);
    JsonNode matchNode = parentJsonNode.get("match");

    VirtualServiceDetailsBuilder virtualServiceDetailsBuilder = VirtualServiceDetails.builder();

    List<Match> matches = new ArrayList<>();
    if (matchNode != null && matchNode.isArray()) {
      for (JsonNode match : matchNode) {
        matches.add(parseMatch(jsonParser, match));
      }
    }
    virtualServiceDetailsBuilder.match(matches);

    List<HttpRouteDestination> routes = new ArrayList<>();
    JsonNode routeNode = parentJsonNode.get("route");
    if (routeNode != null && routeNode.isArray()) {
      for (JsonNode route : routeNode) {
        routes.add(jsonParser.getCodec().treeToValue(route, HttpRouteDestination.class));
      }
    }
    virtualServiceDetailsBuilder.route(routes);

    return virtualServiceDetailsBuilder.build();
  }

  private Match parseMatch(JsonParser jsonParser, JsonNode match) throws IOException {
    if (match.get("authority") != null) {
      return jsonParser.getCodec().treeToValue(match, AuthorityMatch.class);
    }

    if (match.get("header") != null) {
      return jsonParser.getCodec().treeToValue(match, HeaderMatch.class);
    }

    if (match.get("sniHosts") != null) {
      return jsonParser.getCodec().treeToValue(match, HostMatch.class);
    }

    if (match.get("method") != null) {
      return jsonParser.getCodec().treeToValue(match, MethodMatch.class);
    }

    if (match.get("port") != null) {
      return jsonParser.getCodec().treeToValue(match, PortMatch.class);
    }

    if (match.get("scheme") != null) {
      return jsonParser.getCodec().treeToValue(match, SchemeMatch.class);
    }

    if (match.get("uri") != null) {
      return jsonParser.getCodec().treeToValue(match, URIMatch.class);
    }

    throw new IOException("Failed to parse Match object from Traffic Routing config");
  }
}
