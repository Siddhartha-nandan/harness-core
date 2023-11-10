/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.pipeline.resources;

import static io.harness.rule.OwnerRule.PRATYUSH;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStrategyType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.helpers.CDNGPipelineConfigurationHelper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.data.structure.ListUtils;
import io.harness.ng.core.Status;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CDNGPipelineConfigurationResourceTest extends CategoryTest {
  @Mock CDNGPipelineConfigurationHelper pipelineService;
  CDNGPipelineConfigurationResource cdngPipelineConfigurationResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cdngPipelineConfigurationResource = new CDNGPipelineConfigurationResource(pipelineService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionStrategyList() {
    doCallRealMethod().when(pipelineService).getExecutionStrategyList();
    Map<ServiceDefinitionType, List<ExecutionStrategyType>> executionStrategyResponse =
        cdngPipelineConfigurationResource.getExecutionStrategyList().getData();

    assertThat(executionStrategyResponse).isNotNull();
    assertThat(executionStrategyResponse.keySet().size()).isEqualTo(14);

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.KUBERNETES))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.ROLLING, ExecutionStrategyType.BLUE_GREEN,
            ExecutionStrategyType.CANARY, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.NATIVE_HELM))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.ROLLING, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.SSH))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.DEFAULT, ExecutionStrategyType.BASIC,
            ExecutionStrategyType.ROLLING, ExecutionStrategyType.CANARY));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.WINRM))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.DEFAULT, ExecutionStrategyType.BASIC,
            ExecutionStrategyType.ROLLING, ExecutionStrategyType.CANARY));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.SERVERLESS_AWS_LAMBDA))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.AZURE_WEBAPP))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN,
            ExecutionStrategyType.CANARY, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.CUSTOM_DEPLOYMENT))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.ECS))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.ROLLING,
            ExecutionStrategyType.CANARY, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.ASG))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.ROLLING, ExecutionStrategyType.CANARY,
            ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.TAS))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.CANARY,
            ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.DEFAULT, ExecutionStrategyType.ROLLING));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.GOOGLE_CLOUD_FUNCTIONS))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.CANARY,
            ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.AWS_LAMBDA))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.DEFAULT));

    assertThat(executionStrategyResponse.get(ServiceDefinitionType.AWS_SAM))
        .isEqualTo(ListUtils.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.DEFAULT));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetServiceDefinitionTypes() {
    when(pipelineService.getServiceDefinitionTypes(null)).thenReturn(Arrays.asList(ServiceDefinitionType.values()));
    List<ServiceDefinitionType> serviceDefinitionTypes =
        cdngPipelineConfigurationResource.getServiceDefinitionTypes(null).getData();

    assertThat(serviceDefinitionTypes).isNotNull();
    assertThat(serviceDefinitionTypes.size()).isEqualTo(14);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testGetFailureStrategiesYaml() throws IOException {
    String data = "failureStrategies:\n"
        + "  - onFailure:\n"
        + "      errors:\n"
        + "        - AllErrors\n"
        + "      action:\n"
        + "        type: StageRollback";
    assertThat(cdngPipelineConfigurationResource.getFailureStrategiesYaml().getStatus().equals(Status.SUCCESS));
    assertThat(cdngPipelineConfigurationResource.getFailureStrategiesYaml().getData().equals(data));
  }
}
