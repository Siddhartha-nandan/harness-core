/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.ManifestType.HELM_MANIFEST;
import static io.harness.ngtriggers.beans.source.NGTriggerType.ARTIFACT;
import static io.harness.ngtriggers.beans.source.NGTriggerType.MANIFEST;
import static io.harness.polling.contracts.HelmVersion.V2;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BUHA;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SHIVAM;
import static io.harness.rule.OwnerRule.VINICIUS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.artifact.EcrSpec;
import io.harness.ngtriggers.beans.source.artifact.HelmManifestSpec;
import io.harness.ngtriggers.beans.source.artifact.ManifestTriggerConfig;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.inputset.InputSetErrorDTOPMS;
import io.harness.pms.inputset.InputSetErrorResponseDTOPMS;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.pipeline.PMSPipelineResponseDTO;
import io.harness.polling.contracts.AcrPayload;
import io.harness.polling.contracts.ArtifactPathList;
import io.harness.polling.contracts.ArtifactoryRegistryPayload;
import io.harness.polling.contracts.BambooPayload;
import io.harness.polling.contracts.BuildInfo;
import io.harness.polling.contracts.DockerHubPayload;
import io.harness.polling.contracts.EcrPayload;
import io.harness.polling.contracts.GcrPayload;
import io.harness.polling.contracts.GcsHelmPayload;
import io.harness.polling.contracts.HttpHelmPayload;
import io.harness.polling.contracts.Nexus2RegistryPayload;
import io.harness.polling.contracts.Nexus3RegistryPayload;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.contracts.PollingResponse;
import io.harness.polling.contracts.Qualifier;
import io.harness.polling.contracts.S3HelmPayload;
import io.harness.rule.Owner;
import io.harness.utils.PmsFeatureFlagHelper;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PIPELINE)
public class BuildTriggerHelperTest extends CategoryTest {
  @Mock PmsFeatureFlagHelper pmsFeatureFlagHelper;
  @InjectMocks NGTriggerElementMapper ngTriggerElementMapper;
  @InjectMocks BuildTriggerHelper buildTriggerHelper;
  @Mock private PipelineServiceClient pipelineServiceClient;

