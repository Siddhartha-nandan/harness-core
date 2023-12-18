/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.remediation_tracker;

import static io.harness.rule.OwnerRule.VARSHA_LALWANI;
import static io.harness.ssca.entities.remediation_tracker.RemediationStatus.COMPLETED;
import static io.harness.ssca.entities.remediation_tracker.RemediationStatus.ON_GOING;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.repositories.remediation_tracker.RemediationTrackerRepository;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.RemediationCondition;
import io.harness.spec.server.ssca.v1.model.RemediationStatus;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackersOverallSummaryResponseBody;
import io.harness.spec.server.ssca.v1.model.VulnerabilitySeverity;
import io.harness.ssca.beans.EnvType;
import io.harness.ssca.beans.remediation_tracker.PatchedPendingArtifactEntitiesResult;
import io.harness.ssca.entities.remediation_tracker.CVEVulnerability;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.CdInstanceSummaryService;
import io.harness.ssca.services.NormalisedSbomComponentService;

import com.google.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RemediationTrackerServiceImplTest extends SSCAManagerTestBase {
  @Inject RemediationTrackerService remediationTrackerService;

  @Mock ArtifactService artifactService;

  @Mock CdInstanceSummaryService cdInstanceSummaryService;

  @Mock NormalisedSbomComponentService normalisedSbomComponentService;

  private BuilderFactory builderFactory;

  private RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody;

  @Inject RemediationTrackerRepository repository;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    remediationTrackerCreateRequestBody = builderFactory.getRemediationTrackerCreateRequestBody();
    when(normalisedSbomComponentService.getOrchestrationIds(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    when(artifactService.getDistinctArtifactIds(any(), any(), any(), any()))
        .thenReturn(Set.of("artifactId1", "artifactId2"));
    when(artifactService.listDeployedArtifactsFromIdsWithCriteria(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(builderFactory.getPatchedPendingArtifactEntitiesResult()));
    when(cdInstanceSummaryService.getCdInstanceSummaries(any(), any(), any(), any()))
        .thenReturn(List.of(
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTracker() {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    assertThat(remediationTrackerEntity).isNotNull();
    assertThat(remediationTrackerEntity.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(remediationTrackerEntity.getOrgIdentifier()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(remediationTrackerEntity.getProjectIdentifier())
        .isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(remediationTrackerEntity.getCondition()).isNotNull();
    assertThat(remediationTrackerEntity.getCondition().getOperator())
        .isEqualTo(io.harness.ssca.entities.remediation_tracker.RemediationCondition.Operator.ALL);
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getType())
        .isEqualTo(io.harness.ssca.entities.remediation_tracker.VulnerabilityInfoType.DEFAULT);
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getComponent()).isEqualTo("log4j");
    assertThat(remediationTrackerEntity.getComments()).isEqualTo("test");
    assertThat(remediationTrackerEntity.getArtifactInfos().size()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactId()).isEqualTo("artifactId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactName()).isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().size()).isEqualTo(2);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(0).getEnvIdentifier())
        .isEqualTo("env1");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(0).isPatched())
        .isTrue();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(1).getEnvIdentifier())
        .isEqualTo("envId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().get(1).isPatched())
        .isFalse();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPatchedProdCount())
        .isEqualTo(1);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPendingProdCount())
        .isEqualTo(1);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPatchedNonProdCount())
        .isZero();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPendingNonProdCount())
        .isZero();
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedProdCount()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingProdCount()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTrackerWithCVE() {
    io.harness.spec.server.ssca.v1.model.CVEVulnerability cveVulnerability =
        new io.harness.spec.server.ssca.v1.model.CVEVulnerability();
    cveVulnerability.setType("CVE");
    cveVulnerability.setCve("CVE-2021-44228");
    cveVulnerability.setComponentName("log4j");
    cveVulnerability.setComponentVersion("1.2.3");
    cveVulnerability.setSeverity(VulnerabilitySeverity.HIGH);
    remediationTrackerCreateRequestBody.setVulnerabilityInfo(cveVulnerability);
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    assertThat(remediationTrackerEntity).isNotNull();
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getType())
        .isEqualTo(io.harness.ssca.entities.remediation_tracker.VulnerabilityInfoType.CVE);
    assertThat(((CVEVulnerability) remediationTrackerEntity.getVulnerabilityInfo()).getCve())
        .isEqualTo("CVE-2021-44228");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTracker_withFailedValidation_ForVersion() {
    remediationTrackerCreateRequestBody.setRemediationCondition(
        new RemediationCondition().operator(RemediationCondition.OperatorEnum.LESSTHAN).version("tag1"));
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> remediationTrackerService.createRemediationTracker(
                            builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody))
        .withMessage(
            "Unsupported Version Format. Semantic Versioning is required for LessThan and LessThanEquals operator.");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testUpdateArtifactsAndEnvironmentsInRemediationTracker() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    updatedArtifactsAndCdInstanceSummaries();

    remediationTrackerService.updateArtifactsAndEnvironments(remediationTrackerEntity);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getArtifactInfos()).isNotNull();
    assertThat(remediationTrackerEntity.getArtifactInfos().size()).isEqualTo(2);
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactId()).isEqualTo("artifactId");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getArtifactName()).isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").getEnvironments().size()).isEqualTo(6);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPatchedProdCount())
        .isEqualTo(3);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPendingProdCount())
        .isEqualTo(2);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPatchedNonProdCount())
        .isEqualTo(1);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId").getDeploymentsCount().getPendingNonProdCount())
        .isZero();

    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getArtifactId()).isEqualTo("artifactId1");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getArtifactName())
        .isEqualTo("test/image");
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId1").getEnvironments().size()).isEqualTo(1);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId1").getDeploymentsCount().getPatchedProdCount())
        .isZero();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId1").getDeploymentsCount().getPendingProdCount())
        .isEqualTo(1);
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId1").getDeploymentsCount().getPatchedNonProdCount())
        .isZero();
    assertThat(
        remediationTrackerEntity.getArtifactInfos().get("artifactId1").getDeploymentsCount().getPendingNonProdCount())
        .isZero();

    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedProdCount()).isEqualTo(3);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedNonProdCount()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingProdCount()).isEqualTo(3);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testDeploymentCounts_WithExcludedArtifacts() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    remediationTrackerEntity.getArtifactInfos().get("artifactId").setExcluded(true);

    updatedArtifactsAndCdInstanceSummaries();

    remediationTrackerService.updateArtifactsAndEnvironments(remediationTrackerEntity);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getArtifactInfos()).isNotNull();
    assertThat(remediationTrackerEntity.getArtifactInfos().size()).isEqualTo(2);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedProdCount()).isZero();
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingProdCount()).isEqualTo(1);
    assertThat(remediationTrackerEntity.getDeploymentsCount().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testAutoCloseRemediationTracker() {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    assertThat(remediationTrackerEntity.getStatus()).isEqualTo(COMPLETED);
    assertThat(remediationTrackerEntity.getEndTimeMilli()).isNotNull();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetOverallSummary() {
    // Creating two completed remediation tracker and one on-going remediation tracker
    RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody =
        builderFactory.getRemediationTrackerCreateRequestBody();
    String remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody);
    RemediationTrackerEntity remediationTrackerEntity =
        remediationTrackerService.getRemediationTracker(remediationTrackerId);
    remediationTrackerEntity.setStatus(COMPLETED);
    remediationTrackerEntity.setStartTimeMilli(builderFactory.getClock().millis());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(1, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerId);
    remediationTrackerEntity.setStatus(COMPLETED);
    remediationTrackerEntity.setStartTimeMilli(
        builderFactory.getClock().instant().plus(5, ChronoUnit.MINUTES).toEpochMilli());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(2, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerCreateRequestBody);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerId);
    remediationTrackerEntity.setStatus(ON_GOING);
    repository.save(remediationTrackerEntity);

    RemediationTrackersOverallSummaryResponseBody response =
        remediationTrackerService.getOverallSummaryForRemediationTrackers(builderFactory.getContext().getAccountId(),
            builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier());
    assertThat(response).isNotNull();
    assertThat(response.getMeanTimeToRemediateInHours()).isEqualTo(35.96);
    assertThat(response.getRemediationCounts()).hasSize(2);
    assertThat(response.getRemediationCounts().get(0).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.getRemediationCounts().get(0).getCount()).isEqualTo(2);
    assertThat(response.getRemediationCounts().get(1).getStatus()).isEqualTo(RemediationStatus.ON_GOING);
    assertThat(response.getRemediationCounts().get(1).getCount()).isEqualTo(1);
  }

  private RemediationTrackerEntity getRemediationTrackerEntity(RemediationTrackerCreateRequestBody requestBody) {
    String remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), requestBody);
    assertThat(remediationTrackerId).isNotNull();
    return remediationTrackerService.getRemediationTracker(remediationTrackerId);
  }

  private void updatedArtifactsAndCdInstanceSummaries() throws IllegalAccessException {
    when(artifactService.listDeployedArtifactsFromIdsWithCriteria(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(
            PatchedPendingArtifactEntitiesResult.builder()
                .patchedArtifacts(
                    List.of(builderFactory.getArtifactEntityBuilder().artifactCorrelationId("patched").build(),
                        builderFactory.getArtifactEntityBuilder().artifactCorrelationId("patched1").build()))
                .pendingArtifacts(
                    List.of(builderFactory.getArtifactEntityBuilder().artifactCorrelationId("pending").build(),
                        builderFactory.getArtifactEntityBuilder()
                            .artifactCorrelationId("pending1")
                            .artifactId("artifactId1")
                            .build()))
                .build()));
    when(cdInstanceSummaryService.getCdInstanceSummaries(any(), any(), any(), any()))
        .thenReturn(List.of(builderFactory.getCdInstanceSummaryBuilder()
                                .artifactCorrelationId("patched")
                                .envIdentifier("env2")
                                .envType(EnvType.PreProduction)
                                .build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env3")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env1")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("pending1")
                .envIdentifier("env2")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
  }
}
