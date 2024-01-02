/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.remediation_tracker;

import static io.harness.rule.OwnerRule.HUMANSHU_ARORA;
import static io.harness.rule.OwnerRule.VARSHA_LALWANI;
import static io.harness.rule.TestUserProvider.testUserProvider;
import static io.harness.ssca.entities.remediation_tracker.RemediationStatus.COMPLETED;
import static io.harness.ssca.entities.remediation_tracker.RemediationStatus.ON_GOING;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.user.UserInfo;
import io.harness.repositories.remediation_tracker.RemediationTrackerRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.CreateTicketRequest;
import io.harness.spec.server.ssca.v1.model.EnvironmentInfo;
import io.harness.spec.server.ssca.v1.model.EnvironmentType;
import io.harness.spec.server.ssca.v1.model.EnvironmentTypeFilter;
import io.harness.spec.server.ssca.v1.model.ExcludeArtifactRequest;
import io.harness.spec.server.ssca.v1.model.NameOperator;
import io.harness.spec.server.ssca.v1.model.PipelineInfo;
import io.harness.spec.server.ssca.v1.model.RemediationArtifactDeploymentsListingRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationArtifactDeploymentsListingResponse;
import io.harness.spec.server.ssca.v1.model.RemediationArtifactDetailsResponse;
import io.harness.spec.server.ssca.v1.model.RemediationArtifactListingRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationArtifactListingResponse;
import io.harness.spec.server.ssca.v1.model.RemediationCondition;
import io.harness.spec.server.ssca.v1.model.RemediationDetailsResponse;
import io.harness.spec.server.ssca.v1.model.RemediationListingRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationListingRequestBodyComponentNameFilter;
import io.harness.spec.server.ssca.v1.model.RemediationListingRequestBodyCveFilter;
import io.harness.spec.server.ssca.v1.model.RemediationListingResponse;
import io.harness.spec.server.ssca.v1.model.RemediationStatus;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerUpdateRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackersOverallSummaryResponseBody;
import io.harness.spec.server.ssca.v1.model.VulnerabilitySeverity;
import io.harness.ssca.api.ArtifactApiUtils;
import io.harness.ssca.beans.EnvType;
import io.harness.ssca.beans.remediation_tracker.PatchedPendingArtifactEntitiesResult;
import io.harness.ssca.entities.remediation_tracker.CVEVulnerability;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.entities.remediation_tracker.VulnerabilityInfoType;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.CdInstanceSummaryService;
import io.harness.ssca.services.NormalisedSbomComponentService;
import io.harness.ssca.ticket.TicketServiceRestClientService;
import io.harness.ssca.utils.PageResponseUtils;
import io.harness.ssca.utils.PipelineUtils;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import retrofit2.Call;
import retrofit2.Response;

public class RemediationTrackerServiceImplTest extends SSCAManagerTestBase {
  @Inject RemediationTrackerService remediationTrackerService;

  @Mock ArtifactService artifactService;

  @Mock CdInstanceSummaryService cdInstanceSummaryService;

  @Mock NormalisedSbomComponentService normalisedSbomComponentService;

  @Mock TicketServiceRestClientService ticketServiceRestClientService;

  @Mock private UserClient userClient;

  @Mock private PipelineUtils pipelineUtils;

  private BuilderFactory builderFactory;

  private RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody;

  @Inject RemediationTrackerRepository repository;

