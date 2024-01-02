/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.aws.resources;

import static io.harness.rule.OwnerRule.VITALIE;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.IdentifierRef;
import io.harness.beans.ScopeInfo;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.cdng.aws.service.AwsResourceServiceImpl;
import io.harness.cdng.infra.mapper.InfrastructureEntityConfigMapper;
import io.harness.cdng.infra.yaml.AsgInfrastructure;
import io.harness.cdng.infra.yaml.InfrastructureConfig;
import io.harness.cdng.infra.yaml.InfrastructureDefinitionConfig;
import io.harness.cdng.infra.yaml.SshWinRmAwsInfrastructure;
import io.harness.ng.core.artifacts.resources.util.ArtifactResourceUtils;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.utils.IdentifierRefHelper;

import software.wings.service.impl.aws.model.AwsVPC;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class AwsHelperResourceTest extends CategoryTest {
  private static final String CONNECTOR_REF = "connectorRef";
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";

  private static final String ENV_ID = "envId";
  private static final String INFRA_DEFINITION_ID = "infraDefinitionId";
  private static final String REGION = "us-east";

  @Mock AwsResourceServiceImpl awsHelperService;
  @Mock InfrastructureEntityService infrastructureEntityService;
  @Mock ArtifactResourceUtils artifactResourceUtils;

  IdentifierRef identifierRef = mock(IdentifierRef.class);

  @InjectMocks AwsHelperResource awsHelperResource;

  ScopeInfo scopeInfo = ScopeInfo.builder()
                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJECT_IDENTIFIER)
                            .scopeType(ScopeLevel.PROJECT)
                            .uniqueId(randomAlphabetic(10))
                            .build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getASGNames() {
    List<String> asgList = Arrays.asList("asg1", "asg2");
    try (MockedStatic<IdentifierRefHelper> ignore = mockStatic(IdentifierRefHelper.class)) {
      when(IdentifierRefHelper.getIdentifierRef(anyString(), anyString(), anyString(), anyString()))
          .thenAnswer(i -> identifierRef);
      when(awsHelperService.getASGNames(eq(scopeInfo), any(), anyString())).thenReturn(asgList);

      ResponseDTO<List<String>> result = awsHelperResource.getASGNames(
          CONNECTOR_REF, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, REGION, null, null, scopeInfo);
      assertThat(result.getData()).isEqualTo(asgList);
      verify(awsHelperService, times(1)).getASGNames(eq(scopeInfo), any(), anyString());
    }
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getASGNamesByInfra() {
    List<String> asgList = Arrays.asList("asg1", "asg2");
    try (MockedStatic<IdentifierRefHelper> ignore = mockStatic(IdentifierRefHelper.class);
         MockedStatic<InfrastructureEntityConfigMapper> ignore2 = mockStatic(InfrastructureEntityConfigMapper.class)) {
      when(IdentifierRefHelper.getIdentifierRef(anyString(), anyString(), anyString(), anyString()))
          .thenAnswer(i -> identifierRef);
      when(InfrastructureEntityConfigMapper.toInfrastructureConfig(any(InfrastructureEntity.class)))
          .thenAnswer(i
              -> InfrastructureConfig.builder()
                     .infrastructureDefinitionConfig(
                         InfrastructureDefinitionConfig.builder()
                             .spec(AsgInfrastructure.builder()
                                       .connectorRef(ParameterField.createValueField(CONNECTOR_REF))
                                       .region(ParameterField.createValueField(REGION))
                                       .build())
                             .build())
                     .build());
      when(awsHelperService.getASGNames(eq(scopeInfo), any(), anyString())).thenReturn(asgList);
      when(infrastructureEntityService.get(anyString(), anyString(), anyString(), anyString(), anyString()))
          .thenReturn(Optional.of(InfrastructureEntity.builder().build()));

      ResponseDTO<List<String>> result = awsHelperResource.getASGNames(
          null, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null, ENV_ID, INFRA_DEFINITION_ID, scopeInfo);
      assertThat(result.getData()).isEqualTo(asgList);
      verify(awsHelperService, times(1)).getASGNames(eq(scopeInfo), any(), anyString());
    }
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getVpcs() {
    List<AwsVPC> vpcs = Arrays.asList(AwsVPC.builder().id("vpc1").build(), AwsVPC.builder().id("vpc2").build());
    try (MockedStatic<IdentifierRefHelper> ignore = mockStatic(IdentifierRefHelper.class)) {
      when(IdentifierRefHelper.getIdentifierRef(anyString(), anyString(), anyString(), anyString()))
          .thenAnswer(i -> identifierRef);
      when(awsHelperService.getVPCs(any(), any(), anyString())).thenReturn(vpcs);

      ResponseDTO<List<AwsVPC>> result = awsHelperResource.getVpcs(
          CONNECTOR_REF, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, REGION, null, null, scopeInfo);
      assertThat(result.getData()).isEqualTo(vpcs);
    }
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void getVpcsByInfra() {
    List<AwsVPC> vpcs = Arrays.asList(AwsVPC.builder().id("vpc1").build(), AwsVPC.builder().id("vpc2").build());
    try (MockedStatic<IdentifierRefHelper> ignore = mockStatic(IdentifierRefHelper.class);
         MockedStatic<InfrastructureEntityConfigMapper> ignore2 = mockStatic(InfrastructureEntityConfigMapper.class)) {
      when(IdentifierRefHelper.getIdentifierRef(anyString(), anyString(), anyString(), anyString()))
          .thenAnswer(i -> identifierRef);
      when(InfrastructureEntityConfigMapper.toInfrastructureConfig(any(InfrastructureEntity.class)))
          .thenAnswer(i
              -> InfrastructureConfig.builder()
                     .infrastructureDefinitionConfig(
                         InfrastructureDefinitionConfig.builder()
                             .spec(SshWinRmAwsInfrastructure.builder()
                                       .connectorRef(ParameterField.createValueField(CONNECTOR_REF))
                                       .region(ParameterField.createValueField(REGION))
                                       .build())
                             .build())
                     .build());
      when(awsHelperService.getVPCs(any(), any(), anyString())).thenReturn(vpcs);
      when(infrastructureEntityService.get(anyString(), anyString(), anyString(), anyString(), anyString()))
          .thenReturn(Optional.of(InfrastructureEntity.builder().build()));

      ResponseDTO<List<AwsVPC>> result = awsHelperResource.getVpcs(
          null, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null, ENV_ID, INFRA_DEFINITION_ID, scopeInfo);
      assertThat(result.getData()).isEqualTo(vpcs);
    }
  }
}
