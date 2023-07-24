/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.jenkins.mappers;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.artifact.resources.jenkins.dtos.JenkinsJobDetailsDTO;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;

import lombok.experimental.UtilityClass;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ARTIFACTS})
@UtilityClass
public class JenkinsResourceMapper {
  public JenkinsJobDetailsDTO toJenkinsJobDetailsDTO(ArtifactTaskExecutionResponse artifactTaskExecutionResponse) {
    return JenkinsJobDetailsDTO.builder().jobDetails(artifactTaskExecutionResponse.getJobDetails()).build();
  }
}
