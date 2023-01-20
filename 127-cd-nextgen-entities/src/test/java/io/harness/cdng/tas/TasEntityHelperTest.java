/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.rule.OwnerRule.SOURABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTaskRequest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.TanzuApplicationServiceInfrastructureOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.tasconnector.TasConnectorDTO;
import io.harness.delegate.beans.connector.tasconnector.TasCredentialDTO;
import io.harness.delegate.beans.connector.tasconnector.TasManualDetailsDTO;
import io.harness.delegate.task.pcf.request.CfInfraMappingDataRequestNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TasEntityHelperTest extends CategoryTest {
  private static final String PROJECT_IDENTIFIER = "project";
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final String ORG_IDENTIFIER = "org";
  @Mock private ConnectorService connectorService;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock ExceptionManager exceptionManager;
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @Captor private ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestArgumentCaptor;

  @InjectMocks private TasEntityHelper tasEntityHelper;
  @Before
  public void prepare() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetEncryptionDetails() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().connectorType(ConnectorType.TAS).build();
    TasConnectorDTO tasConnectorDTO =
        TasConnectorDTO.builder()
            .credential(TasCredentialDTO.builder().spec(TasManualDetailsDTO.builder().build()).build())
            .build();
    connectorInfoDTO.setConnectorConfig(tasConnectorDTO);
    NGAccess ngAccess = BaseNGAccess.builder()
                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJECT_IDENTIFIER)
                            .build();
    List<EncryptedDataDetail> encryptedDataDetails = List.of(EncryptedDataDetail.builder().build());
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(encryptedDataDetails);
    assertThatCode(() -> tasEntityHelper.getEncryptionDataDetails(connectorInfoDTO, ngAccess))
        .doesNotThrowAnyException();
  }
  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetEncryptionDetailsWithUnsupportedConnector() {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.KUBERNETES_CLUSTER).build();
    TasConnectorDTO tasConnectorDTO =
        TasConnectorDTO.builder()
            .credential(TasCredentialDTO.builder().spec(TasManualDetailsDTO.builder().build()).build())
            .build();
    connectorInfoDTO.setConnectorConfig(tasConnectorDTO);
    NGAccess ngAccess = BaseNGAccess.builder()
                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJECT_IDENTIFIER)
                            .build();
    List<EncryptedDataDetail> encryptedDataDetails = List.of(EncryptedDataDetail.builder().build());
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(encryptedDataDetails);
    assertThatCode(() -> tasEntityHelper.getEncryptionDataDetails(connectorInfoDTO, ngAccess))
        .hasMessageContaining("Unsupported connector type");
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetTasInfraConfig() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().connectorType(ConnectorType.TAS).build();
    TasConnectorDTO tasConnectorDTO =
        TasConnectorDTO.builder()
            .credential(TasCredentialDTO.builder().spec(TasManualDetailsDTO.builder().build()).build())
            .build();
    connectorInfoDTO.setConnectorConfig(tasConnectorDTO);
    ConnectorResponseDTO connectorResponseDTO = ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorResponseDTO));
    List<EncryptedDataDetail> encryptedDataDetails = List.of(EncryptedDataDetail.builder().build());
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(encryptedDataDetails);
    NGAccess ngAccess = BaseNGAccess.builder()
                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJECT_IDENTIFIER)
                            .build();
    TanzuApplicationServiceInfrastructureOutcome infrastructureOutcome =
        TanzuApplicationServiceInfrastructureOutcome.builder()
            .connectorRef("connector")
            .space("space")
            .organization("org")
            .build();
    TasInfraConfig tasInfraConfig = tasEntityHelper.getTasInfraConfig(infrastructureOutcome, ngAccess);
    assertThat(tasInfraConfig.getOrganization()).isEqualTo("org");
    assertThat(tasInfraConfig.getSpace()).isEqualTo("space");
    assertThat(tasInfraConfig.getTasConnectorDTO()).isEqualTo(tasConnectorDTO);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testExecSyncTask() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().connectorType(ConnectorType.TAS).build();
    TasConnectorDTO tasConnectorDTO =
        TasConnectorDTO.builder()
            .credential(TasCredentialDTO.builder().spec(TasManualDetailsDTO.builder().build()).build())
            .delegateSelectors(Collections.emptySet())
            .build();
    connectorInfoDTO.setConnectorConfig(tasConnectorDTO);
    ConnectorResponseDTO connectorResponseDTO = ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(connectorResponseDTO));
    List<EncryptedDataDetail> encryptedDataDetails = List.of(EncryptedDataDetail.builder().build());
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(encryptedDataDetails);
    BaseNGAccess ngAccess = BaseNGAccess.builder()
                                .accountIdentifier(ACCOUNT_IDENTIFIER)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJECT_IDENTIFIER)
                                .build();
    CfInfraMappingDataRequestNG cfInfraMappingDataRequestNG =
        CfInfraMappingDataRequestNG.builder()
            .tasInfraConfig(TasInfraConfig.builder().tasConnectorDTO(tasConnectorDTO).build())
            .build();
    TaskType taskType = TaskType.TAS_DATA_FETCH;
    tasEntityHelper.executeSyncTask(cfInfraMappingDataRequestNG, ngAccess, taskType);
    verify(delegateGrpcClientWrapper).executeSyncTaskV2(delegateTaskRequestArgumentCaptor.capture());
    assertThat(delegateTaskRequestArgumentCaptor.getValue().getTaskType())
        .isEqualTo(TaskType.TAS_DATA_FETCH.toString());
    assertThat(delegateTaskRequestArgumentCaptor.getValue().getTaskParameters()).isEqualTo(cfInfraMappingDataRequestNG);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetSetupOutcome() {
    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(true).build();
    when(executionSweepingOutputService.resolveOptional(any(), any())).thenReturn(optionalSweepingOutput);
    OptionalSweepingOutput expectedOutput = tasEntityHelper.getSetupOutcome(
        null, "tasBGSetupFqn", "tasBasicSetupFqn", "tasCanarySetupFqn", "output", executionSweepingOutputService);
    assertThat(expectedOutput.isFound()).isTrue();
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetSetupOutcomeWithBGFqnNull() {
    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(true).build();
    when(executionSweepingOutputService.resolveOptional(any(), any())).thenReturn(optionalSweepingOutput);
    OptionalSweepingOutput expectedOutput = tasEntityHelper.getSetupOutcome(
        null, null, "tasBasicSetupFqn", "tasCanarySetupFqn", "output", executionSweepingOutputService);
    assertThat(expectedOutput.isFound()).isTrue();
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetSetupOutcomeWithBasicFqnNull() {
    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(true).build();
    when(executionSweepingOutputService.resolveOptional(any(), any())).thenReturn(optionalSweepingOutput);
    OptionalSweepingOutput expectedOutput = tasEntityHelper.getSetupOutcome(
        null, null, null, "tasCanarySetupFqn", "output", executionSweepingOutputService);
    assertThat(expectedOutput.isFound()).isTrue();
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testGetSetupOutcomeWithAllFqnNull() {
    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(true).build();
    when(executionSweepingOutputService.resolveOptional(any(), any())).thenReturn(optionalSweepingOutput);
    OptionalSweepingOutput expectedOutput =
        tasEntityHelper.getSetupOutcome(null, null, null, null, "output", executionSweepingOutputService);
    assertThat(expectedOutput.isFound()).isFalse();
  }
}
