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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class RouteSpecGsonDeserializer implements JsonDeserializer<RouteSpec> {
  @Override
  public RouteSpec deserialize(JsonElement jsonElement, Type type,
      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    JsonObject jsonObject = jsonElement.getAsJsonObject();
    JsonElement matchElement = jsonObject.get("matches");

    RouteSpecBuilder routeSpecBuilder = RouteSpec.builder();

    List<Match> matches = new ArrayList<>();
    if (matchElement != null && matchElement.isJsonArray()) {
      for (JsonElement match : matchElement.getAsJsonArray()) {
        matches.add(parseMatch(jsonDeserializationContext, match));
      }
    }
    routeSpecBuilder.matches(matches);

    return routeSpecBuilder.build();
  }

  private Match parseMatch(JsonDeserializationContext jsonDeserializationContext, JsonElement match)
      throws JsonParseException {
    if (match.getAsJsonObject().get("headers") != null) {
      return jsonDeserializationContext.deserialize(match, HeaderMatch.class);
    }

    if (match.getAsJsonObject().get("methods") != null) {
      return jsonDeserializationContext.deserialize(match, MethodMatch.class);
    }

    if (match.getAsJsonObject().get("ports") != null) {
      return jsonDeserializationContext.deserialize(match, PortMatch.class);
    }

    if (match.getAsJsonObject().get("pathRegex") != null) {
      return jsonDeserializationContext.deserialize(match, URIMatch.class);
    }

    throw new JsonParseException("Failed to parse RouteSpec object from Traffic Routing config");
  }
}