  private static final String INPUT = "<+input>";

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_DockerRegistry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setDockerHubPayload(DockerHubPayload.newBuilder().setImagePath("test").build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setDockerHubPayload(DockerHubPayload.newBuilder().setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setDockerHubPayload(DockerHubPayload.newBuilder().build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_ArtifactoryGenericRegistry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactDirectory("dir")
                                               .setRepositoryFormat("generic")
                                               .setArtifactPath(Strings.EMPTY)
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef(Strings.EMPTY)
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactDirectory("dir")
                                               .setRepositoryFormat("generic")
                                               .setArtifactPath(Strings.EMPTY)
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRepository = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository(Strings.EMPTY)
                                               .setArtifactFilter("filter")
                                               .setRepositoryFormat("generic")
                                               .setArtifactPath(Strings.EMPTY)
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepository))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repository can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoArtifactDirectory =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                                   .setRepository("repo")
                                                   .setArtifactDirectory(Strings.EMPTY)
                                                   .setRepositoryFormat("generic")
                                                   .setArtifactPath(Strings.EMPTY)
                                                   .setRepositoryUrl(Strings.EMPTY)
                                                   .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoArtifactDirectory))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "artifactDirectory, artifactFilter can not be blank or Runtime input. Please provide concrete value to one of these.");

    final PollingItem pollingItemRuntimeArtifactDirectory =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                                   .setRepository("repo")
                                                   .setArtifactDirectory(INPUT)
                                                   .setRepositoryFormat("generic")
                                                   .setArtifactPath(Strings.EMPTY)
                                                   .setRepositoryUrl(Strings.EMPTY)
                                                   .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemRuntimeArtifactDirectory))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "artifactDirectory, artifactFilter can not be blank or Runtime input. Please provide concrete value to one of these.");

    final PollingItem pollingItemNoArtifactFilter = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactFilter(Strings.EMPTY)
                                               .setRepositoryFormat("generic")
                                               .setArtifactPath(Strings.EMPTY)
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoArtifactFilter))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "artifactDirectory, artifactFilter can not be blank or Runtime input. Please provide concrete value to one of these.");

    final PollingItem pollingItemRuntimeArtifactFilter =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                                   .setRepository("repo")
                                                   .setArtifactFilter(INPUT)
                                                   .setRepositoryFormat("generic")
                                                   .setArtifactPath(Strings.EMPTY)
                                                   .setRepositoryUrl(Strings.EMPTY)
                                                   .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemRuntimeArtifactFilter))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "artifactDirectory, artifactFilter can not be blank or Runtime input. Please provide concrete value to one of these.");

    final PollingItem pollingItemNoRepositoryFormat =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                                   .setRepository("repo")
                                                   .setArtifactDirectory("dir")
                                                   .setRepositoryFormat(Strings.EMPTY)
                                                   .setArtifactPath(Strings.EMPTY)
                                                   .setRepositoryUrl(Strings.EMPTY)
                                                   .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepositoryFormat))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repositoryFormat can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_ArtifactoryDockerRegistry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactDirectory(Strings.EMPTY)
                                               .setRepositoryFormat("docker")
                                               .setArtifactPath("path")
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef(Strings.EMPTY)
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactDirectory(Strings.EMPTY)
                                               .setRepositoryFormat("docker")
                                               .setArtifactPath("path")
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRepository = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository(Strings.EMPTY)
                                               .setArtifactDirectory(Strings.EMPTY)
                                               .setRepositoryFormat("docker")
                                               .setArtifactPath("path")
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepository))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repository can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoArtifactPath = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                               .setRepository("repo")
                                               .setArtifactDirectory(Strings.EMPTY)
                                               .setRepositoryFormat("docker")
                                               .setArtifactPath(Strings.EMPTY)
                                               .setRepositoryUrl(Strings.EMPTY)
                                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoArtifactPath))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("artifactPath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRepositoryFormat =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                                   .setRepository("repo")
                                                   .setArtifactDirectory(Strings.EMPTY)
                                                   .setRepositoryFormat(Strings.EMPTY)
                                                   .setArtifactPath("path")
                                                   .setRepositoryUrl(Strings.EMPTY)
                                                   .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepositoryFormat))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repositoryFormat can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Gcr() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").setImagePath("test").build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRegistryHostName =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setGcrPayload(GcrPayload.newBuilder().setImagePath("test").build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRegistryHostName))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("registryHostname can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_ecr() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").setImagePath("test").build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRegion = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRegion))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("region can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForHelmHttp() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setHttpHelmPayload(HttpHelmPayload.newBuilder().setHelmVersion(V2).setChartName("chart").build())
            .build());

    validatePollingItemForManifest(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setHttpHelmPayload(HttpHelmPayload.newBuilder().setHelmVersion(V2).setChartName("chart").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoChart = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setHttpHelmPayload(HttpHelmPayload.newBuilder().setHelmVersion(V2).build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoChart))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ChartName can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForHelmS3() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setS3HelmPayload(S3HelmPayload.newBuilder()
                                  .setHelmVersion(V2)
                                  .setChartName("chart")
                                  .setBucketName("bucket")
                                  .setFolderPath("path")
                                  .setRegion("us-east-1")
                                  .build())
            .build());

    validatePollingItemForManifest(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setS3HelmPayload(S3HelmPayload.newBuilder()
                                  .setHelmVersion(V2)
                                  .setChartName("chart")
                                  .setBucketName("bucket")
                                  .setFolderPath("path")
                                  .setRegion("us-east-1")
                                  .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoChart = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setS3HelmPayload(S3HelmPayload.newBuilder()
                                  .setHelmVersion(V2)
                                  .setBucketName("bucket")
                                  .setFolderPath("path")
                                  .setRegion("us-east-1")
                                  .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoChart))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ChartName can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForHelmGcs() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcsHelmPayload(GcsHelmPayload.newBuilder()
                                   .setHelmVersion(V2)
                                   .setChartName("chart")
                                   .setBucketName("bucket")
                                   .setFolderPath("path")
                                   .build())
            .build());

    validatePollingItemForManifest(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setGcsHelmPayload(GcsHelmPayload.newBuilder()
                                   .setHelmVersion(V2)
                                   .setChartName("chart")
                                   .setBucketName("bucket")
                                   .setFolderPath("path")
                                   .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoChart = generatePollingItem(io.harness.polling.contracts.Category.MANIFEST,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcsHelmPayload(
                GcsHelmPayload.newBuilder().setHelmVersion(V2).setBucketName("bucket").setFolderPath("path").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForHelmChart(pollingItemNoChart))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ChartName can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testVerifyStageAndBuildRefForArtifact() {
    ArtifactTriggerConfig ecr =
        ArtifactTriggerConfig.builder().type(ArtifactType.ECR).spec(EcrSpec.builder().build()).build();
    ecr.setStageIdentifier("dev");
    ecr.setArtifactRef("primary");
    NGTriggerConfigV2 ngTriggerConfigV2 =
        NGTriggerConfigV2.builder().source(NGTriggerSourceV2.builder().type(ARTIFACT).spec(ecr).build()).build();

    try {
      buildTriggerHelper.verifyStageAndBuildRef(
          TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef");
    } catch (Exception e) {
      fail("Exception was n ot expected");
    }

    ecr.setStageIdentifier(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. ");

    ecr.setArtifactRef(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. artifactRef can not be blank/missing. ");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testErrorMapGeneration() {
    Map<String, InputSetErrorResponseDTOPMS> map = new HashMap<>();
    map.put("path.abc",
        InputSetErrorResponseDTOPMS.builder()
            .errors(Arrays.asList(InputSetErrorDTOPMS.builder()
                                      .identifierOfErrorSource("identifierOfErrorSource")
                                      .message("message1")
                                      .fieldName("fieldName1")
                                      .build()))
            .build());

    Map<String, Map<String, String>> expectedErrorMap = new HashMap<>();
    Map<String, String> fields = new HashMap<>();
    fields.put("fieldName", "fieldName1");
    fields.put("message", "message1");
    expectedErrorMap.put("path.abc", fields);
    assertThat(
        buildTriggerHelper.generateErrorMap(InputSetErrorWrapperDTOPMS.builder().uuidToErrorResponseMap(map).build()))
        .isEqualTo(expectedErrorMap);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testVerifyStageAndBuildRefForManifest() {
    ManifestTriggerConfig manifestTriggerConfig = ManifestTriggerConfig.builder()
                                                      .type(HELM_MANIFEST)
                                                      .spec(HelmManifestSpec.builder().chartName("chart").build())
                                                      .build();

    manifestTriggerConfig.setStageIdentifier("dev");
    manifestTriggerConfig.setManifestRef("man1");
    NGTriggerConfigV2 ngTriggerConfigV2 =
        NGTriggerConfigV2.builder()
            .source(NGTriggerSourceV2.builder().type(MANIFEST).spec(manifestTriggerConfig).build())
            .build();

    try {
      buildTriggerHelper.verifyStageAndBuildRef(
          TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "manifestRef");
    } catch (Exception e) {
      fail("Exception was n ot expected");
    }

    manifestTriggerConfig.setStageIdentifier(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "manifestRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. ");

    manifestTriggerConfig.setManifestRef(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. artifactRef can not be blank/missing. ");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Acr() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setAcrPayload(AcrPayload.newBuilder()
                               .setSubscriptionId("test-subscriptionId")
                               .setRegistry("test-registry")
                               .setRepository("test-repository")
                               .build())
            .build());

    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setAcrPayload(AcrPayload.newBuilder()
                               .setSubscriptionId("test-subscriptionId")
                               .setRegistry("test-registry")
                               .setRepository("test-repository")
                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoSubscription = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setAcrPayload(
                AcrPayload.newBuilder().setRegistry("test-registry").setRepository("test-repository").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoSubscription))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("subscriptionId can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRegistry = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setAcrPayload(AcrPayload.newBuilder()
                               .setSubscriptionId("test-subscriptionId")
                               .setRepository("test-repository")
                               .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRegistry))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("registry can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRepository = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setAcrPayload(
                AcrPayload.newBuilder().setSubscriptionId("test-subscriptionId").setRegistry("test-registry").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepository))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repository can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Nexus3Registry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepository("repo")
                                          .setRepositoryFormat("docker")
                                          .setArtifactId("nexus")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setRepositoryPort("8080")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());
    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItemNoRepo = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("docker")
                                          .setArtifactId("nexus")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setRepositoryPort("8080")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepo))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repository can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRepositoryFormat =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                              .setRepository("repo")
                                              .setArtifactId("nexus")
                                              .setArtifactPath("path")
                                              .setRepositoryUrl("http://docker.com")
                                              .setRepositoryPort("8080")
                                              .setGroupId("groupId")
                                              .setPackageName("packageName")
                                              .build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepositoryFormat))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repositoryFormat not supported");
    final PollingItem pollingItemNoURL = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("docker")
                                          .setRepository("repo")
                                          .setArtifactId("nexus")
                                          .setArtifactPath("path")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoURL))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repositoryUrl can not be blank. Needs to have concrete value"
            + " \n "
            + "repositoryPort can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoPath = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("docker")
                                          .setRepository("repo")
                                          .setArtifactId("nexus")
                                          .setRepositoryUrl("http://docker.com")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoPath))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("artifactPath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoGroupId = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("maven")
                                          .setRepository("repo")
                                          .setArtifactId("nexus")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoGroupId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("groupId can not be blank. Needs to have concrete value");
    final PollingItem pollingItemNoArtifactId = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("maven")
                                          .setRepository("repo")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoArtifactId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("artifactId can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoPackageName = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("nuget")
                                          .setRepository("repo")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setGroupId("groupId")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoPackageName))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("packageName can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoPackageNameNPM = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("npm")
                                          .setRepository("repo")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setGroupId("groupId")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoPackageNameNPM))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("packageName can not be blank. Needs to have concrete value");
    final PollingItem pollingItemNoGroup = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus3RegistryPayload(Nexus3RegistryPayload.newBuilder()
                                          .setRepositoryFormat("raw")
                                          .setRepository("repo")
                                          .setArtifactPath("path")
                                          .setRepositoryUrl("http://docker.com")
                                          .setGroupId("groupId")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoGroup))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("group can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Nexus2Registry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepository("repo")
                                          .setRepositoryFormat("maven")
                                          .setArtifactId("nexus")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());
    validatePollingItemForArtifact(pollingItem);

    PollingItem pollingItemNoRepo = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepositoryFormat("maven")
                                          .setArtifactId("nexus")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepo))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repository can not be blank. Needs to have concrete value");

    PollingItem pollingItemNoRepositoryFormat = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepository("repo")
                                          .setArtifactId("nexus")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRepositoryFormat))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("repositoryFormat not supported");

    final PollingItem pollingItemNoGroupId = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepositoryFormat("maven")
                                          .setRepository("repo")
                                          .setArtifactId("nexus")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoGroupId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("groupId can not be blank. Needs to have concrete value");
    final PollingItem pollingItemNoArtifactId = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepositoryFormat("maven")
                                          .setRepository("repo")
                                          .setGroupId("groupId")
                                          .setPackageName("packageName")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoArtifactId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("artifactId can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoPackageName = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepositoryFormat("nuget")
                                          .setRepository("repo")
                                          .setGroupId("groupId")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoPackageName))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("packageName can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoPackageNameNPM = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setNexus2RegistryPayload(Nexus2RegistryPayload.newBuilder()
                                          .setRepositoryFormat("npm")
                                          .setRepository("repo")
                                          .setGroupId("groupId")
                                          .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoPackageNameNPM))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("packageName can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = SHIVAM)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Bamboo() {
    final PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setBambooPayload(BambooPayload.newBuilder()
                                  .setPlanKey("plan")
                                  .addAllArtifactPath(mapToArtifactPathList(Collections.singletonList("Path")))
                                  .build())
            .build());
    validatePollingItemForArtifact(pollingItem);

    final PollingItem pollingItem2 = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setBambooPayload(BambooPayload.newBuilder()
                                  .addAllArtifactPath(mapToArtifactPathList(Collections.singletonList("Path")))
                                  .build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItem2))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("planKey can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = VINICIUS)
  @Category(UnitTests.class)
  public void testGenerateBuildTriggerOpsDataForMultiRegionArtifact() throws Exception {
    when(pmsFeatureFlagHelper.isEnabled("account", FeatureName.CDS_NG_TRIGGER_MULTI_ARTIFACTS)).thenReturn(true);
    ClassLoader classLoader = getClass().getClassLoader();
    String multiRegionArtifactTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource("ng-trigger-multi-region-artifact.yaml")),
            StandardCharsets.UTF_8);
    TriggerDetails triggerDetails =
        ngTriggerElementMapper.toTriggerDetails("account", "org", "proj", multiRegionArtifactTriggerYaml, true);
    List<BuildTriggerOpsData> buildTriggerOpsData =
        buildTriggerHelper.generateBuildTriggerOpsDataForMultiArtifact(triggerDetails);
    assertThat(buildTriggerOpsData.size()).isEqualTo(2);
    // assertions for first item of polling data
    assertThat(buildTriggerOpsData.get(0).getPipelineBuildSpecMap().isEmpty()).isTrue();
    assertThat(buildTriggerOpsData.get(0).getTriggerDetails()).isEqualToComparingFieldByField(triggerDetails);
    assertThat(buildTriggerOpsData.get(0).getBuildMetadata())
        .isEqualToComparingFieldByField(
            triggerDetails.getNgTriggerEntity().getMetadata().getMultiBuildMetadata().get(0));
    assertThat(buildTriggerOpsData.get(0).getSignaturesToLock().size()).isEqualTo(1);
    assertThat(buildTriggerOpsData.get(0).getSignaturesToLock().contains(triggerDetails.getNgTriggerEntity()
                                                                             .getMetadata()
                                                                             .getMultiBuildMetadata()
                                                                             .get(1)
                                                                             .getPollingConfig()
                                                                             .getSignature()))
        .isTrue();
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(0), "type"))
        .isEqualTo("DockerRegistry");
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(0), "spec.connectorRef"))
        .isEqualTo("DockerConnectorUs");
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(0), "spec.imagePath"))
        .isEqualTo("v2/hello-world-us");
    // assertions for second item of polling data
    assertThat(buildTriggerOpsData.get(1).getPipelineBuildSpecMap().isEmpty()).isTrue();
    assertThat(buildTriggerOpsData.get(1).getTriggerDetails()).isEqualToComparingFieldByField(triggerDetails);
    assertThat(buildTriggerOpsData.get(1).getBuildMetadata())
        .isEqualToComparingFieldByField(
            triggerDetails.getNgTriggerEntity().getMetadata().getMultiBuildMetadata().get(1));
    assertThat(buildTriggerOpsData.get(1).getSignaturesToLock().size()).isEqualTo(1);
    assertThat(buildTriggerOpsData.get(1).getSignaturesToLock().contains(triggerDetails.getNgTriggerEntity()
                                                                             .getMetadata()
                                                                             .getMultiBuildMetadata()
                                                                             .get(0)
                                                                             .getPollingConfig()
                                                                             .getSignature()));
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(1), "type"))
        .isEqualTo("DockerRegistry");
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(1), "spec.connectorRef"))
        .isEqualTo("DockerConnectorApac");
    assertThat(buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData.get(1), "spec.imagePath"))
        .isEqualTo("v2/hello-world-apac");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testGeneratePollingDescriptor() {
    PollingResponse pollingResponse =
        PollingResponse.newBuilder()
            .setAccountId("AccountId")
            .addSignatures("Signature")
            .setBuildInfo(BuildInfo.newBuilder().setName("buildName").addAllVersions(Arrays.asList("v1", "v2")).build())
            .build();

    String pollingDescriptor = buildTriggerHelper.generatePollingDescriptor(pollingResponse);

    assertThat(pollingDescriptor).isNotEmpty();
    assertThat(pollingDescriptor.equals(
                   "AccountId: AccountId, Signatures: [Signature  ], , BuildInfo Name: buildName, Version: [v1  v2  ]"))
        .isTrue();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testFetchPipelineYamlForTrigger() throws IOException {
    Call pipelineCall = mock(Call.class);
    when(pipelineServiceClient.getPipelineByIdentifier(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(pipelineCall);
    when(pipelineCall.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(PMSPipelineResponseDTO.builder().build())));

    PMSPipelineResponseDTO pmsPipelineResponseDTO =
        buildTriggerHelper.fetchPipelineForTrigger(TriggerDetails.builder()
                                                       .ngTriggerEntity(NGTriggerEntity.builder().build())
                                                       .ngTriggerConfigV2(NGTriggerConfigV2.builder().build())
                                                       .build());
    assertThat(pmsPipelineResponseDTO).isNotNull();
  }

  private void validatePollingItemForArtifact(PollingItem pollingItem) {
    try {
      buildTriggerHelper.validatePollingItemForArtifact(pollingItem);
    } catch (Exception e) {
      fail("Exception wasnt expected");
    }
  }

  private void validatePollingItemForManifest(PollingItem pollingItem) {
    try {
      buildTriggerHelper.validatePollingItemForHelmChart(pollingItem);
    } catch (Exception e) {
      fail("Exception wasnt expected");
    }
  }

  PollingItem generatePollingItem(
      io.harness.polling.contracts.Category category, PollingPayloadData pollingPayloadData) {
    return PollingItem.newBuilder()
        .setCategory(category)
        .setQualifier(Qualifier.newBuilder().setProjectId("proj").setOrganizationId("org").setAccountId("acc").build())
        .setSignature("sig1")
        .setPollingPayloadData(pollingPayloadData)
        .build();
  }

  public List<ArtifactPathList> mapToArtifactPathList(List<String> variables) {
    List<ArtifactPathList> inputs = new ArrayList<>();
    for (String variable : variables) {
      inputs.add(ArtifactPathList.newBuilder().setArtifactPath(variable).build());
    }
    return inputs;
  }
}
