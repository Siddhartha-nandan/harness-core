/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.exemption;

import static io.harness.rule.OwnerRule.DHRUVX;
import static io.harness.rule.TestUserProvider.testUserProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.DuplicateEntityException;
import io.harness.ng.core.user.UserInfo;
import io.harness.repositories.exemption.ExemptionRepository;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.ExemptionDurationDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionInitiatorDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionRequestDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionResponseDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionReviewRequestDTO;
import io.harness.spec.server.ssca.v1.model.ExemptionStatusDTO;
import io.harness.spec.server.ssca.v1.model.Operator;
import io.harness.ssca.entities.OperatorEntity;
import io.harness.ssca.entities.exemption.Exemption;
import io.harness.ssca.entities.exemption.Exemption.ExemptionDuration;
import io.harness.ssca.entities.exemption.Exemption.ExemptionInitiator;
import io.harness.ssca.entities.exemption.Exemption.ExemptionStatus;
import io.harness.ssca.services.user.UserService;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.SSCA)
public class ExemptionServiceImplTest extends SSCAManagerTestBase {
  @Mock ExemptionRepository exemptionRepository;
  @Mock UserService userService;
  @Inject ExemptionService exemptionService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String artifactId;
  private String exemptionId;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(exemptionService, "exemptionRepository", exemptionRepository, true);
    FieldUtils.writeField(exemptionService, "userService", userService, true);
    BuilderFactory builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    artifactId = UUID.randomUUID().toString();
    exemptionId = UUID.randomUUID().toString();
    testUserProvider.setActiveUser(
        EmbeddedUser.builder().uuid("UUID1").name("user1").email("user1@harness.io").build());
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testCreateExemption_similarExemptionExists() {
    List<Exemption> existingExemption = new ArrayList<>();
    existingExemption.add(Exemption.builder().build());
    when(exemptionRepository.findExemptions(any())).thenReturn(existingExemption);
    assertThatThrownBy(()
                           -> exemptionService.createExemption(
                               accountId, orgIdentifier, projectIdentifier, artifactId, getExemptionRequestDTO()))
        .isInstanceOf(DuplicateEntityException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testCreateExemption_versionOperatorIsMissing() {
    when(exemptionRepository.findExemptions(any())).thenReturn(Collections.emptyList());
    ExemptionRequestDTO exemptionRequestDTO = new ExemptionRequestDTO();
    exemptionRequestDTO.setComponentVersion("version");
    assertThatThrownBy(()
                           -> exemptionService.createExemption(
                               accountId, orgIdentifier, projectIdentifier, artifactId, exemptionRequestDTO))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Version operator not present");
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testCreateExemption() {
    when(exemptionRepository.findExemptions(any())).thenReturn(Collections.emptyList());
    Exemption savedExemption = getSavedExemption();
    when(exemptionRepository.createExemption(any())).thenReturn(savedExemption);
    when(userService.getUsersWithIds(any(), any())).thenReturn(getUserInfos());
    ExemptionRequestDTO exemptionRequestDTO = getExemptionRequestDTO();
    ExemptionResponseDTO exemptionResponseDTO =
        exemptionService.createExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionRequestDTO);
    ArgumentCaptor<Exemption> argument = ArgumentCaptor.forClass(Exemption.class);
    verify(exemptionRepository, times(1)).createExemption(argument.capture());
    Exemption exemptionRequest = argument.getValue();
    assertThat(exemptionRequest.getCreatedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
    assertThat(exemptionRequest.getUpdatedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
    assertThat(exemptionRequest.getExemptionStatus()).isEqualTo(ExemptionStatus.PENDING);
    assertThat(exemptionResponseDTO.getComponentName()).isEqualTo(savedExemption.getComponentName());
    assertThat(exemptionResponseDTO.getComponentVersion()).isEqualTo(savedExemption.getComponentVersion());
    assertThat(exemptionResponseDTO.getVersionOperator().name()).isEqualTo(savedExemption.getVersionOperator().name());
    assertThat(exemptionResponseDTO.getCreatedAt()).isEqualTo(savedExemption.getCreatedAt());
    assertThat(exemptionResponseDTO.getCreatedByUserId()).isEqualTo("UUID1");
    assertThat(exemptionResponseDTO.getCreatedByName()).isEqualTo("user1");
    assertThat(exemptionResponseDTO.getUpdatedAt()).isEqualTo(savedExemption.getUpdatedAt());
    assertThat(exemptionResponseDTO.getUpdatedBy()).isEqualTo(savedExemption.getUpdatedBy());
    assertThat(exemptionResponseDTO.getExemptionStatus().name()).isEqualTo(savedExemption.getExemptionStatus().name());
    assertThat(exemptionResponseDTO.getExemptionDuration().getDays())
        .isEqualTo(savedExemption.getExemptionDuration().getDays());
    assertThat(exemptionResponseDTO.getExemptionDuration().isAlwaysExempt())
        .isEqualTo(savedExemption.getExemptionDuration().isAlwaysExempt());
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testUpdateExemption_exemptionDoesNotExist() {
    when(exemptionRepository.findExemptions(any())).thenReturn(Collections.emptyList());
    ExemptionRequestDTO exemptionRequestDTO = getExemptionRequestDTO();
    assertThatThrownBy(()
                           -> exemptionService.updateExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionRequestDTO))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testUpdateExemption_versionOperatorIsMissing() {
    Exemption exemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(exemption));
    ExemptionRequestDTO exemptionRequestDTO = new ExemptionRequestDTO();
    exemptionRequestDTO.setComponentVersion("version");
    assertThatThrownBy(()
                           -> exemptionService.updateExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionRequestDTO))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Version operator not present");
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testUpdateExemption_existingExemptionStatusIsNotPending() {
    Exemption exemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.APPROVED).build()));
    ExemptionRequestDTO exemptionRequestDTO = getExemptionRequestDTO();
    assertThatThrownBy(()
                           -> exemptionService.updateExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionRequestDTO))
        .isInstanceOf(BadRequestException.class);
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.REJECTED).build()));
    assertThatThrownBy(()
                           -> exemptionService.updateExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionRequestDTO))
        .isInstanceOf(BadRequestException.class);
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.EXPIRED).build()));
    assertThatThrownBy(()
                           -> exemptionService.updateExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionRequestDTO))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testUpdateExemption() {
    testUserProvider.setActiveUser(
        EmbeddedUser.builder().uuid("UUID2").name("user2").email("user2@harness.io").build());
    Exemption savedExemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(savedExemption));
    when(exemptionRepository.save(any())).thenReturn(savedExemption);
    when(userService.getUsersWithIds(any(), any())).thenReturn(getUserInfos());
    ExemptionRequestDTO exemptionRequestDTO = getUpdateExemptionRequestDTO();
    ExemptionResponseDTO exemptionResponseDTO = exemptionService.updateExemption(
        accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId, exemptionRequestDTO);

    ArgumentCaptor<Exemption> argument = ArgumentCaptor.forClass(Exemption.class);
    verify(exemptionRepository, times(1)).save(argument.capture());
    Exemption updateExemptionRequest = argument.getValue();
    assertThat(updateExemptionRequest.getComponentName()).isEqualTo(exemptionRequestDTO.getComponentName());
    assertThat(updateExemptionRequest.getComponentVersion()).isEqualTo(exemptionRequestDTO.getComponentVersion());
    assertThat(updateExemptionRequest.getVersionOperator().name())
        .isEqualTo(exemptionRequestDTO.getVersionOperator().name());
    assertThat(updateExemptionRequest.getUpdatedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
    assertThat(updateExemptionRequest.getExemptionDuration().isAlwaysExempt())
        .isEqualTo(exemptionRequestDTO.getExemptionDuration().isAlwaysExempt());
    assertThat(updateExemptionRequest.getExemptionDuration().getDays())
        .isEqualTo(exemptionRequestDTO.getExemptionDuration().getDays());

    assertThat(exemptionResponseDTO.getComponentName()).isEqualTo(savedExemption.getComponentName());
    assertThat(exemptionResponseDTO.getComponentVersion()).isEqualTo(savedExemption.getComponentVersion());
    assertThat(exemptionResponseDTO.getVersionOperator().name()).isEqualTo(savedExemption.getVersionOperator().name());
    assertThat(exemptionResponseDTO.getUpdatedAt()).isEqualTo(savedExemption.getUpdatedAt());
    assertThat(exemptionResponseDTO.getUpdatedBy()).isEqualTo("UUID2");
    assertThat(exemptionResponseDTO.getCreatedByUserId()).isEqualTo("UUID1");
    assertThat(exemptionResponseDTO.getCreatedByName()).isEqualTo("user1");
    assertThat(exemptionResponseDTO.getExemptionStatus().name()).isEqualTo(savedExemption.getExemptionStatus().name());
    assertThat(exemptionResponseDTO.getExemptionDuration().getDays())
        .isEqualTo(savedExemption.getExemptionDuration().getDays());
    assertThat(exemptionResponseDTO.getExemptionDuration().isAlwaysExempt())
        .isEqualTo(savedExemption.getExemptionDuration().isAlwaysExempt());
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testDeleteExemption_exemptionDoesNotExist() {
    when(exemptionRepository.findExemptions(any())).thenReturn(Collections.emptyList());
    assertThatThrownBy(
        () -> exemptionService.deleteExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testDeleteExemption_existingExemptionStatusIsNotPending() {
    Exemption exemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.APPROVED).build()));
    ExemptionRequestDTO exemptionRequestDTO = getExemptionRequestDTO();
    assertThatThrownBy(
        () -> exemptionService.deleteExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId))
        .isInstanceOf(BadRequestException.class);
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.REJECTED).build()));
    assertThatThrownBy(
        () -> exemptionService.deleteExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId))
        .isInstanceOf(BadRequestException.class);
    when(exemptionRepository.findExemptions(any()))
        .thenReturn(List.of(exemption.toBuilder().exemptionStatus(ExemptionStatus.EXPIRED).build()));
    assertThatThrownBy(
        () -> exemptionService.deleteExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testDeleteExemption() {
    Exemption exemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(exemption));
    exemptionService.deleteExemption(accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId);
    verify(exemptionRepository, times(1)).deleteById(exemptionId);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testReivewExemption_exemptionDoesNotExist() {
    when(exemptionRepository.findExemptions(any())).thenReturn(Collections.emptyList());
    ExemptionReviewRequestDTO exemptionReviewRequestDTO = new ExemptionReviewRequestDTO();
    exemptionReviewRequestDTO.setExemptionStatus(ExemptionStatusDTO.PENDING);
    exemptionReviewRequestDTO.setReviewComment("comment");
    assertThatThrownBy(()
                           -> exemptionService.reviewExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionReviewRequestDTO))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testReivewExemption_reviewedStatusIsNeitherApprovedNorRejected() {
    Exemption exemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(exemption));
    ExemptionReviewRequestDTO exemptionReviewRequestDTO = new ExemptionReviewRequestDTO();
    exemptionReviewRequestDTO.setExemptionStatus(ExemptionStatusDTO.PENDING);
    exemptionReviewRequestDTO.setReviewComment("comment");
    assertThatThrownBy(()
                           -> exemptionService.reviewExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionReviewRequestDTO))
        .isInstanceOf(BadRequestException.class);
    exemptionReviewRequestDTO.setExemptionStatus(ExemptionStatusDTO.EXPIRED);
    assertThatThrownBy(()
                           -> exemptionService.reviewExemption(accountId, orgIdentifier, projectIdentifier, artifactId,
                               exemptionId, exemptionReviewRequestDTO))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testReviewExemption_approved() {
    testUserProvider.setActiveUser(
        EmbeddedUser.builder().uuid("UUID3").name("user3").email("user3@harness.io").build());
    Exemption savedExemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(savedExemption));
    when(exemptionRepository.save(any())).thenReturn(savedExemption);
    when(userService.getUsersWithIds(any(), any())).thenReturn(getUserInfos());
    ExemptionReviewRequestDTO exemptionReviewRequestDTO = new ExemptionReviewRequestDTO();
    exemptionReviewRequestDTO.setExemptionStatus(ExemptionStatusDTO.APPROVED);
    exemptionReviewRequestDTO.setReviewComment("comment");
    ExemptionResponseDTO exemptionResponseDTO = exemptionService.reviewExemption(
        accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId, exemptionReviewRequestDTO);

    ArgumentCaptor<Exemption> argument = ArgumentCaptor.forClass(Exemption.class);
    verify(exemptionRepository, times(1)).save(argument.capture());
    Exemption reviewExemptionRequest = argument.getValue();
    assertThat(reviewExemptionRequest.getExemptionStatus().name())
        .isEqualTo(exemptionReviewRequestDTO.getExemptionStatus().name());
    assertThat(reviewExemptionRequest.getReviewedAt()).isCloseTo(System.currentTimeMillis(), offset(60000L));
    assertThat(reviewExemptionRequest.getReviewedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
    assertThat(reviewExemptionRequest.getReviewComment()).isEqualTo(exemptionReviewRequestDTO.getReviewComment());
    assertThat(reviewExemptionRequest.getValidUntil())
        .isCloseTo(
            Instant.now().plus(reviewExemptionRequest.getExemptionDuration().getDays(), ChronoUnit.DAYS).toEpochMilli(),
            offset(60000L));

    assertThat(exemptionResponseDTO.getComponentName()).isEqualTo(savedExemption.getComponentName());
    assertThat(exemptionResponseDTO.getComponentVersion()).isEqualTo(savedExemption.getComponentVersion());
    assertThat(exemptionResponseDTO.getVersionOperator().name()).isEqualTo(savedExemption.getVersionOperator().name());
    assertThat(exemptionResponseDTO.getUpdatedAt()).isEqualTo(savedExemption.getUpdatedAt());
    assertThat(exemptionResponseDTO.getUpdatedBy()).isEqualTo("UUID2");
    assertThat(exemptionResponseDTO.getCreatedByUserId()).isEqualTo("UUID1");
    assertThat(exemptionResponseDTO.getCreatedByName()).isEqualTo("user1");
    assertThat(exemptionResponseDTO.getReviewedByUserId()).isEqualTo("UUID3");
    assertThat(exemptionResponseDTO.getReviewedByName()).isEqualTo("user3");
    assertThat(exemptionResponseDTO.getExemptionStatus().name()).isEqualTo(savedExemption.getExemptionStatus().name());
    assertThat(exemptionResponseDTO.getExemptionDuration().getDays())
        .isEqualTo(savedExemption.getExemptionDuration().getDays());
    assertThat(exemptionResponseDTO.getExemptionDuration().isAlwaysExempt())
        .isEqualTo(savedExemption.getExemptionDuration().isAlwaysExempt());
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testReviewExemption_rejected() {
    testUserProvider.setActiveUser(
        EmbeddedUser.builder().uuid("UUID3").name("user3").email("user3@harness.io").build());
    Exemption savedExemption = getSavedExemption();
    when(exemptionRepository.findExemptions(any())).thenReturn(List.of(savedExemption));
    when(exemptionRepository.save(any())).thenReturn(savedExemption);
    when(userService.getUsersWithIds(any(), any())).thenReturn(getUserInfos());
    ExemptionReviewRequestDTO exemptionReviewRequestDTO = new ExemptionReviewRequestDTO();
    exemptionReviewRequestDTO.setExemptionStatus(ExemptionStatusDTO.REJECTED);
    exemptionReviewRequestDTO.setReviewComment("comment");
    ExemptionResponseDTO exemptionResponseDTO = exemptionService.reviewExemption(
        accountId, orgIdentifier, projectIdentifier, artifactId, exemptionId, exemptionReviewRequestDTO);

    ArgumentCaptor<Exemption> argument = ArgumentCaptor.forClass(Exemption.class);
    verify(exemptionRepository, times(1)).save(argument.capture());
    Exemption reviewExemptionRequest = argument.getValue();
    assertThat(reviewExemptionRequest.getExemptionStatus().name())
        .isEqualTo(exemptionReviewRequestDTO.getExemptionStatus().name());
    assertThat(reviewExemptionRequest.getReviewedAt()).isCloseTo(System.currentTimeMillis(), offset(60000L));
    assertThat(reviewExemptionRequest.getReviewedBy()).isEqualTo(testUserProvider.activeUser().getUuid());
    assertThat(reviewExemptionRequest.getReviewComment()).isEqualTo(exemptionReviewRequestDTO.getReviewComment());
    assertThat(reviewExemptionRequest.getValidUntil()).isZero();

    assertThat(exemptionResponseDTO.getComponentName()).isEqualTo(savedExemption.getComponentName());
    assertThat(exemptionResponseDTO.getComponentVersion()).isEqualTo(savedExemption.getComponentVersion());
    assertThat(exemptionResponseDTO.getVersionOperator().name()).isEqualTo(savedExemption.getVersionOperator().name());
    assertThat(exemptionResponseDTO.getUpdatedAt()).isEqualTo(savedExemption.getUpdatedAt());
    assertThat(exemptionResponseDTO.getUpdatedBy()).isEqualTo("UUID2");
    assertThat(exemptionResponseDTO.getCreatedByUserId()).isEqualTo("UUID1");
    assertThat(exemptionResponseDTO.getCreatedByName()).isEqualTo("user1");
    assertThat(exemptionResponseDTO.getReviewedByUserId()).isEqualTo("UUID3");
    assertThat(exemptionResponseDTO.getReviewedByName()).isEqualTo("user3");
    assertThat(exemptionResponseDTO.getExemptionStatus().name()).isEqualTo(savedExemption.getExemptionStatus().name());
    assertThat(exemptionResponseDTO.getExemptionDuration().getDays())
        .isEqualTo(savedExemption.getExemptionDuration().getDays());
    assertThat(exemptionResponseDTO.getExemptionDuration().isAlwaysExempt())
        .isEqualTo(savedExemption.getExemptionDuration().isAlwaysExempt());
  }

  private Exemption getSavedExemption() {
    return Exemption.builder()
        .componentName("name")
        .componentVersion("version")
        .versionOperator(OperatorEntity.LESSTHAN)
        .exemptionStatus(ExemptionStatus.PENDING)
        .exemptionDuration(getExemptionDuration())
        .exemptionInitiator(ExemptionInitiator.builder().projectId(projectIdentifier).artifactId(artifactId).build())
        .uuid("uuid")
        .createdBy("UUID1")
        .createdAt(1L)
        .updatedAt(1L)
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .artifactId(artifactId)
        .reason("reason")
        .updatedBy("UUID2")
        .reviewedBy("UUID3")
        .build();
  }

  private static ExemptionDuration getExemptionDuration() {
    return ExemptionDuration.builder().alwaysExempt(false).days(5).build();
  }

  private static List<UserInfo> getUserInfos() {
    List<UserInfo> userInfos = new ArrayList<>();
    userInfos.add(UserInfo.builder().uuid("UUID1").name("user1").build());
    userInfos.add(UserInfo.builder().uuid("UUID2").name("user2").build());
    userInfos.add(UserInfo.builder().uuid("UUID3").name("user3").build());
    return userInfos;
  }

  private ExemptionRequestDTO getExemptionRequestDTO() {
    ExemptionRequestDTO exemptionRequestDTO = new ExemptionRequestDTO();
    exemptionRequestDTO.setComponentName("name");
    exemptionRequestDTO.setComponentVersion("version");
    exemptionRequestDTO.versionOperator(Operator.LESSTHAN);
    ExemptionDurationDTO exemptionDurationDTO = new ExemptionDurationDTO();
    exemptionDurationDTO.alwaysExempt(false);
    exemptionDurationDTO.days(5);
    exemptionRequestDTO.exemptionDuration(exemptionDurationDTO);
    exemptionRequestDTO.reason("reason");
    ExemptionInitiatorDTO exemptionInitiatorDTO = new ExemptionInitiatorDTO();
    exemptionInitiatorDTO.setProjectIdentifier(projectIdentifier);
    exemptionInitiatorDTO.setArtifactId(artifactId);
    exemptionRequestDTO.setExemptionInitiator(exemptionInitiatorDTO);
    return exemptionRequestDTO;
  }

  private ExemptionRequestDTO getUpdateExemptionRequestDTO() {
    ExemptionRequestDTO exemptionRequestDTO = new ExemptionRequestDTO();
    exemptionRequestDTO.setComponentName("updatedName");
    exemptionRequestDTO.setComponentVersion("updatedVersion");
    exemptionRequestDTO.versionOperator(Operator.EQUALS);
    ExemptionDurationDTO exemptionDurationDTO = new ExemptionDurationDTO();
    exemptionDurationDTO.alwaysExempt(true);
    exemptionRequestDTO.exemptionDuration(exemptionDurationDTO);
    exemptionRequestDTO.reason("updatedReason");
    ExemptionInitiatorDTO exemptionInitiatorDTO = new ExemptionInitiatorDTO();
    exemptionInitiatorDTO.setProjectIdentifier(projectIdentifier);
    exemptionInitiatorDTO.setArtifactId(artifactId);
    exemptionRequestDTO.setExemptionInitiator(exemptionInitiatorDTO);
    return exemptionRequestDTO;
  }
}