  @Before
  public void setup() throws IllegalAccessException, IOException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    remediationTrackerCreateRequestBody = builderFactory.getRemediationTrackerCreateRequestBody();
    UserInfo user = UserInfo.builder().email("EMAIL").name("NAME").build();
    Call userCall = mock(Call.class);
    when(userClient.getUserById(any())).thenReturn(userCall);
    when(userCall.execute()).thenReturn(Response.success(new RestResponse(Optional.of(user))));
    when(normalisedSbomComponentService.getOrchestrationIds(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    when(artifactService.getDistinctArtifactIds(any(), any(), any(), any()))
        .thenReturn(Set.of("artifactId1", "artifactId2"));
    when(artifactService.getLatestArtifact(any(), any(), any(), any()))
        .thenReturn(builderFactory.getArtifactEntityBuilder().build());
    when(pipelineUtils.getPipelineInfo(any(), any(), any(), any()))
        .thenReturn(new PipelineInfo().id("pipelineId").executionId("executionId").name("name"));
    when(artifactService.listDeployedArtifactsFromIdsWithCriteria(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(builderFactory.getPatchedPendingArtifactEntitiesResult()));
    when(cdInstanceSummaryService.getCdInstanceSummaries(any(), any(), any(), any()))
        .thenReturn(List.of(
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("patched").envIdentifier("env1").build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "userClient", userClient, true);
    when(ticketServiceRestClientService.createTicket(any(), any(), any(), any(), any()))
        .thenReturn(builderFactory.getTicketResponseDto());
    FieldUtils.writeField(
        remediationTrackerService, "ticketServiceRestClientService", ticketServiceRestClientService, true);
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
    FieldUtils.writeField(remediationTrackerService, "pipelineUtils", pipelineUtils, true);
    testUserProvider.setActiveUser(EmbeddedUser.builder().uuid("UUID").name("user1").email("user1@harness.io").build());
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCreateRemediationTracker() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

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
    cveVulnerability.setSeverity(VulnerabilitySeverity.HIGH);
    remediationTrackerCreateRequestBody.setVulnerabilityInfo(cveVulnerability);
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

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
  public void testUpdate_OnGoingRemediation() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(ON_GOING);
    repository.save(remediationTrackerEntity);
    RemediationTrackerUpdateRequestBody remediationTrackerUpdateRequestBody =
        new RemediationTrackerUpdateRequestBody()
            .comments("test1")
            .contact(new io.harness.spec.server.ssca.v1.model.ContactInfo().name("test1").email("test1@gmail.com"))
            .vulnerabilityDescription("description1")
            .targetEndDate(LocalDate.of(2021, 5, 22))
            .severity(VulnerabilitySeverity.LOW);
    remediationTrackerService.updateRemediationTracker(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        remediationTrackerEntity.getUuid(), remediationTrackerUpdateRequestBody);
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getComments()).isEqualTo("test1");
    assertThat(remediationTrackerEntity.getContactInfo().getName()).isEqualTo("test1");
    assertThat(remediationTrackerEntity.getContactInfo().getEmail()).isEqualTo("test1@gmail.com");
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getVulnerabilityDescription()).isEqualTo("description1");
    assertThat(remediationTrackerEntity.getVulnerabilityInfo().getSeverity().toString())
        .isEqualTo(VulnerabilitySeverity.LOW.toString());
    assertThat(remediationTrackerEntity.getTargetEndDateEpochDay()).isEqualTo(LocalDate.of(2021, 5, 22).toEpochDay());
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testUpdate_CompletedRemediation() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(COMPLETED);
    repository.save(remediationTrackerEntity);
    RemediationTrackerUpdateRequestBody remediationTrackerUpdateRequestBody =
        new RemediationTrackerUpdateRequestBody().comments("test1");
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(
            ()
                -> remediationTrackerService.updateRemediationTracker(builderFactory.getContext().getAccountId(),
                    builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                    remediationTrackerEntity.getUuid(), remediationTrackerUpdateRequestBody))
        .withMessage(String.format("Remediation Tracker: %s is already closed.", remediationTrackerEntity.getUuid()));
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testUpdateArtifactsAndEnvironmentsInRemediationTracker() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

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
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

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
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    assertThat(remediationTrackerEntity.getStatus()).isEqualTo(COMPLETED);
    assertThat(remediationTrackerEntity.getEndTimeMilli()).isNotNull();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetOverallSummary() {
    // Creating two completed remediation tracker and one on-going remediation tracker
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(COMPLETED);
    remediationTrackerEntity.setStartTimeMilli(builderFactory.getClock().millis());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(1, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerEntity = createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(COMPLETED);
    remediationTrackerEntity.setStartTimeMilli(
        builderFactory.getClock().instant().plus(5, ChronoUnit.MINUTES).toEpochMilli());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(2, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerEntity = createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

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
                                .envName("env2")
                                .envType(EnvType.PreProduction)
                                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched")
                .envIdentifier("env1")
                .envName("env1")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env3")
                .envName("env3")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("patched1")
                .envIdentifier("env1")
                .envName("env1")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("pending1")
                .envIdentifier("env2")
                .envName("env2")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder()
                .artifactCorrelationId("pending")
                .envIdentifier("env1")
                .envName("env1")
                .build(),
            builderFactory.getCdInstanceSummaryBuilder().artifactCorrelationId("pending").build()));
    FieldUtils.writeField(remediationTrackerService, "artifactService", artifactService, true);
    FieldUtils.writeField(remediationTrackerService, "cdInstanceSummaryService", cdInstanceSummaryService, true);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testCloseRemediationTracker() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(ON_GOING);
    repository.save(remediationTrackerEntity);

    remediationTrackerService.close(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        remediationTrackerEntity.getUuid());
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getStatus()).isEqualTo(COMPLETED);
    assertThat(remediationTrackerEntity.getEndTimeMilli()).isNotNull();
    assertThat(remediationTrackerEntity.isClosedManually()).isTrue();
    assertThat(remediationTrackerEntity.getClosedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testExcludeArtifact() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(ON_GOING);
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").isExcluded()).isFalse();
    repository.save(remediationTrackerEntity);

    remediationTrackerService.excludeArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        remediationTrackerEntity.getUuid(), new ExcludeArtifactRequest().artifactId("artifactId"));
    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());
    assertThat(remediationTrackerEntity.getArtifactInfos().get("artifactId").isExcluded()).isTrue();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediations() {
    createRemediations();

    Pageable pageable = PageResponseUtils.getPageable(0, 3, ArtifactApiUtils.getSortFieldMapping("component"), "ASC");
    List<RemediationListingResponse> response =
        remediationTrackerService
            .listRemediations(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                new RemediationListingRequestBody(), pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(3);
    assertThat(response.get(0).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(0).getComponent()).isEqualTo("remediation1");
    assertThat(response.get(0).getRemediationCondition().getOperator())
        .isEqualTo(RemediationCondition.OperatorEnum.ALL);
    assertThat(response.get(0).getScheduleStatus()).isEqualTo("On time");
    assertThat(response.get(0).getCve()).isNull();
    assertThat(response.get(0).getSeverity()).isEqualTo(VulnerabilitySeverity.HIGH);
    assertThat(response.get(0).getContact().getName()).isEqualTo("test");
    assertThat(response.get(0).getContact().getEmail()).isEqualTo("test@gmail.com");
    assertThat(response.get(0).getDeploymentsCount().getPatchedProdCount()).isEqualTo(1);
    assertThat(response.get(0).getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(response.get(0).getDeploymentsCount().getPendingProdCount()).isEqualTo(1);
    assertThat(response.get(0).getDeploymentsCount().getPendingNonProdCount()).isZero();

    assertThat(response.get(1).getCve()).isNotNull();
    assertThat(response.get(1).getCve()).isEqualTo("CVE-2021-44228");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationsWithBothFilters() {
    createRemediations();

    Pageable pageable = PageResponseUtils.getPageable(0, 3, ArtifactApiUtils.getSortFieldMapping("component"), "ASC");
    List<RemediationListingResponse> response =
        remediationTrackerService
            .listRemediations(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                new RemediationListingRequestBody()
                    .cveFilter(
                        new RemediationListingRequestBodyCveFilter().value("CVE-").operator(NameOperator.CONTAINS))
                    .componentNameFilter(new RemediationListingRequestBodyComponentNameFilter()
                                             .value("remediation2")
                                             .operator(NameOperator.EQUALS)),
                pageable)
            .getContent();
    assertThat(response).isNotNull();
    assertThat(response).hasSize(1);
    assertThat(response.get(0).getComponent()).isEqualTo("remediation2");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationsWithOneFilter() {
    createRemediations();

    Pageable pageable = PageResponseUtils.getPageable(0, 3, ArtifactApiUtils.getSortFieldMapping("component"), "ASC");
    List<RemediationListingResponse> response =
        remediationTrackerService
            .listRemediations(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                new RemediationListingRequestBody().cveFilter(
                    new RemediationListingRequestBodyCveFilter().value("CVE-").operator(NameOperator.CONTAINS)),
                pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(2);
    assertThat(response.get(0).getComponent()).isEqualTo("remediation2");
    assertThat(response.get(1).getComponent()).isEqualTo("remediation3");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationsArtifacts() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    updatedArtifactsAndCdInstanceSummaries();
    Pageable pageable = PageResponseUtils.getPageable(0, 3);
    List<RemediationArtifactListingResponse> response =
        remediationTrackerService
            .listRemediationArtifacts(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(), new RemediationArtifactListingRequestBody(), pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(2);

    assertThat(response.get(0).getId()).isEqualTo("artifactId1");
    assertThat(response.get(0).getName()).isEqualTo("test/image");
    assertThat(response.get(0).getTicket()).isNull();
    assertThat(response.get(0).getDeployments().getPatchedProdCount()).isZero();
    assertThat(response.get(0).getDeployments().getPatchedNonProdCount()).isZero();
    assertThat(response.get(0).getDeployments().getPendingProdCount()).isEqualTo(1);
    assertThat(response.get(0).getDeployments().getPendingNonProdCount()).isZero();

    assertThat(response.get(1).getId()).isEqualTo("artifactId");
    assertThat(response.get(1).getName()).isEqualTo("test/image");
    assertThat(response.get(1).getTicket()).isNull();
    assertThat(response.get(1).getDeployments().getPatchedProdCount()).isEqualTo(3);
    assertThat(response.get(1).getDeployments().getPatchedNonProdCount()).isEqualTo(1);
    assertThat(response.get(1).getDeployments().getPendingProdCount()).isEqualTo(2);
    assertThat(response.get(1).getDeployments().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationsArtifacts_WithPendingRemediationStatus() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    updatedArtifactsAndCdInstanceSummaries();

    Pageable pageable = PageResponseUtils.getPageable(0, 3);
    List<RemediationArtifactListingResponse> response =
        remediationTrackerService
            .listRemediationArtifacts(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(),
                new RemediationArtifactListingRequestBody().remediationStatus(
                    RemediationArtifactListingRequestBody.RemediationStatusEnum.PENDING),
                pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(2);

    assertThat(response.get(0).getId()).isEqualTo("artifactId1");
    assertThat(response.get(0).getName()).isEqualTo("test/image");
    assertThat(response.get(0).getTicket()).isNull();
    assertThat(response.get(0).getDeployments().getPatchedProdCount()).isZero();
    assertThat(response.get(0).getDeployments().getPatchedNonProdCount()).isZero();
    assertThat(response.get(0).getDeployments().getPendingProdCount()).isEqualTo(1);
    assertThat(response.get(0).getDeployments().getPendingNonProdCount()).isZero();

    assertThat(response.get(1).getId()).isEqualTo("artifactId");
    assertThat(response.get(1).getName()).isEqualTo("test/image");
    assertThat(response.get(1).getTicket()).isNull();
    assertThat(response.get(1).getDeployments().getPatchedProdCount()).isEqualTo(3);
    assertThat(response.get(1).getDeployments().getPatchedNonProdCount()).isEqualTo(1);
    assertThat(response.get(1).getDeployments().getPendingProdCount()).isEqualTo(2);
    assertThat(response.get(1).getDeployments().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationsArtifacts_WithCompletedRemediationStatus_AndDeploymentPreProd()
      throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    updatedArtifactsAndCdInstanceSummaries();
    Pageable pageable = PageResponseUtils.getPageable(0, 3);
    List<RemediationArtifactListingResponse> response =
        remediationTrackerService
            .listRemediationArtifacts(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(),
                new RemediationArtifactListingRequestBody()
                    .deploymentStatus(RemediationArtifactListingRequestBody.DeploymentStatusEnum.PREPROD)
                    .remediationStatus(RemediationArtifactListingRequestBody.RemediationStatusEnum.COMPLETED),
                pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(1);

    assertThat(response.get(0).getId()).isEqualTo("artifactId1");
    assertThat(response.get(0).getName()).isEqualTo("test/image");
    assertThat(response.get(0).getTicket()).isNull();
    assertThat(response.get(0).getDeployments().getPatchedProdCount()).isNull();
    assertThat(response.get(0).getDeployments().getPatchedNonProdCount()).isZero();
    assertThat(response.get(0).getDeployments().getPendingProdCount()).isNull();
    assertThat(response.get(0).getDeployments().getPendingNonProdCount()).isZero();
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationArtifactDeployments() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    updatedArtifactsAndCdInstanceSummaries();
    Pageable pageable = PageResponseUtils.getPageable(0, 10);
    List<RemediationArtifactDeploymentsListingResponse> response =
        remediationTrackerService
            .listRemediationArtifactDeployments(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(), "artifactId",
                new RemediationArtifactDeploymentsListingRequestBody(), pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(6);

    assertThat(response.get(0).getName()).isEqualTo("env1");
    assertThat(response.get(1).getName()).isEqualTo("env1");
    assertThat(response.get(2).getName()).isEqualTo("env1");
    assertThat(response.get(3).getName()).isEqualTo("env2");
    assertThat(response.get(4).getName()).isEqualTo("env3");
    assertThat(response.get(5).getName()).isEqualTo("envName");

    assertThat(response.get(0).getIdentifier()).isEqualTo("env1");
    assertThat(response.get(1).getIdentifier()).isEqualTo("env1");
    assertThat(response.get(2).getIdentifier()).isEqualTo("env1");
    assertThat(response.get(3).getIdentifier()).isEqualTo("env2");
    assertThat(response.get(4).getIdentifier()).isEqualTo("env3");
    assertThat(response.get(5).getIdentifier()).isEqualTo("envId");

    assertThat(response.get(0).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(1).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(2).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(3).getType()).isEqualTo(EnvironmentType.PREPROD);
    assertThat(response.get(4).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(5).getType()).isEqualTo(EnvironmentType.PROD);

    assertThat(response.get(0).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(1).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(2).getStatus()).isEqualTo(RemediationStatus.ON_GOING);
    assertThat(response.get(3).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(4).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(5).getStatus()).isEqualTo(RemediationStatus.ON_GOING);

    assertThat(response.get(0).getTag()).isEqualTo("tag");
    assertThat(response.get(1).getTag()).isEqualTo("tag");
    assertThat(response.get(2).getTag()).isEqualTo("tag");
    assertThat(response.get(3).getTag()).isEqualTo("tag");
    assertThat(response.get(4).getTag()).isEqualTo("tag");
    assertThat(response.get(5).getTag()).isEqualTo("tag");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationArtifactDeployments_WithEnvIdentifier() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    updatedArtifactsAndCdInstanceSummaries();
    Pageable pageable = PageResponseUtils.getPageable(0, 10);
    List<RemediationArtifactDeploymentsListingResponse> response =
        remediationTrackerService
            .listRemediationArtifactDeployments(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(), "artifactId",
                new RemediationArtifactDeploymentsListingRequestBody().envIdentifier("env1"), pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(3);

    assertThat(response.get(0).getName()).isEqualTo("env1");
    assertThat(response.get(1).getName()).isEqualTo("env1");
    assertThat(response.get(2).getName()).isEqualTo("env1");

    assertThat(response.get(0).getIdentifier()).isEqualTo("env1");
    assertThat(response.get(1).getIdentifier()).isEqualTo("env1");
    assertThat(response.get(2).getIdentifier()).isEqualTo("env1");

    assertThat(response.get(0).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(1).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(response.get(2).getType()).isEqualTo(EnvironmentType.PROD);

    assertThat(response.get(0).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(1).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(2).getStatus()).isEqualTo(RemediationStatus.ON_GOING);

    assertThat(response.get(0).getTag()).isEqualTo("tag");
    assertThat(response.get(1).getTag()).isEqualTo("tag");
    assertThat(response.get(2).getTag()).isEqualTo("tag");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testListRemediationArtifactDeployments_WithPreProdEnv() throws IllegalAccessException {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    updatedArtifactsAndCdInstanceSummaries();
    Pageable pageable = PageResponseUtils.getPageable(0, 10);
    List<RemediationArtifactDeploymentsListingResponse> response =
        remediationTrackerService
            .listRemediationArtifactDeployments(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                remediationTrackerEntity.getUuid(), "artifactId",
                new RemediationArtifactDeploymentsListingRequestBody().envType(EnvironmentTypeFilter.PREPROD), pageable)
            .getContent();

    assertThat(response).isNotNull();
    assertThat(response).hasSize(1);

    assertThat(response.get(0).getName()).isEqualTo("env2");
    assertThat(response.get(0).getIdentifier()).isEqualTo("env2");
    assertThat(response.get(0).getType()).isEqualTo(EnvironmentType.PREPROD);
    assertThat(response.get(0).getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.get(0).getTag()).isEqualTo("tag");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetRemediationDetails() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    RemediationDetailsResponse response = remediationTrackerService.getRemediationDetails(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerEntity.getUuid());
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(remediationTrackerEntity.getUuid());
    assertThat(response.getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.getComponent()).isEqualTo("log4j");
    assertThat(response.getRemediationCondition().getOperator()).isEqualTo(RemediationCondition.OperatorEnum.ALL);
    assertThat(response.getCve()).isNull();
    assertThat(response.getSeverity()).isEqualTo(VulnerabilitySeverity.HIGH);
    assertThat(response.getComments()).isEqualTo("test");
    assertThat(response.getContact().getName()).isEqualTo("test");
    assertThat(response.getContact().getEmail()).isEqualTo("test@gmail.com");
    assertThat(response.getDeploymentsCount().getPatchedProdCount()).isEqualTo(1);
    assertThat(response.getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(response.getDeploymentsCount().getPendingProdCount()).isEqualTo(1);
    assertThat(response.getDeploymentsCount().getPendingNonProdCount()).isZero();
    assertThat(response.getArtifacts()).isEqualTo(1);
    assertThat(response.getEnvironments()).isEqualTo(2);
    assertThat(response.getArtifactsExcluded()).isZero();
    assertThat(response.getEndTimeMilli()).isNotNull();
    assertThat(response.getCreatedByName()).isEqualTo("NAME");
    assertThat(response.getCreatedByEmail()).isEqualTo("EMAIL");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetRemediationDetails_WithExcludedArtifacts() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(ON_GOING);
    repository.save(remediationTrackerEntity);

    remediationTrackerService.excludeArtifact(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
        remediationTrackerEntity.getUuid(), new ExcludeArtifactRequest().artifactId("artifactId"));

    RemediationDetailsResponse response = remediationTrackerService.getRemediationDetails(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerEntity.getUuid());
    assertThat(response).isNotNull();
    assertThat(response.getArtifactsExcluded()).isEqualTo(1);
    assertThat(response.getArtifacts()).isZero();
    assertThat(response.getEnvironments()).isZero();
    assertThat(response.getDeploymentsCount().getPatchedProdCount()).isZero();
    assertThat(response.getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(response.getDeploymentsCount().getPendingProdCount()).isZero();
    assertThat(response.getDeploymentsCount().getPendingNonProdCount()).isZero();
    assertThat(response.getStatus()).isEqualTo(RemediationStatus.COMPLETED);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetArtifactInRemediationDetails() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    RemediationArtifactDetailsResponse response = remediationTrackerService.getRemediationArtifactDetails(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerEntity.getUuid(), "artifactId");
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo("artifactId");
    assertThat(response.getRemediationId()).isEqualTo(remediationTrackerEntity.getUuid());
    assertThat(response.getComponent()).isEqualTo("log4j");
    assertThat(response.getCve()).isNull();
    assertThat(response.getSeverity()).isEqualTo(VulnerabilitySeverity.HIGH);
    assertThat(response.getStatus()).isEqualTo(RemediationStatus.COMPLETED);
    assertThat(response.getArtifactName()).isEqualTo("test/image");
    assertThat(response.getLatestFixedArtifact()).isEqualTo("tag1");
    assertThat(response.getContact().getName()).isEqualTo("test");
    assertThat(response.getContact().getEmail()).isEqualTo("test@gmail.com");
    assertThat(response.getDeploymentsCount().getPatchedProdCount()).isEqualTo(1);
    assertThat(response.getDeploymentsCount().getPatchedNonProdCount()).isZero();
    assertThat(response.getDeploymentsCount().getPendingProdCount()).isEqualTo(1);
    assertThat(response.getDeploymentsCount().getPendingNonProdCount()).isZero();
    assertThat(response.getLatestBuildTag()).isEqualTo("tag");
    assertThat(response.getBuildPipeline().getId()).isEqualTo("pipelineId");
    assertThat(response.getBuildPipeline().getExecutionId()).isEqualTo("executionId");
    assertThat(response.getBuildPipeline().getName()).isEqualTo("name");
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetAllEnvironments() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    List<EnvironmentInfo> environmentInfos = remediationTrackerService.getAllEnvironmentsForArtifact(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), remediationTrackerEntity.getUuid(), "artifactId", null);

    assertThat(environmentInfos.size()).isEqualTo(2);
    assertThat(environmentInfos.get(0).getIdentifier()).isEqualTo("env1");
    assertThat(environmentInfos.get(0).getName()).isEqualTo("envName");
    assertThat(environmentInfos.get(0).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(environmentInfos.get(1).getIdentifier()).isEqualTo("envId");
    assertThat(environmentInfos.get(1).getName()).isEqualTo("envName");
    assertThat(environmentInfos.get(1).getType()).isEqualTo(EnvironmentType.PROD);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetProdEnvironments() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    List<EnvironmentInfo> environmentInfos =
        remediationTrackerService.getAllEnvironmentsForArtifact(builderFactory.getContext().getAccountId(),
            builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
            remediationTrackerEntity.getUuid(), "artifactId", EnvType.Production);

    assertThat(environmentInfos.size()).isEqualTo(2);
    assertThat(environmentInfos.get(0).getIdentifier()).isEqualTo("env1");
    assertThat(environmentInfos.get(0).getName()).isEqualTo("envName");
    assertThat(environmentInfos.get(0).getType()).isEqualTo(EnvironmentType.PROD);
    assertThat(environmentInfos.get(1).getIdentifier()).isEqualTo("envId");
    assertThat(environmentInfos.get(1).getName()).isEqualTo("envName");
    assertThat(environmentInfos.get(1).getType()).isEqualTo(EnvironmentType.PROD);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetPreProdEnvironments() {
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    List<EnvironmentInfo> environmentInfos =
        remediationTrackerService.getAllEnvironmentsForArtifact(builderFactory.getContext().getAccountId(),
            builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
            remediationTrackerEntity.getUuid(), "artifactId", EnvType.PreProduction);

    assertThat(environmentInfos.size()).isZero();
  }

  private void createRemediations() {
    // This creates three different remediation trackers, two completed, one on-going. one completed and one on-going
    // are with CVE.
    RemediationTrackerCreateRequestBody remediationTrackerCreateRequestBody =
        builderFactory.getRemediationTrackerCreateRequestBody();
    RemediationTrackerEntity remediationTrackerEntity =
        createRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    remediationTrackerEntity.getVulnerabilityInfo().setComponent("remediation1");
    remediationTrackerEntity.setStatus(COMPLETED);
    remediationTrackerEntity.setStartTimeMilli(builderFactory.getClock().millis());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(1, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerEntity = createRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    remediationTrackerEntity.setStatus(COMPLETED);
    CVEVulnerability cveVulnerability =
        CVEVulnerability.builder()
            .type(VulnerabilityInfoType.CVE)
            .cve("CVE-2021-44228")
            .component("log4j")
            .severity(io.harness.ssca.entities.remediation_tracker.VulnerabilitySeverity.HIGH)
            .build();
    remediationTrackerEntity.setVulnerabilityInfo(cveVulnerability);
    remediationTrackerEntity.getVulnerabilityInfo().setComponent("remediation2");
    remediationTrackerEntity.setStartTimeMilli(
        builderFactory.getClock().instant().plus(5, ChronoUnit.MINUTES).toEpochMilli());
    remediationTrackerEntity.setEndTimeMilli(
        builderFactory.getClock().instant().plus(2, ChronoUnit.DAYS).toEpochMilli());
    repository.save(remediationTrackerEntity);

    remediationTrackerEntity = createRemediationTrackerEntity(remediationTrackerCreateRequestBody);
    cveVulnerability.setCve("CVE-2021-44229");
    remediationTrackerEntity.setStatus(ON_GOING);
    remediationTrackerEntity.setVulnerabilityInfo(cveVulnerability);
    remediationTrackerEntity.getVulnerabilityInfo().setComponent("remediation3");
    repository.save(remediationTrackerEntity);
  }

  @Test
  @Owner(developers = HUMANSHU_ARORA)
  @Category(UnitTests.class)
  public void testCreateTicketStatusCompleted() {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(()
                        -> remediationTrackerService.createTicket(builderFactory.getContext().getProjectIdentifier(),
                            remediationTrackerEntity.getUuid(), builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getCreateTicketRequest(), builderFactory.getContext().getAccountId()))
        .withMessage(String.format("Remediation Tracker: %s is already closed.", remediationTrackerEntity.getUuid()));
  }

  @Test
  @Owner(developers = HUMANSHU_ARORA)
  @Category(UnitTests.class)
  public void testCreateTicketWhenArtifactIdNull() {
    RemediationTrackerEntity remediationTrackerEntity =
        getRemediationTrackerEntity(remediationTrackerCreateRequestBody);

    remediationTrackerEntity.setStatus(ON_GOING);

    repository.save(remediationTrackerEntity);

    CreateTicketRequest createTicketRequest = builderFactory.getCreateTicketRequest();
    createTicketRequest.artifactId(null);

    String ticketId = remediationTrackerService.createTicket(builderFactory.getContext().getProjectIdentifier(),
        remediationTrackerEntity.getUuid(), builderFactory.getContext().getOrgIdentifier(), createTicketRequest,
        builderFactory.getContext().getAccountId());

    remediationTrackerEntity = remediationTrackerService.getRemediationTracker(remediationTrackerEntity.getUuid());

    assertThat(ticketId).isEqualTo("external");
    assertThat(remediationTrackerEntity.getTicketId()).isEqualTo("external");
    
    private RemediationTrackerEntity createRemediationTrackerEntity(RemediationTrackerCreateRequestBody requestBody) {
    String remediationTrackerId = remediationTrackerService.createRemediationTracker(
        builderFactory.getContext().getAccountId(), builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), requestBody);
    assertThat(remediationTrackerId).isNotNull();
    return remediationTrackerService.getRemediationTracker(remediationTrackerId);
  }
}
