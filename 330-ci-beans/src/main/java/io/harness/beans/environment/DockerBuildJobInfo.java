/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.environment;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.yaml.core.variables.NGVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

/**
 * Stores docker runner specific data
 */

@Data
@Value
@Builder
@TypeAlias("dockerBuildJobInfo")
public class DockerBuildJobInfo implements BuildJobEnvInfo {
  @NotEmpty String workDir;
  CIExecutionArgs ciExecutionArgs;
  ArrayList<String> connectorRefs;
  List<NGVariable> stageVars;
  Map<String, String> volToMountPath;
  List<DependencyElement> serviceDependencies;

  @Override
  public Type getType() {
    return Type.DOCKER;
  }
}
