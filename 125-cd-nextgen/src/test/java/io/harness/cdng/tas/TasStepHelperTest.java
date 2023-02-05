/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.cdng.manifest.ManifestConfigType.TAS_AUTOSCALER;
import static io.harness.cdng.manifest.ManifestConfigType.TAS_MANIFEST;
import static io.harness.cdng.manifest.ManifestConfigType.TAS_VARS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.AMAZON_S3_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ARTIFACTORY_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.AZURE_ARTIFACTS_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.DOCKER_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.GITHUB_PACKAGES_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.JENKINS_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.NEXUS2_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.NEXUS3_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceType.AMAZONS3;
import static io.harness.delegate.task.artifacts.ArtifactSourceType.AZURE_ARTIFACTS;
import static io.harness.delegate.task.artifacts.ArtifactSourceType.JENKINS;
import static io.harness.rule.OwnerRule.RISHABH;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.artifact.outcome.AcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactoryArtifactOutcome;
import io.harness.cdng.artifact.outcome.AzureArtifactsOutcome;
import io.harness.cdng.artifact.outcome.CustomArtifactOutcome;
import io.harness.cdng.artifact.outcome.DockerArtifactOutcome;
import io.harness.cdng.artifact.outcome.EcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.GithubPackagesArtifactOutcome;
import io.harness.cdng.artifact.outcome.JenkinsArtifactOutcome;
import io.harness.cdng.artifact.outcome.NexusArtifactOutcome;
import io.harness.cdng.artifact.outcome.S3ArtifactOutcome;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.infra.beans.TanzuApplicationServiceInfrastructureOutcome;
import io.harness.cdng.k8s.K8sEntityHelper;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.AutoScalerManifestOutcome;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.CustomRemoteStoreConfig;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.OciHelmChartConfig;
import io.harness.cdng.manifest.yaml.TasManifestOutcome;
import io.harness.cdng.manifest.yaml.VarsManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.azureartifacts.AzureArtifactsAuthenticationDTO;
import io.harness.delegate.beans.connector.azureartifacts.AzureArtifactsAuthenticationType;
import io.harness.delegate.beans.connector.azureartifacts.AzureArtifactsConnectorDTO;
import io.harness.delegate.beans.connector.azureartifacts.AzureArtifactsCredentialsDTO;
import io.harness.delegate.beans.connector.azureartifacts.AzureArtifactsTokenDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsAuthType;
import io.harness.delegate.beans.connector.jenkins.JenkinsAuthenticationDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsUserNamePasswordDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.pcf.artifact.TasArtifactRegistryType;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.manifests.request.CustomManifestFetchConfig;
import io.harness.delegate.task.manifests.request.CustomManifestValuesFetchParams;
import io.harness.delegate.task.pcf.artifact.AwsS3TasArtifactRequestDetails;
import io.harness.delegate.task.pcf.artifact.AzureDevOpsTasArtifactRequestDetails;
import io.harness.delegate.task.pcf.artifact.CustomArtifactTasRequestDetails;
import io.harness.delegate.task.pcf.artifact.JenkinsTasArtifactRequestDetails;
import io.harness.delegate.task.pcf.artifact.NexusTasArtifactRequestDetails;
import io.harness.delegate.task.pcf.artifact.TasArtifactConfig;
import io.harness.delegate.task.pcf.artifact.TasArtifactType;
import io.harness.delegate.task.pcf.artifact.TasContainerArtifactConfig;
import io.harness.delegate.task.pcf.artifact.TasPackageArtifactConfig;
import io.harness.delegate.task.pcf.request.TasManifestsPackage;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.encryption.SecretRefData;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.dto.node.FolderNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.manifest.CustomManifestSource;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.filestore.FileUsage;
import io.harness.pcf.CfCommandUnitConstants;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.CfCliVersionNG;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.StepHelper;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(HarnessTeam.CDP)
public class TasStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private StepHelper stepHelper;
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private TasStepExecutor tasStepExecutor;
  @Mock private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Mock private PipelineRbacHelper pipelineRbacHelper;
  @Mock private KryoSerializer kryoSerializer;
  @Mock private ConnectorService connectorService;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private TasRollingDeployStep tasRollingDeployStep;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private OutcomeService outcomeService;
  @Mock private LogCallback mockLogCallback;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Mock private FileStoreService fileStoreService;
  @Spy @InjectMocks private K8sEntityHelper k8sEntityHelper;
  @Mock private TasInfraConfig tasInfraConfig;
  @Spy @InjectMocks private CDStepHelper cdStepHelper;
  @Spy @InjectMocks private TasStepHelper tasStepHelper;
  @Spy @InjectMocks private TasBasicAppSetupStep tasBasicAppSetupStep = new TasBasicAppSetupStep();
  private final Ambiance ambiance = Ambiance.newBuilder()
                                        .putSetupAbstractions(SetupAbstractionKeys.accountId, "test-account")
                                        .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, "test-org")
                                        .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, "test-project")
                                        .build();
  private final TanzuApplicationServiceInfrastructureOutcome infrastructureOutcome =
      TanzuApplicationServiceInfrastructureOutcome.builder()
          .connectorRef("tas")
          .organization("org")
          .space("space")
          .build();
  private final String ACR_SUBSCRIPTION_ID = "123456-5432-5432-543213";
  private final String NEXUS_URL = "https://nexus3.dev/repo/abc/def";
  private final String NEXUS_MAVEN_FORMAT = "maven";
  private final String NEXUS_DOCKER_FORMAT = "docker";
  private final String ARTIFACT_ID = "primary";
  private final String CONNECTOR_REF = "connectorRef";
  private final String ACR_REGISTRY_NAME = "testreg";
  private final String ACR_REGISTRY = format("%s.azurecr.io", ACR_REGISTRY_NAME.toLowerCase());
  private final String ACR_REPOSITORY = "test/app";
  private final String ACR_TAG = "2.51";
  private final String ACR_CONNECTOR_REF = "aztestref";
  public static String MANIFEST_YML = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n";
  public static String MANIFEST_YML_WITH_WEB_PROCESS = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  processes:\n"
      + "    - type: web\n"
      + "      instances: ((WEB_INSTANCES))\n"
      + "      memory: 100M\n"
      + "    - type: worker1\n"
      + "      health-check-type: port\n"
      + "      health-check-invocation-timeout: 10\n"
      + "      instances: 1\n"
      + "      memory: 100M";
  public static String MANIFEST_YML_WITHOUT_WEB_PROCESS = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n"
      + "  processes:\n"
      + "    - type: worker1\n"
      + "      health-check-type: port\n"
      + "      health-check-invocation-timeout: 10\n"
      + "      instances: 1\n"
      + "      memory: 100M";
  public static String COMMAND_SCRIPT = "## Performing cf login\n"
      + "\n"
      + "\n"
      + "cf login\n"
      + "\n"
      + "## Get apps\n"
      + "cf apps";
  public static String COMMAND_SCRIPT_WITHOUT_COMMENTS = "cf login\n"
      + "cf apps";
  public static String MANIFEST_YML_WITH_ROUTES = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n"
      + "  routes:\n"
      + "    - route: route1\n"
      + "    - route: route2";
  public static String NO_ROUTE_TRUE_MANIFEST_YML = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n"
      + "  no-route: true\n";
  public static String NO_ROUTE_FALSE_MANIFEST_YML = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n"
      + "  no-route: false\n";
  public static String MANIFEST_YML_WITH_VARS_NAME = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n";
  private final String INVALID_MANIFEST_YML = "applications:\n"
      + "  - name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: 2\n"
      + "  random-route: true\n";
  private final String NO_YML = "";
  public static String VARS_YML_1 = "MY: order\n"
      + "PCF_APP_NAME: test-tas\n"
      + "INSTANCES: 3\n"
      + "WEB_INSTANCES: 1\n"
      + "ROUTE: route1";
  public static String VARS_YML_2 = "env: prod\n"
      + "test: yes";
  public static String AUTOSCALAR_YML = "---\n"
      + "instance_limits:\n"
      + "  min: 1\n"
      + "  max: 2\n"
      + "rules:\n"
      + "- rule_type: \"http_latency\"\n"
      + "  rule_sub_type: \"avg_99th\"\n"
      + "  threshold:\n"
      + "    min: 100\n"
      + "    max: 200\n"
      + "scheduled_limit_changes:\n"
      + "- recurrence: 10\n"
      + "  executes_at: \"2032-01-01T00:00:00Z\"\n"
      + "  instance_limits:\n"
      + "    min: 1\n"
      + "    max: 2";
  private final String NOT_VAR = "APP_NAME: ${app.name}__${service.name}__${env.name}\n"
      + "APP_MEMORY: 750M\n"
      + "INSTANCES";
  private final RefObject manifests =
      RefObject.newBuilder()
          .setName(OutcomeExpressionConstants.MANIFESTS)
          .setKey(OutcomeExpressionConstants.MANIFESTS)
          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
          .build();
  private final RefObject infra = RefObject.newBuilder()
                                      .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                                      .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                                      .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                                      .build();

  public TasStepHelperTest() {}

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(mockLogCallback).when(cdStepHelper).getLogCallback(anyString(), eq(ambiance), anyBoolean());
    doReturn(EnvironmentType.PROD).when(stepHelper).getEnvironmentType(ambiance);
    doReturn(infrastructureOutcome).when(cdStepHelper).getInfrastructureOutcome(ambiance);
    doReturn(tasInfraConfig).when(cdStepHelper).getTasInfraConfig(infrastructureOutcome, ambiance);
    doAnswer(invocation -> invocation.getArgument(1, String.class))
        .when(engineExpressionService)
        .renderExpression(eq(ambiance), anyString());
    Reflect.on(tasStepHelper).set("cdStepHelper", cdStepHelper);
    doReturn(infrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetManifestType() {
    LogCallback logCallback = Mockito.mock(LogCallback.class);
    assertThat(tasStepHelper.getManifestType(MANIFEST_YML, null, logCallback)).isEqualTo(TAS_MANIFEST);
    assertThat(tasStepHelper.getManifestType(AUTOSCALAR_YML, null, logCallback)).isEqualTo(TAS_AUTOSCALER);
    assertThat(tasStepHelper.getManifestType(VARS_YML_1, null, logCallback)).isEqualTo(TAS_VARS);
    assertThat(tasStepHelper.getManifestType(NOT_VAR, null, logCallback)).isNull();
    assertThat(tasStepHelper.getManifestType(INVALID_MANIFEST_YML, null, logCallback)).isNull();
    assertThat(tasStepHelper.getManifestType(NO_YML, null, logCallback)).isNull();
    Mockito.verify(logCallback, Mockito.times(6)).saveExecutionLog(anyString(), eq(LogLevel.WARN));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetDeploymentIdentifier() {
    assertThat(tasStepHelper.getDeploymentIdentifier(
                   TasInfraConfig.builder().organization("org").space("space").build(), "testApp"))
        .isEqualTo("org-space-testApp");
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testFetchTasApplicationName() {
    TasManifestsPackage tasManifestsPackage = TasManifestsPackage.builder().manifestYml(MANIFEST_YML).build();
    assertThat(tasStepHelper.fetchTasApplicationName(tasManifestsPackage)).isEqualTo("test-tas");
    TasManifestsPackage tasManifestsPackageWithoutVars =
        TasManifestsPackage.builder().manifestYml(MANIFEST_YML_WITH_VARS_NAME).build();
    assertThatThrownBy(() -> tasStepHelper.fetchTasApplicationName(tasManifestsPackageWithoutVars))
        .hasMessage("No Valid Variable file Found, please verify var file is present and has valid structure");
    TasManifestsPackage tasManifestsPackageWithVars = TasManifestsPackage.builder()
                                                          .manifestYml(MANIFEST_YML_WITH_VARS_NAME)
                                                          .variableYmls(List.of(VARS_YML_1))
                                                          .build();
    assertThat(tasStepHelper.fetchTasApplicationName(tasManifestsPackageWithVars)).isEqualTo("test-tas");
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchCustomRemote() {
    TasManifestOutcome tasManifestOutcome =
        TasManifestOutcome.builder().store(CustomRemoteStoreConfig.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchBitBucket() {
    TasManifestOutcome tasManifestOutcome =
        TasManifestOutcome.builder().store(BitbucketStore.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchGitLab() {
    TasManifestOutcome tasManifestOutcome = TasManifestOutcome.builder().store(GitLabStore.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchGitHub() {
    TasManifestOutcome tasManifestOutcome = TasManifestOutcome.builder().store(GithubStore.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchGit() {
    TasManifestOutcome tasManifestOutcome = TasManifestOutcome.builder().store(GitStore.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldExecuteStoreFetchHarness() {
    TasManifestOutcome tasManifestOutcome = TasManifestOutcome.builder().store(HarnessStore.builder().build()).build();
    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder().build();
    tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData, tasManifestOutcome);
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isTrue();
    assertThatThrownBy(()
                           -> tasStepHelper.shouldExecuteStoreFetch(
                               tasStepPassThroughData, TasManifestOutcome.builder().identifier("test").build()))
        .hasMessage("Store is null for manifest: test");
    assertThatThrownBy(
        ()
            -> tasStepHelper.shouldExecuteStoreFetch(tasStepPassThroughData,
                TasManifestOutcome.builder().identifier("test").store(OciHelmChartConfig.builder().build()).build()))
        .hasMessage("Manifest store type: OciHelmChart is not supported yet");
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetCommandUnitsForTanzuCommand() {
    assertThat(tasStepHelper.getCommandUnitsForTanzuCommand(TasStepPassThroughData.builder().build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCommandScript, CfCommandUnitConstants.Pcfplugin,
            CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnitsForTanzuCommand(
                   TasStepPassThroughData.builder().shouldExecuteCustomFetch(true).build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCommandScript, CfCommandUnitConstants.FetchCustomFiles,
            CfCommandUnitConstants.Pcfplugin, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnitsForTanzuCommand(
                   TasStepPassThroughData.builder().shouldExecuteGitStoreFetch(true).build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCommandScript, K8sCommandUnitConstants.FetchFiles,
            CfCommandUnitConstants.Pcfplugin, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnitsForTanzuCommand(
                   TasStepPassThroughData.builder().shouldExecuteHarnessStoreFetch(true).build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCommandScript, CfCommandUnitConstants.FetchFiles,
            CfCommandUnitConstants.Pcfplugin, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnitsForTanzuCommand(TasStepPassThroughData.builder()
                                                                .shouldExecuteCustomFetch(true)
                                                                .shouldExecuteGitStoreFetch(true)
                                                                .shouldExecuteHarnessStoreFetch(true)
                                                                .build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCommandScript, CfCommandUnitConstants.FetchFiles,
            CfCommandUnitConstants.FetchCustomFiles, K8sCommandUnitConstants.FetchFiles,
            CfCommandUnitConstants.Pcfplugin, CfCommandUnitConstants.Wrapup));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetCommandUnits() {
    assertThat(tasStepHelper.getCommandUnits(tasBasicAppSetupStep, TasStepPassThroughData.builder().build()))
        .isEqualTo(asList(
            CfCommandUnitConstants.VerifyManifests, CfCommandUnitConstants.PcfSetup, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnits(
                   tasBasicAppSetupStep, TasStepPassThroughData.builder().shouldExecuteCustomFetch(true).build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchCustomFiles, CfCommandUnitConstants.VerifyManifests,
            CfCommandUnitConstants.PcfSetup, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnits(
                   tasBasicAppSetupStep, TasStepPassThroughData.builder().shouldExecuteGitStoreFetch(true).build()))
        .isEqualTo(asList(K8sCommandUnitConstants.FetchFiles, CfCommandUnitConstants.VerifyManifests,
            CfCommandUnitConstants.PcfSetup, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnits(
                   tasBasicAppSetupStep, TasStepPassThroughData.builder().shouldExecuteHarnessStoreFetch(true).build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchFiles, CfCommandUnitConstants.VerifyManifests,
            CfCommandUnitConstants.PcfSetup, CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnits(tasBasicAppSetupStep,
                   TasStepPassThroughData.builder()
                       .shouldExecuteCustomFetch(true)
                       .shouldExecuteGitStoreFetch(true)
                       .shouldExecuteHarnessStoreFetch(true)
                       .build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchFiles, CfCommandUnitConstants.FetchCustomFiles,
            K8sCommandUnitConstants.FetchFiles, CfCommandUnitConstants.VerifyManifests, CfCommandUnitConstants.PcfSetup,
            CfCommandUnitConstants.Wrapup));
    assertThat(tasStepHelper.getCommandUnits(tasRollingDeployStep,
                   TasStepPassThroughData.builder()
                       .shouldExecuteCustomFetch(true)
                       .shouldExecuteGitStoreFetch(true)
                       .shouldExecuteHarnessStoreFetch(true)
                       .build()))
        .isEqualTo(asList(CfCommandUnitConstants.FetchFiles, CfCommandUnitConstants.FetchCustomFiles,
            K8sCommandUnitConstants.FetchFiles, CfCommandUnitConstants.VerifyManifests, CfCommandUnitConstants.Deploy,
            CfCommandUnitConstants.Wrapup));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testBuildCustomManifestFetchConfig() {
    assertThat(tasStepHelper.buildCustomManifestFetchConfig(
                   "id", true, false, asList("path1", "path2"), "script", "accountId"))
        .isEqualTo(CustomManifestFetchConfig.builder()
                       .key("id")
                       .required(true)
                       .defaultSource(false)
                       .customManifestSource(CustomManifestSource.builder()
                                                 .script("script")
                                                 .filePaths(asList("path1", "path2"))
                                                 .accountId("accountId")
                                                 .build())
                       .build());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testCfCliVersionNGMapper() {
    assertThat(tasStepHelper.cfCliVersionNGMapper(CfCliVersionNG.V7)).isEqualTo(CfCliVersion.V7);
    assertThatThrownBy(() -> tasStepHelper.cfCliVersionNGMapper(null)).hasMessage("CF CLI Version can't be null");
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetRouteMaps() {
    assertThat(tasStepHelper.getRouteMaps(NO_ROUTE_TRUE_MANIFEST_YML, new ArrayList<>())).isEqualTo(emptyList());
    assertThat(tasStepHelper.getRouteMaps(NO_ROUTE_TRUE_MANIFEST_YML, asList("temp-route1", "temp-route2")))
        .isEqualTo(emptyList());
    assertThat(tasStepHelper.getRouteMaps(NO_ROUTE_FALSE_MANIFEST_YML, asList("temp-route1", "temp-route2")))
        .isEqualTo(asList("temp-route1", "temp-route2"));
    assertThat(tasStepHelper.getRouteMaps(NO_ROUTE_FALSE_MANIFEST_YML, new ArrayList<>())).isEqualTo(emptyList());
    assertThat(tasStepHelper.getRouteMaps(MANIFEST_YML, asList("temp-route1", "temp-route2")))
        .isEqualTo(asList("temp-route1", "temp-route2"));
    assertThat(tasStepHelper.getRouteMaps(MANIFEST_YML, new ArrayList<>())).isEqualTo(emptyList());
    assertThat(tasStepHelper.getRouteMaps(MANIFEST_YML_WITH_ROUTES, asList("temp-route1", "temp-route2")))
        .isEqualTo(asList("route1", "route2", "temp-route1", "temp-route2"));
    assertThat(tasStepHelper.getRouteMaps(MANIFEST_YML_WITH_ROUTES, new ArrayList<>()))
        .isEqualTo(asList("route1", "route2"));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testFetchMaxCountFromManifest() {
    TasManifestsPackage tasManifestsPackage = TasManifestsPackage.builder().manifestYml(MANIFEST_YML).build();
    assertThatThrownBy(() -> tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackage))
        .hasMessage("No Valid Variable file Found, please verify var file is present and has valid structure");

    tasManifestsPackage.setVariableYmls(List.of(VARS_YML_1));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackage)).isEqualTo(3);

    tasManifestsPackage.setManifestYml(MANIFEST_YML.replace("((INSTANCES))", "2"));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackage)).isEqualTo(2);

    TasManifestsPackage tasManifestsPackageProcesses =
        TasManifestsPackage.builder().manifestYml(MANIFEST_YML_WITH_WEB_PROCESS).build();
    assertThatThrownBy(() -> tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcesses))
        .hasMessage("No Valid Variable file Found, please verify var file is present and has valid structure");

    tasManifestsPackageProcesses.setVariableYmls(List.of(VARS_YML_1));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcesses)).isEqualTo(1);

    tasManifestsPackageProcesses.setManifestYml(MANIFEST_YML_WITH_WEB_PROCESS.replace("((WEB_INSTANCES))", "2"));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcesses)).isEqualTo(2);

    TasManifestsPackage tasManifestsPackageProcess =
        TasManifestsPackage.builder().manifestYml(MANIFEST_YML_WITHOUT_WEB_PROCESS).build();
    assertThatThrownBy(() -> tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcess))
        .hasMessage("No Valid Variable file Found, please verify var file is present and has valid structure");

    tasManifestsPackageProcess.setVariableYmls(List.of(VARS_YML_1));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcess)).isEqualTo(3);

    tasManifestsPackageProcess.setManifestYml(MANIFEST_YML_WITHOUT_WEB_PROCESS.replace("((INSTANCES))", "2"));
    assertThat(tasStepHelper.fetchMaxCountFromManifest(tasManifestsPackageProcess)).isEqualTo(2);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testRemoveCommentedLineFromScript() {
    assertThat(tasStepHelper.removeCommentedLineFromScript(COMMAND_SCRIPT)).isEqualTo(COMMAND_SCRIPT_WITHOUT_COMMENTS);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testAddToPcfManifestPackageByType() {
    LogCallback logCallback = Mockito.mock(LogCallback.class);
    TasManifestsPackage tasManifestsPackage = TasManifestsPackage.builder().variableYmls(new ArrayList<>()).build();

    tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, MANIFEST_YML, null, logCallback);
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isNull();
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(emptyList());

    assertThatThrownBy(
        () -> tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, MANIFEST_YML, null, logCallback))
        .hasMessage("Only one Tas Manifest Yml is supported");
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isNull();
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(emptyList());

    tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, AUTOSCALAR_YML, null, logCallback);
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isEqualTo(AUTOSCALAR_YML);
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(emptyList());

    assertThatThrownBy(
        () -> tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, AUTOSCALAR_YML, null, logCallback))
        .hasMessage("Only one AutoScalar Yml is supported");
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isEqualTo(AUTOSCALAR_YML);
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(emptyList());

    tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, VARS_YML_1, null, logCallback);
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isEqualTo(AUTOSCALAR_YML);
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(List.of(VARS_YML_1));

    tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, VARS_YML_2, null, logCallback);
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isEqualTo(AUTOSCALAR_YML);
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(asList(VARS_YML_1, VARS_YML_2));

    tasStepHelper.addToPcfManifestPackageByType(tasManifestsPackage, NOT_VAR, null, logCallback);
    assertThat(tasManifestsPackage.getManifestYml()).isEqualTo(MANIFEST_YML);
    assertThat(tasManifestsPackage.getAutoscalarManifestYml()).isEqualTo(AUTOSCALAR_YML);
    assertThat(tasManifestsPackage.getVariableYmls()).isEqualTo(asList(VARS_YML_1, VARS_YML_2));
    Mockito.verify(logCallback, Mockito.times(2)).saveExecutionLog(anyString(), eq(LogLevel.WARN));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigDocker() {
    final DockerArtifactOutcome dockerArtifactOutcome = DockerArtifactOutcome.builder()
                                                            .connectorRef("docker")
                                                            .image("harness")
                                                            .tag("test")
                                                            .type(DOCKER_REGISTRY_NAME)
                                                            .build();
    final DockerConnectorDTO dockerConnectorDTO =
        DockerConnectorDTO.builder()
            .auth(DockerAuthenticationDTO.builder().authType(DockerAuthType.ANONYMOUS).build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.DOCKER).connectorConfig(dockerConnectorDTO).build();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("docker", ambiance);

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, dockerArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(dockerConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("harness");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("test");
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.DOCKER_HUB_PUBLIC);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
    verify(secretManagerClientService, never()).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigDockerPrivate() {
    final DockerArtifactOutcome dockerArtifactOutcome = DockerArtifactOutcome.builder()
                                                            .connectorRef("docker")
                                                            .image("harness")
                                                            .tag("test")
                                                            .type(DOCKER_REGISTRY_NAME)
                                                            .build();
    final DockerConnectorDTO dockerConnectorDTO =
        DockerConnectorDTO.builder()
            .providerType(DockerRegistryProviderType.DOCKER_HUB)
            .auth(DockerAuthenticationDTO.builder()
                      .authType(DockerAuthType.USER_PASSWORD)
                      .credentials(
                          DockerUserNamePasswordDTO.builder()
                              .username("username")
                              .passwordRef(SecretRefData.builder().decryptedValue("password".toCharArray()).build())
                              .build())
                      .build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.DOCKER).connectorConfig(dockerConnectorDTO).build();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("docker", ambiance);

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, dockerArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(dockerConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("harness");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("test");
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.DOCKER_HUB_PRIVATE);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigGithubPackageRegistry() {
    final GithubPackagesArtifactOutcome githubPackagesArtifactOutcome = GithubPackagesArtifactOutcome.builder()
                                                                            .connectorRef("githubArtifact")
                                                                            .image("harness")
                                                                            .version("test")
                                                                            .type(GITHUB_PACKAGES_NAME)
                                                                            .build();
    final GithubConnectorDTO githubConnectorDTO =
        GithubConnectorDTO.builder()
            .authentication(
                GithubAuthenticationDTO.builder()
                    .authType(GitAuthType.HTTP)
                    .credentials(
                        GithubHttpCredentialsDTO.builder()
                            .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                            .httpCredentialsSpec(
                                GithubUsernamePasswordDTO.builder()
                                    .username("username")
                                    .passwordRef(
                                        SecretRefData.builder().decryptedValue("password".toCharArray()).build())
                                    .build())
                            .build())
                    .build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.GITHUB).connectorConfig(githubConnectorDTO).build();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("githubArtifact", ambiance);

    TasArtifactConfig tasArtifactConfig =
        tasStepHelper.getPrimaryArtifactConfig(ambiance, githubPackagesArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(githubConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("harness");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("test");
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.GITHUB_PACKAGE_REGISTRY);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigECR() {
    final EcrArtifactOutcome ecrArtifactOutcome = EcrArtifactOutcome.builder()
                                                      .type(ArtifactSourceConstants.ECR_NAME)
                                                      .connectorRef("ecr")
                                                      .image("harness")
                                                      .tag("test")
                                                      .build();
    final AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.IRSA).build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.AWS).connectorConfig(awsConnectorDTO).build();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("ecr", ambiance);

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, ecrArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(awsConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("harness");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("test");
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.ECR);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
    verify(secretManagerClientService, never()).getEncryptionDetails(any(), any());
  }
  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigACR() {
    final AcrArtifactOutcome acrArtifactOutcome = AcrArtifactOutcome.builder()
                                                      .subscription(ACR_SUBSCRIPTION_ID)
                                                      .registry(ACR_REGISTRY)
                                                      .repository(ACR_REPOSITORY)
                                                      .tag(ACR_TAG)
                                                      .type(ArtifactSourceConstants.ACR_NAME)
                                                      .connectorRef(ACR_CONNECTOR_REF)
                                                      .image(format("%s/%s:%s", ACR_REGISTRY, ACR_REPOSITORY, ACR_TAG))
                                                      .build();
    AzureConnectorDTO azureConnectorDTO =
        AzureConnectorDTO.builder()
            .credential(AzureCredentialDTO.builder()
                            .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                            .config(AzureManualDetailsDTO.builder()
                                        .tenantId("tenant-id")
                                        .clientId("client-id")
                                        .authDTO(AzureAuthDTO.builder()
                                                     .azureSecretType(AzureSecretType.SECRET_KEY)
                                                     .credentials(AzureClientSecretKeyDTO.builder().build())
                                                     .build())
                                        .build())
                            .build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.AZURE).connectorConfig(azureConnectorDTO).build();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("aztestref", ambiance);

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, acrArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(azureConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo(format("%s/%s:%s", ACR_REGISTRY, ACR_REPOSITORY, ACR_TAG));
    assertThat(containerArtifactConfig.getTag()).isEqualTo(ACR_TAG);
    assertThat(containerArtifactConfig.getRegistryHostname()).isEqualTo(ACR_REGISTRY);
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.ACR);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryArtifactConfigArtifactory() {
    final ArtifactoryArtifactOutcome artifactoryArtifactOutcome = ArtifactoryArtifactOutcome.builder()
                                                                      .connectorRef("artifactory")
                                                                      .image("harness")
                                                                      .tag("test")
                                                                      .type(ARTIFACTORY_REGISTRY_NAME)
                                                                      .build();
    final ArtifactoryUsernamePasswordAuthDTO usernamePasswordAuthDTO =
        ArtifactoryUsernamePasswordAuthDTO.builder().build();
    final ArtifactoryConnectorDTO artifactoryConnectorDTO = ArtifactoryConnectorDTO.builder()
                                                                .auth(ArtifactoryAuthenticationDTO.builder()
                                                                          .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                          .credentials(usernamePasswordAuthDTO)
                                                                          .build())
                                                                .build();
    final ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                                  .connectorType(ConnectorType.ARTIFACTORY)
                                                  .connectorConfig(artifactoryConnectorDTO)
                                                  .build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("artifactory", ambiance);
    doReturn(encryptedDataDetails)
        .when(secretManagerClientService)
        .getEncryptionDetails(any(NGAccess.class), eq(usernamePasswordAuthDTO));

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, artifactoryArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfig;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(artifactoryConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("harness");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("test");
    assertThat(containerArtifactConfig.getRegistryType())
        .isEqualTo(TasArtifactRegistryType.ARTIFACTORY_PRIVATE_REGISTRY);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEqualTo(encryptedDataDetails);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigAwsS3() {
    final S3ArtifactOutcome s3ArtifactOutcome = S3ArtifactOutcome.builder()
                                                    .connectorRef("s3awsconnector")
                                                    .bucketName("testBucketName")
                                                    .region("testRegion")
                                                    .filePath("test_app.war")
                                                    .type(AMAZON_S3_NAME)
                                                    .build();

    final AwsCredentialDTO awsCredentialDTO = AwsCredentialDTO.builder()
                                                  .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                                  .config(AwsManualConfigSpecDTO.builder().build())
                                                  .build();

    final AwsConnectorDTO awsConnectorDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.AWS).connectorConfig(awsConnectorDTO).build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("s3awsconnector", ambiance);
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, s3ArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    AwsS3TasArtifactRequestDetails azureArtifactRequestDetails =
        (AwsS3TasArtifactRequestDetails) packageArtifactConfig.getArtifactDetails();

    assertThat(packageArtifactConfig.getConnectorConfig()).isEqualTo(awsConnectorDTO);
    assertThat(azureArtifactRequestDetails.getBucketName()).isEqualTo("testBucketName");
    assertThat(azureArtifactRequestDetails.getFilePath()).isEqualTo("test_app.war");
    assertThat(azureArtifactRequestDetails.getRegion()).isEqualTo("testRegion");
    assertThat(packageArtifactConfig.getSourceType()).isEqualTo(AMAZONS3);
    assertThat(packageArtifactConfig.getEncryptedDataDetails()).isEqualTo(encryptedDataDetails);
  }
  private NexusArtifactOutcome getNexusArtifactOutcome(Map<String, String> metadata, String repositoryFormat) {
    return NexusArtifactOutcome.builder()
        .repositoryFormat(repositoryFormat)
        .type(NEXUS3_REGISTRY_NAME)
        .image("nginx")
        .primaryArtifact(true)
        .metadata(metadata)
        .identifier(ARTIFACT_ID)
        .connectorRef(CONNECTOR_REF)
        .tag("latest")
        .build();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigNexus3() {
    final Map<String, String> metadata = ImmutableMap.of("url", "https://nexus3.dev/repo/abc/def");
    final NexusConnectorDTO nexusConnectorDTO =
        NexusConnectorDTO.builder()
            .auth(NexusAuthenticationDTO.builder().authType(NexusAuthType.ANONYMOUS).build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.NEXUS).connectorConfig(nexusConnectorDTO).build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(CONNECTOR_REF, ambiance);
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());

    TasArtifactConfig tasArtifactConfig =
        tasStepHelper.getPrimaryArtifactConfig(ambiance, getNexusArtifactOutcome(metadata, NEXUS_MAVEN_FORMAT));
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    NexusTasArtifactRequestDetails nexusRequestDetails =
        (NexusTasArtifactRequestDetails) packageArtifactConfig.getArtifactDetails();
    assertThat(packageArtifactConfig.getConnectorConfig()).isEqualTo(nexusConnectorDTO);
    assertThat(nexusRequestDetails.getArtifactUrl()).isEqualTo(NEXUS_URL);
    assertThat(nexusRequestDetails.getRepositoryFormat()).isEqualTo(NEXUS_MAVEN_FORMAT);
    assertThat(nexusRequestDetails.isCertValidationRequired()).isFalse();
    assertThat(nexusRequestDetails.getMetadata()).isEqualTo(metadata);

    TasArtifactConfig tasArtifactConfigDocker =
        tasStepHelper.getPrimaryArtifactConfig(ambiance, getNexusArtifactOutcome(metadata, NEXUS_DOCKER_FORMAT));
    assertThat(tasArtifactConfigDocker.getArtifactType()).isEqualTo(TasArtifactType.CONTAINER);
    TasContainerArtifactConfig containerArtifactConfig = (TasContainerArtifactConfig) tasArtifactConfigDocker;
    assertThat(containerArtifactConfig.getConnectorConfig()).isEqualTo(nexusConnectorDTO);
    assertThat(containerArtifactConfig.getImage()).isEqualTo("nginx");
    assertThat(containerArtifactConfig.getTag()).isEqualTo("latest");
    assertThat(containerArtifactConfig.getRegistryType()).isEqualTo(TasArtifactRegistryType.NEXUS_PRIVATE_REGISTRY);
    assertThat(containerArtifactConfig.getEncryptedDataDetails()).isEmpty();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigCustomArtifact() {
    final Map<String, String> metadata = ImmutableMap.of("url", "https://nexus3.dev/repo/abc/def");
    final CustomArtifactOutcome nexusArtifactOutcome = CustomArtifactOutcome.builder()
                                                           .primaryArtifact(true)
                                                           .version("latest")
                                                           .identifier("primary")
                                                           .image("nginx")
                                                           .displayName("nginx")
                                                           .artifactPath("tmp")
                                                           .metadata(metadata)
                                                           .build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    verify(cdStepHelper, Mockito.times(0)).getConnector(any(), any());
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, nexusArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    CustomArtifactTasRequestDetails customArtifactTasRequestDetails =
        (CustomArtifactTasRequestDetails) packageArtifactConfig.getArtifactDetails();
    assertThat(packageArtifactConfig.getConnectorConfig()).isNull();
    assertThat(customArtifactTasRequestDetails.getArtifactPath()).isEqualTo("tmp");
    assertThat(customArtifactTasRequestDetails.getIdentifier()).isEqualTo("primary");
    assertThat(customArtifactTasRequestDetails.getImage()).isEqualTo("nginx");
    assertThat(customArtifactTasRequestDetails.getDisplayName()).isEqualTo("nginx");
    assertThat(customArtifactTasRequestDetails.getMetadata()).isEqualTo(metadata);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigNexus2() {
    final Map<String, String> metadata = ImmutableMap.of("url", "https://nexus2.dev/repo/abc/def");
    final NexusArtifactOutcome nexusArtifactOutcome = NexusArtifactOutcome.builder()
                                                          .repositoryFormat("maven")
                                                          .type(NEXUS2_REGISTRY_NAME)
                                                          .primaryArtifact(true)
                                                          .metadata(metadata)
                                                          .identifier("primary")
                                                          .connectorRef("nexusconnector")
                                                          .build();
    final NexusConnectorDTO nexusConnectorDTO =
        NexusConnectorDTO.builder()
            .auth(NexusAuthenticationDTO.builder().authType(NexusAuthType.ANONYMOUS).build())
            .build();
    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.NEXUS).connectorConfig(nexusConnectorDTO).build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("nexusconnector", ambiance);
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, nexusArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    NexusTasArtifactRequestDetails nexusRequestDetails =
        (NexusTasArtifactRequestDetails) packageArtifactConfig.getArtifactDetails();
    assertThat(packageArtifactConfig.getConnectorConfig()).isEqualTo(nexusConnectorDTO);
    assertThat(nexusRequestDetails.getArtifactUrl()).isEqualTo("https://nexus2.dev/repo/abc/def");
    assertThat(nexusRequestDetails.getRepositoryFormat()).isEqualTo("maven");
    assertThat(nexusRequestDetails.isCertValidationRequired()).isFalse();
    assertThat(nexusRequestDetails.getMetadata()).isEqualTo(metadata);
  }
  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigAzureArtifacts() {
    final AzureArtifactsOutcome azureArtifactsOutcome = AzureArtifactsOutcome.builder()
                                                            .connectorRef("azureConenectorRef")
                                                            .scope("org")
                                                            .feed("testFeed")
                                                            .packageType("maven")
                                                            .packageName("com.test.my")
                                                            .version("testVersion")
                                                            .type(AZURE_ARTIFACTS_NAME)
                                                            .build();

    AzureArtifactsCredentialsDTO azureArtifactsCredentialsDTO =
        AzureArtifactsCredentialsDTO.builder()
            .type(AzureArtifactsAuthenticationType.PERSONAL_ACCESS_TOKEN)
            .credentialsSpec(AzureArtifactsTokenDTO.builder().tokenRef(SecretRefData.builder().build()).build())
            .build();

    AzureArtifactsConnectorDTO azureArtifactsConnectorDTO =
        AzureArtifactsConnectorDTO.builder()
            .azureArtifactsUrl("dummyDevopsAzureURL")
            .auth(AzureArtifactsAuthenticationDTO.builder().credentials(azureArtifactsCredentialsDTO).build())

            .build();

    final ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                                  .connectorType(ConnectorType.AZURE_ARTIFACTS)
                                                  .connectorConfig(azureArtifactsConnectorDTO)
                                                  .build();

    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("azureConenectorRef", ambiance);
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());

    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, azureArtifactsOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    AzureDevOpsTasArtifactRequestDetails azureArtifactRequestDetails =
        (AzureDevOpsTasArtifactRequestDetails) packageArtifactConfig.getArtifactDetails();

    assertThat(packageArtifactConfig.getConnectorConfig()).isEqualTo(azureArtifactsConnectorDTO);
    assertThat(azureArtifactRequestDetails.getFeed()).isEqualTo("testFeed");
    assertThat(azureArtifactRequestDetails.getScope()).isEqualTo("org");
    assertThat(azureArtifactRequestDetails.getPackageType()).isEqualTo("maven");
    assertThat(azureArtifactRequestDetails.getPackageName()).isEqualTo("com.test.my");
    assertThat(azureArtifactRequestDetails.getVersion()).isEqualTo("testVersion");
    assertThat(packageArtifactConfig.getSourceType()).isEqualTo(AZURE_ARTIFACTS);
    assertThat(packageArtifactConfig.getEncryptedDataDetails()).isEqualTo(encryptedDataDetails);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testGetPrimaryPackageArtifactConfigJenkins() {
    final JenkinsArtifactOutcome jenkinsArtifactOutcome = JenkinsArtifactOutcome.builder()
                                                              .connectorRef("jenkinsConnector")
                                                              .artifactPath("testArtifact")
                                                              .jobName("testJobName")
                                                              .build("testBuild")
                                                              .identifier("testIdentifier")
                                                              .type(JENKINS_NAME)
                                                              .build();

    final JenkinsUserNamePasswordDTO awsCredentialDTO = JenkinsUserNamePasswordDTO.builder()
                                                            .username("testUsername")
                                                            .passwordRef(SecretRefData.builder().build())
                                                            .build();

    final JenkinsConnectorDTO jenkinsConnectorDTO = JenkinsConnectorDTO.builder()
                                                        .jenkinsUrl("testJenkinsUrl")
                                                        .auth(JenkinsAuthenticationDTO.builder()
                                                                  .authType(JenkinsAuthType.USER_PASSWORD)
                                                                  .credentials(awsCredentialDTO)
                                                                  .build())
                                                        .build();

    final ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorType(ConnectorType.JENKINS).connectorConfig(jenkinsConnectorDTO).build();
    final List<EncryptedDataDetail> encryptedDataDetails = singletonList(EncryptedDataDetail.builder().build());

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector("jenkinsConnector", ambiance);
    doReturn(encryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(NGAccess.class), any());
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(anyString(), any());
    TasArtifactConfig tasArtifactConfig = tasStepHelper.getPrimaryArtifactConfig(ambiance, jenkinsArtifactOutcome);
    assertThat(tasArtifactConfig.getArtifactType()).isEqualTo(TasArtifactType.PACKAGE);
    TasPackageArtifactConfig packageArtifactConfig = (TasPackageArtifactConfig) tasArtifactConfig;

    JenkinsTasArtifactRequestDetails azureArtifactRequestDetails =
        (JenkinsTasArtifactRequestDetails) packageArtifactConfig.getArtifactDetails();

    assertThat(packageArtifactConfig.getConnectorConfig()).isEqualTo(jenkinsConnectorDTO);
    assertThat(azureArtifactRequestDetails.getArtifactPath()).isEqualTo("testArtifact");
    assertThat(azureArtifactRequestDetails.getBuild()).isEqualTo("testBuild");
    assertThat(azureArtifactRequestDetails.getJobName()).isEqualTo("testJobName");
    assertThat(packageArtifactConfig.getSourceType()).isEqualTo(JENKINS);
    assertThat(packageArtifactConfig.getEncryptedDataDetails()).isEqualTo(encryptedDataDetails);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldPrepareTasGitValuesFetchTask() {
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("tas",
        getTasManifestOutcome(0, getGitStore("master", asList("path/to/tas/manifest/tasManifest.yml"), "git-connector"),
            "tas", asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"),
            "path/to/tas/manifest/autoscalar.yml"));
    StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(TasBasicAppSetupStepParameters.infoBuilder().build()).build();
    OptionalOutcome manifestsOutcome =
        OptionalOutcome.builder().found(true).outcome(new ManifestsOutcome(manifestOutcomeMap)).build();
    doReturn(manifestsOutcome).when(outcomeService).resolveOptional(eq(ambiance), eq(manifests));
    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder()
                                       .connectorConfig(
                                           GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url("SOME_URL").build())
                                       .name("test")
                                       .build())
                        .build()))
        .when(connectorService)
        .get(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class));
    TaskChainResponse taskChainResponse =
        tasStepHelper.startChainLink(tasStepExecutor, ambiance, stepElementParameters);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(TasStepPassThroughData.class);
    TasStepPassThroughData tasStepPassThroughData = (TasStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList()).isEmpty();
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getAutoScalerManifestOutcome()).isNull();
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(4)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(GitFetchRequest.class);
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskParameters;
    assertThat(gitFetchRequest.getGitFetchFilesConfigs()).isNotEmpty();
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(3);
    assertGitConfig(gitFetchRequest.getGitFetchFilesConfigs().get(0), 2,
        asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"));
    assertGitConfig(gitFetchRequest.getGitFetchFilesConfigs().get(1), 1, asList("path/to/tas/manifest/autoscalar.yml"));
    assertGitConfig(
        gitFetchRequest.getGitFetchFilesConfigs().get(2), 1, asList("path/to/tas/manifest/tasManifest.yml"));
    assertThat(argumentCaptor.getAllValues().get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  private TasManifestOutcome getTasManifestOutcome(
      int order, StoreConfig storeConfig, String identifier, List<String> varsPaths, String autoScalarPath) {
    ParameterField<List<String>> autoScalerPath =
        isEmpty(autoScalarPath) ? null : ParameterField.createValueField(asList(autoScalarPath));
    return TasManifestOutcome.builder()
        .identifier(identifier)
        .cfCliVersion(CfCliVersionNG.V7)
        .store(storeConfig)
        .order(order)
        .varsPaths(ParameterField.createValueField(varsPaths))
        .autoScalerPath(autoScalerPath)
        .build();
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldPrepareTasGitValuesFetchTaskWithTasManifestOverride() {
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("tas",
        getTasManifestOutcome(0, getGitStore("master", asList("path/to/tas/manifest/tasManifest.yml"), "git-connector"),
            "tas", asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"),
            "path/to/tas/manifest/autoscalar.yml"),
        "tasOverride",
        getTasManifestOutcome(2,
            getGitStore("master", asList("path/to/tasOverride/manifest/tasManifest.yml"), "git-connector"),
            "tasOverride", asList("path/to/tasOverride/manifest/vars1.yml", "path/to/tasOverride/manifest/vars2.yml"),
            null),
        "autoScalarOverride",
        getAutoScalarManifestOutcome(
            1, getGitStore("master", asList("path/to/autoScalar.yml"), "git-connector"), "autoScalarOverride"),
        "varsOverride",
        getVarsManifestOutcome(3,
            getGitStore("master", asList("path/to/varsOverride1.yml", "path/to/varsOverride2.yml"), "git-connector"),
            "varsOverride"));
    StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(TasBasicAppSetupStepParameters.infoBuilder().build()).build();
    OptionalOutcome manifestsOutcome =
        OptionalOutcome.builder().found(true).outcome(new ManifestsOutcome(manifestOutcomeMap)).build();
    doReturn(manifestsOutcome).when(outcomeService).resolveOptional(eq(ambiance), eq(manifests));
    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder()
                                       .connectorConfig(
                                           GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url("SOME_URL").build())
                                       .name("test")
                                       .build())
                        .build()))
        .when(connectorService)
        .get(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class));
    TaskChainResponse taskChainResponse =
        tasStepHelper.startChainLink(tasStepExecutor, ambiance, stepElementParameters);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(TasStepPassThroughData.class);
    TasStepPassThroughData tasStepPassThroughData = (TasStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList().size()).isEqualTo(1);
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList().get(0))
        .isEqualTo(manifestOutcomeMap.get("varsOverride"));
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getAutoScalerManifestOutcome())
        .isEqualTo(manifestOutcomeMap.get("autoScalarOverride"));
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(5)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(GitFetchRequest.class);
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskParameters;
    assertThat(gitFetchRequest.getGitFetchFilesConfigs()).isNotEmpty();
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(4);
    assertGitConfig(gitFetchRequest.getGitFetchFilesConfigs().get(0), 2,
        asList("path/to/varsOverride1.yml", "path/to/varsOverride2.yml"));
    assertGitConfig(gitFetchRequest.getGitFetchFilesConfigs().get(1), 1, asList("path/to/autoScalar.yml"));
    assertGitConfig(gitFetchRequest.getGitFetchFilesConfigs().get(2), 2,
        asList("path/to/tasOverride/manifest/vars1.yml", "path/to/tasOverride/manifest/vars2.yml"));
    assertGitConfig(
        gitFetchRequest.getGitFetchFilesConfigs().get(3), 1, asList("path/to/tasOverride/manifest/tasManifest.yml"));
    assertThat(argumentCaptor.getAllValues().get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldPrepareTasGitCustomAndLocalStoreWithTasManifestOverride() {
    List<String> files = asList("org:/path/to/tas/manifests");
    HarnessStore harnessStore = HarnessStore.builder().files(ParameterField.createValueField(files)).build();
    String extractionScript = "git clone something.git";
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("tas",
        getTasManifestOutcome(0, getGitStore("master", asList("path/to/tas/manifest/tasManifest.yml"), "git-connector"),
            "tas", asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"),
            "path/to/tas/manifest/autoscalar.yml"),
        "tasOverride",
        getTasManifestOutcome(1, harnessStore, "tasOverride",
            asList("path/to/tasOverride/manifest/vars1.yml", "path/to/tasOverride/manifest/vars2.yml"), null),
        "varsOverride",
        getVarsManifestOutcome(
            2, getCustomRemoteStoreConfig(extractionScript, "folderPath/vars.yaml"), "varsOverride"));
    StepElementParameters stepElementParameters =
        StepElementParameters.builder()
            .spec(TasBasicAppSetupStepParameters.infoBuilder()
                      .delegateSelectors(ParameterField.createValueField(List.of(new TaskSelectorYaml("selector-1"))))
                      .existingVersionToKeep(ParameterField.createValueField("3"))
                      .build())
            .build();
    OptionalOutcome manifestsOutcome =
        OptionalOutcome.builder().found(true).outcome(new ManifestsOutcome(manifestOutcomeMap)).build();
    doReturn(manifestsOutcome).when(outcomeService).resolveOptional(eq(ambiance), eq(manifests));
    doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/manifest.yaml", "manifest.yaml", MANIFEST_YML)))
        .doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/vars.yaml", "vars.yaml", VARS_YML_1)))
        .doReturn(Optional.of(getFolderStoreNode("/path/to/tas/manifests", "manifests")))
        .when(fileStoreService)
        .getWithChildrenByPath(any(), any(), any(), any(), eq(true));

    TaskChainResponse taskChainResponse =
        tasStepHelper.startChainLink(tasStepExecutor, ambiance, stepElementParameters);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(TasStepPassThroughData.class);
    TasStepPassThroughData tasStepPassThroughData = (TasStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList().size()).isEqualTo(1);
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList().get(0))
        .isEqualTo(manifestOutcomeMap.get("varsOverride"));
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getLocalStoreFileMapContents().containsKey("1")).isTrue();
    assertThat(tasStepPassThroughData.getLocalStoreFileMapContents().get("1"))
        .isEqualTo(
            asList(getTasManifestFileContents(TAS_MANIFEST.toString(), "org:/path/to/tas/manifests", MANIFEST_YML),
                getTasManifestFileContents(TAS_VARS.toString(), "path/to/tasOverride/manifest/vars1.yml", VARS_YML_1)));
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isTrue();
    assertThat(tasStepPassThroughData.getAutoScalerManifestOutcome()).isNull();
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(1)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(CustomManifestValuesFetchParams.class);
    CustomManifestValuesFetchParams customManifestValuesFetchRequest = (CustomManifestValuesFetchParams) taskParameters;

    assertThat(customManifestValuesFetchRequest.getFetchFilesList().size()).isEqualTo(1);
    assertThat(customManifestValuesFetchRequest.getFetchFilesList().get(0).getCustomManifestSource().getAccountId())
        .isEqualTo("test-account");
    assertThat(customManifestValuesFetchRequest.getFetchFilesList().get(0).getCustomManifestSource().getScript())
        .isEqualTo(extractionScript);
    assertThat(customManifestValuesFetchRequest.getFetchFilesList().get(0).getCustomManifestSource().getFilePaths())
        .isEqualTo(asList("folderPath/vars.yaml"));
  }

  @Test
  @Owner(developers = RISHABH)
  @Category(UnitTests.class)
  public void testShouldPrepareTasCustomManifestWithTasManifestOverride() {
    String extractionScript = "git clone something.git";
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("tas",
        getTasManifestOutcome(0, getCustomRemoteStoreConfig(extractionScript, "folderPath/manifest.yaml"), "tas",
            asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"),
            "path/to/tas/manifest/autoscalar.yml"));
    StepElementParameters stepElementParameters =
        StepElementParameters.builder()
            .spec(TasBasicAppSetupStepParameters.infoBuilder()
                      .delegateSelectors(ParameterField.createValueField(List.of(new TaskSelectorYaml("selector-1"))))
                      .existingVersionToKeep(ParameterField.createValueField("3"))
                      .build())
            .build();
    OptionalOutcome manifestsOutcome =
        OptionalOutcome.builder().found(true).outcome(new ManifestsOutcome(manifestOutcomeMap)).build();
    doReturn(manifestsOutcome).when(outcomeService).resolveOptional(eq(ambiance), eq(manifests));

    TaskChainResponse taskChainResponse =
        tasStepHelper.startChainLink(tasStepExecutor, ambiance, stepElementParameters);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(TasStepPassThroughData.class);
    TasStepPassThroughData tasStepPassThroughData = (TasStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(tasStepPassThroughData.getVarsManifestOutcomeList().size()).isEqualTo(0);
    assertThat(tasStepPassThroughData.getShouldExecuteGitStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getShouldExecuteCustomFetch()).isTrue();
    assertThat(tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()).isFalse();
    assertThat(tasStepPassThroughData.getAutoScalerManifestOutcome()).isNull();
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(1)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(CustomManifestValuesFetchParams.class);
    CustomManifestValuesFetchParams customManifestValuesFetchRequest = (CustomManifestValuesFetchParams) taskParameters;

    assertCustomManifestConfig(customManifestValuesFetchRequest.getCustomManifestSource(), "test-account",
        extractionScript, asList("folderPath/manifest.yaml"));
    assertThat(customManifestValuesFetchRequest.getFetchFilesList().size()).isEqualTo(3);
    assertCustomManifestConfig(customManifestValuesFetchRequest.getFetchFilesList().get(0).getCustomManifestSource(),
        "test-account", extractionScript, asList("folderPath/manifest.yaml"));
    assertCustomManifestConfig(customManifestValuesFetchRequest.getFetchFilesList().get(1).getCustomManifestSource(),
        "test-account", null, asList("path/to/tas/manifest/vars1.yml", "path/to/tas/manifest/vars2.yml"));
    assertCustomManifestConfig(customManifestValuesFetchRequest.getFetchFilesList().get(2).getCustomManifestSource(),
        "test-account", null, asList("path/to/tas/manifest/autoscalar.yml"));
  }

  private void assertGitConfig(
      GitFetchFilesConfig gitFetchFilesConfig, int expectedPathSize, List<String> expectedPaths) {
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths()).isNotEmpty();
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().size()).isEqualTo(expectedPathSize);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths()).isEqualTo(expectedPaths);
  }

  private void assertCustomManifestConfig(
      CustomManifestSource customManifestSource, String accountId, String script, List<String> filePaths) {
    assertThat(customManifestSource.getAccountId()).isEqualTo(accountId);
    assertThat(customManifestSource.getScript()).isEqualTo(script);
    assertThat(customManifestSource.getFilePaths()).isEqualTo(filePaths);
  }

  private AutoScalerManifestOutcome getAutoScalarManifestOutcome(
      int order, StoreConfig storeConfig, String identifier) {
    return AutoScalerManifestOutcome.builder().identifier(identifier).store(storeConfig).order(order).build();
  }

  private VarsManifestOutcome getVarsManifestOutcome(int order, StoreConfig storeConfig, String identifier) {
    return VarsManifestOutcome.builder().identifier(identifier).store(storeConfig).order(order).build();
  }

  private GitStore getGitStore(String branch, List<String> paths, String connectorRef) {
    return GitStore.builder()
        .branch(ParameterField.createValueField(branch))
        .paths(ParameterField.createValueField(paths))
        .connectorRef(ParameterField.createValueField(connectorRef))
        .build();
  }

  private CustomRemoteStoreConfig getCustomRemoteStoreConfig(String extractionScript, String path) {
    return CustomRemoteStoreConfig.builder()
        .filePath(ParameterField.createValueField(path))
        .extractionScript(ParameterField.createValueField(extractionScript))
        .delegateSelectors(ParameterField.createValueField(asList(new TaskSelectorYaml("sample-delegate"))))
        .build();
  }

  private TasManifestFileContents getTasManifestFileContents(String manifestType, String path, String fileContent) {
    return TasManifestFileContents.builder().manifestType(manifestType).filePath(path).fileContent(fileContent).build();
  }

  private FileStoreNodeDTO getFileStoreNode(String path, String name, String fileContent) {
    return FileNodeDTO.builder()
        .name(name)
        .identifier(name)
        .fileUsage(FileUsage.MANIFEST_FILE)
        .parentIdentifier("folder")
        .content(fileContent)
        .path(path)
        .build();
  }

  private FileStoreNodeDTO getFolderStoreNode(String path, String name) {
    return FolderNodeDTO.builder().name(name).identifier("identifier").parentIdentifier("tas").path(path).build();
  }
}
