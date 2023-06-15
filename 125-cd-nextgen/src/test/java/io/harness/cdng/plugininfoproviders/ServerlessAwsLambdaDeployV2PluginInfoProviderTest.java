/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.plugininfoproviders;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.ServerlessAwsLambdaInfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.cdng.serverless.ServerlessEntityHelper;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaDeployV2StepInfo;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaPrepareRollbackContainerStepInfo;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaInfraConfig;
import io.harness.encryption.SecretRefData;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ImageDetails;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.plan.PluginDetails;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class ServerlessAwsLambdaDeployV2PluginInfoProviderTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private OutcomeService outcomeService;
  @Mock private KryoSerializer kryoSerializer;
  @Mock private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Mock private InstanceInfoService instanceInfoService;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private CDStepHelper cdStepHelper;
  @Mock private ServerlessEntityHelper serverlessEntityHelper;
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @Named(DEFAULT_CONNECTOR_SERVICE) @Mock private ConnectorService connectorService;
  @InjectMocks @Spy private ServerlessAwsLambdaDeployV2PluginInfoProvider serverlessAwsLambdaDeployV2PluginInfoProvider;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfo() throws IOException {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    String jsonNode = "jsonNdod";
    PluginCreationRequest pluginCreationRequest = PluginCreationRequest.newBuilder().setStepJsonNode(jsonNode).build();
    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    doReturn("identifier").when(cdAbstractStepNode).getIdentifier();
    doReturn("name").when(cdAbstractStepNode).getName();
    doReturn("uuid").when(cdAbstractStepNode).getUuid();

    ServerlessAwsLambdaDeployV2StepInfo serverlessAwsLambdaDeployV2StepInfo =
        ServerlessAwsLambdaDeployV2StepInfo.infoBuilder()
            .resources(ContainerResource.builder().build())
            .runAsUser(ParameterField.<Integer>builder().value(1).build())
            .connectorRef(ParameterField.<String>builder().value("connectorRef").build())
            .build();
    doReturn(serverlessAwsLambdaDeployV2StepInfo).when(cdAbstractStepNode).getStepSpecType();
    doReturn(Collections.emptyMap())
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .getEnvironmentVariables(any(), any());
    PluginCreationResponseWrapper pluginCreationResponseWrapper = mock(PluginCreationResponseWrapper.class);
    doReturn(pluginCreationResponseWrapper)
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .getPluginCreationResponseWrapper(any(), any());

    doReturn(mock(ImageDetails.class)).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getImageDetails(any());

    PluginDetails.Builder pluginBuilder = PluginDetails.newBuilder();
    doReturn(pluginBuilder)
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .getPluginDetailsBuilder(any(), any(), any());
    doReturn(cdAbstractStepNode).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getRead(jsonNode);

    assertThat(serverlessAwsLambdaDeployV2PluginInfoProvider.getPluginInfo(
                   pluginCreationRequest, Collections.emptySet(), ambiance))
        .isEqualTo(pluginCreationResponseWrapper);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetEnvironmentVariables() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    List<String> paths = Arrays.asList("path1");
    GithubStore storeConfig = GithubStore.builder()
                                  .connectorRef(ParameterField.<String>builder().value("connector").build())
                                  .paths(ParameterField.<List<String>>builder().value(paths).build())
                                  .build();
    ServerlessAwsLambdaDeployV2StepInfo serverlessAwsLambdaDeployV2StepInfo =
        ServerlessAwsLambdaDeployV2StepInfo.infoBuilder()
            .deployCommandOptions(ParameterField.createValueField(Collections.emptyList()))
            .build();
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome();
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder().store(storeConfig).build();
    doReturn(manifestsOutcome)
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .resolveServerlessManifestsOutcome(any());
    doReturn(serverlessAwsLambdaManifestOutcome)
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .getServerlessManifestOutcome(any());

    String configOverridePath = "config";
    doReturn(configOverridePath).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getConfigOverridePath(any());

    doReturn(paths).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getFolderPathsForManifest(any());

    ServerlessAwsLambdaInfrastructureOutcome infrastructureOutcome =
        ServerlessAwsLambdaInfrastructureOutcome.builder().connectorRef("connector").build();

    doReturn(infrastructureOutcome).when(outcomeService).resolve(any(), any());

    ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig = mock(ServerlessAwsLambdaInfraConfig.class);
    doReturn(serverlessAwsLambdaInfraConfig)
        .when(serverlessAwsLambdaDeployV2PluginInfoProvider)
        .getServerlessInfraConfig(any(), any());

    String stage = "stage";
    String region = "region";

    doReturn(stage).when(serverlessAwsLambdaInfraConfig).getStage();
    doReturn(region).when(serverlessAwsLambdaInfraConfig).getRegion();

    NGAccess ngAccess = mock(NGAccess.class);
    doReturn(ngAccess).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getNgAccess(any());

    IdentifierRef identifierRef = mock(IdentifierRef.class);
    doReturn(identifierRef).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getIdentifierRef(any(), any());

    ConnectorResponseDTO connectorResponseDTO = mock(ConnectorResponseDTO.class);
    Optional<ConnectorResponseDTO> optionalConnectorResponseDTO = Optional.of(connectorResponseDTO);

    doReturn(optionalConnectorResponseDTO).when(connectorService).get(any(), any(), any(), any());

    ConnectorInfoDTO connectorInfoDTO = mock(ConnectorInfoDTO.class);
    doReturn(connectorInfoDTO).when(connectorResponseDTO).getConnector();

    SecretRefData awsAccess = mock(SecretRefData.class);
    SecretRefData awsSecret = mock(SecretRefData.class);
    AwsManualConfigSpecDTO awsCredentialSpecDTO =
        AwsManualConfigSpecDTO.builder().accessKey("").accessKeyRef(awsAccess).secretKeyRef(awsSecret).build();
    AwsCredentialDTO awsCredentialDTO = AwsCredentialDTO.builder().config(awsCredentialSpecDTO).build();
    AwsConnectorDTO connectorConfigDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();
    doReturn(connectorConfigDTO).when(connectorInfoDTO).getConnectorConfig();

    String access = "access";
    String secret = "sercret";

    doReturn(access).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getKey(ambiance, awsAccess);
    doReturn(secret).when(serverlessAwsLambdaDeployV2PluginInfoProvider).getKey(ambiance, awsSecret);

    Map<String, String> response = serverlessAwsLambdaDeployV2PluginInfoProvider.getEnvironmentVariables(
        ambiance, serverlessAwsLambdaDeployV2StepInfo);
    assertThat(response.containsKey("PLUGIN_SERVERLESS_DIR")).isTrue();
    assertThat(response.get("PLUGIN_SERVERLESS_DIR")).isEqualTo(paths.get(0));

    assertThat(response.containsKey("PLUGIN_SERVERLESS_YAML_CUSTOM_PATH")).isTrue();
    assertThat(response.get("PLUGIN_SERVERLESS_YAML_CUSTOM_PATH")).isEqualTo(configOverridePath);

    assertThat(response.containsKey("PLUGIN_SERVERLESS_STAGE")).isTrue();
    assertThat(response.get("PLUGIN_SERVERLESS_STAGE")).isEqualTo(stage);

    assertThat(response.containsKey("PLUGIN_AWS_ACCESS_KEY")).isTrue();
    assertThat(response.get("PLUGIN_AWS_ACCESS_KEY")).isEqualTo(access);

    assertThat(response.containsKey("PLUGIN_AWS_SECRET_KEY")).isTrue();
    assertThat(response.get("PLUGIN_AWS_SECRET_KEY")).isEqualTo(secret);

    assertThat(response.containsKey("PLUGIN_REGION")).isTrue();
    assertThat(response.get("PLUGIN_REGION")).isEqualTo(region);
  }
}
