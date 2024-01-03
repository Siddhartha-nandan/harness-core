/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.entities.artifact;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;

@AllArgsConstructor
public enum ArtifactType {
  CONTAINER("image", "image"),
  REPOSITORY("repo", "repo");

  @Getter String name;
  String value;

  private static Map<String, ArtifactType> NAME_TO_ARTIFACT_TYPE_MAP;

  public static ArtifactType getArtifactType(String name) {
    if (MapUtils.isEmpty(NAME_TO_ARTIFACT_TYPE_MAP)) {
      NAME_TO_ARTIFACT_TYPE_MAP =
          Arrays.stream(ArtifactType.values()).collect(Collectors.toMap(ArtifactType::getName, Function.identity()));
    }
    if (!NAME_TO_ARTIFACT_TYPE_MAP.containsKey(name)) {
      throw new IllegalStateException("Artifact type:" + name + " not mapped");
    }
    return NAME_TO_ARTIFACT_TYPE_MAP.get(name);
  }
}
