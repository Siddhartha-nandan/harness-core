/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.container.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.ComputingResource;
import io.harness.delegate.ExecutionInfrastructure;
import io.harness.delegate.K8sInfraSpec;
import io.harness.delegate.LogConfig;
import io.harness.delegate.Secret;
import io.harness.delegate.SecurityContext;
import io.harness.delegate.StepSpec;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.container.ContainerResource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_COMMON_STEPS})
@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class DelegateInitTaskUtils {
  private static final String DEFAULT_CONTAINER_RESOURCE_MEMORY = "100Mi";
  private static final String DEFAULT_CONTAINER_RESOURCE_CPU = "100m";
  private static final String UUID_KEY = "__uuid";

  public ExecutionInfrastructure buildExecutionInfrastructure(
      List<StepSpec> stepSpecList, String initStepLogPrefix, SecurityContext podSecurityContext) {
    return ExecutionInfrastructure.newBuilder()
        .setLogConfig(LogConfig.newBuilder().setLogPrefix(initStepLogPrefix).build())
        .setK8S(getK8sPodInfrastructure(stepSpecList, podSecurityContext))
        .build();
  }

  public static String getImage(ParameterField<String> image) {
    return getParameterFieldValue(image);
  }

  public static Map<String, String> getEnvVariables(ParameterField<Map<String, String>> envVariables) {
    Map<String, String> vars = getParameterFieldValue(envVariables);
    vars.remove(UUID_KEY);
    return vars;
  }

  public static ComputingResource getComputingResource(ContainerResource containerResource) {
    ContainerResource.Limits limits = containerResource.getLimits();
    if (limits != null) {
      String cpu = getParameterFieldValue(limits.getCpu());
      String memory = getParameterFieldValue(limits.getMemory());
      return ComputingResource.newBuilder().setCpu(cpu).setMemory(memory).build();
    }

    return ComputingResource.newBuilder()
        .setCpu(DEFAULT_CONTAINER_RESOURCE_CPU)
        .setMemory(DEFAULT_CONTAINER_RESOURCE_MEMORY)
        .build();
  }

  public static List<Secret> getSecrets(List<String> scopedSecrets) {
    if (isEmpty(scopedSecrets)) {
      return Collections.emptyList();
    }

    return scopedSecrets.stream()
        .map(scopedSecret -> Secret.newBuilder().setScopeSecretIdentifier(scopedSecret).build())
        .collect(Collectors.toList());
  }

  private static K8sInfraSpec getK8sPodInfrastructure(List<StepSpec> stepSpecList, SecurityContext podSecurityContext) {
    K8sInfraSpec.Builder k8sInfraSpecBuilder = K8sInfraSpec.newBuilder();

    if (podSecurityContext != null) {
      k8sInfraSpecBuilder.setSecurityContext(podSecurityContext);
    }

    return k8sInfraSpecBuilder.addAllSteps(stepSpecList).build();
  }
  // need to be moved to some utility class
  public <T> T getParameterFieldValue(ParameterField<T> fieldValue) {
    if (fieldValue == null) {
      return null;
    }
    return fieldValue.getValue();
  }
}
