/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.Bamboo.service;

import static io.harness.rule.OwnerRule.RAKSHIT_AGARWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.resources.bamboo.BambooResourceServiceImpl;
import io.harness.cdng.artifact.resources.bamboo.dtos.BambooPlanKeysDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.bamboo.BambooAuthType;
import io.harness.delegate.beans.connector.bamboo.BambooAuthenticationDTO;
import io.harness.delegate.beans.connector.bamboo.BambooConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.*;
import junitparams.JUnitParamsRunner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
@RunWith(JUnitParamsRunner.class)
public class BambooResourceServiceImplTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String IDENTIFIER = "identifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PLAN_NAME = "planName";
  private static final List ARTIFACT_PATH = new ArrayList<>();
  private static final String INPUT = "<+input>-abc";
  private static final String PLAN_NAME_MESSAGE = "value for planName is empty or not provided";
  private static final IdentifierRef CONNECTOR_REF = IdentifierRef.builder()
                                                         .accountIdentifier(ACCOUNT_ID)
                                                         .identifier(IDENTIFIER)
                                                         .projectIdentifier(PROJECT_IDENTIFIER)
                                                         .orgIdentifier(ORG_IDENTIFIER)
                                                         .build();

  @Mock private ConnectorService connectorService;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @InjectMocks private BambooResourceServiceImpl bambooResourceService;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }
  private ConnectorResponseDTO getConnector() {
    BambooConnectorDTO bambooConnectorDTO =
        BambooConnectorDTO.builder()
            .delegateSelectors(Collections.emptySet())
            .bambooUrl("https://bamboo.com")
            .auth(BambooAuthenticationDTO.builder().authType(BambooAuthType.USER_PASSWORD).build())
            .build();

    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(ConnectorType.BAMBOO)
                                            .connectorConfig(bambooConnectorDTO)
                                            .projectIdentifier("dummyProject")
                                            .orgIdentifier("dummyOrg")
                                            .build();
    return ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestGetPlanName() {
    IdentifierRef connectorRef = IdentifierRef.builder()
                                     .accountIdentifier(ACCOUNT_ID)
                                     .identifier(IDENTIFIER)
                                     .projectIdentifier(PROJECT_IDENTIFIER)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .build();

    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(ArtifactTaskResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder()
                                                           .plans(Collections.EMPTY_MAP)
                                                           .artifactDelegateResponses(new ArrayList<>())
                                                           .build())
                        .build());
    BambooPlanKeysDTO bambooPlanKeysDTO =
        bambooResourceService.getPlanName(connectorRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(bambooPlanKeysDTO).isNotNull();
    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    verify(delegateGrpcClientWrapper).executeSyncTaskV2(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.GET_PLANS);
  }

  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestGetArtifactPath() {
    IdentifierRef connectorRef = IdentifierRef.builder()
                                     .accountIdentifier(ACCOUNT_ID)
                                     .identifier(IDENTIFIER)
                                     .projectIdentifier(PROJECT_IDENTIFIER)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .build();

    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(ArtifactTaskResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder()
                                                           .artifactPath(Collections.emptyList())
                                                           .artifactDelegateResponses(new ArrayList<>())
                                                           .build())
                        .build());
    List<String> artifactPath =
        bambooResourceService.getArtifactPath(connectorRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, PLAN_NAME);
    assertThat(artifactPath).isNotNull();
    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    verify(delegateGrpcClientWrapper).executeSyncTaskV2(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.GET_ARTIFACT_PATH);
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void TestGetBuilds() {
    IdentifierRef connectorRef = IdentifierRef.builder()
                                     .accountIdentifier(ACCOUNT_ID)
                                     .identifier(IDENTIFIER)
                                     .projectIdentifier(PROJECT_IDENTIFIER)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .build();

    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTaskV2(any()))
        .thenReturn(ArtifactTaskResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder()
                                                           .buildDetails(Collections.emptyList())
                                                           .artifactDelegateResponses(new ArrayList<>())
                                                           .build())
                        .build());
    List<BuildDetails> buildDetails =
        bambooResourceService.getBuilds(connectorRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, PLAN_NAME, ARTIFACT_PATH);
    assertThat(buildDetails).isNotNull();
    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    verify(delegateGrpcClientWrapper).executeSyncTaskV2(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.GET_BUILDS);
  }

  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void testGetBuilds_PlanName_Null() {
    assertThatThrownBy(
        () -> bambooResourceService.getBuilds(CONNECTOR_REF, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null, ARTIFACT_PATH))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PLAN_NAME_MESSAGE);
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void testGetBuilds_PlanName_Empty() {
    assertThatThrownBy(
        () -> bambooResourceService.getBuilds(CONNECTOR_REF, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "", ARTIFACT_PATH))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PLAN_NAME_MESSAGE);
  }
  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void testGetBuilds_PlanName_Input() {
    assertThatThrownBy(
        () -> bambooResourceService.getBuilds(CONNECTOR_REF, ORG_IDENTIFIER, PROJECT_IDENTIFIER, INPUT, ARTIFACT_PATH))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PLAN_NAME_MESSAGE);
  }
}
