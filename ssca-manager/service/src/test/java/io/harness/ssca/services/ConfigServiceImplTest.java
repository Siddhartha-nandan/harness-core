/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.rule.OwnerRule.SHASHWAT_SACHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;

import io.harness.BuilderFactory;
import io.harness.SSCAManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ConfigRepo;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.ConfigRequestBody;
import io.harness.spec.server.ssca.v1.model.ConfigResponseBody;
import io.harness.ssca.entities.ConfigEntity;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ConfigServiceImplTest extends SSCAManagerTestBase {
  @Inject ConfigService configService;

  @Inject ConfigRepo configRepo;

  private BuilderFactory builderFactory;

  private final String CONFIG_ID = "configId";

  private final String CONFIG_NAME = "sbomqs";

  private final String CONFIG_TYPE = "scorecard";

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveConfig() {
    ConfigRequestBody request = getConfigRequestBody();

    configService.saveConfig(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), request, builderFactory.getContext().getAccountId());

    ArgumentCaptor<ConfigEntity> argument = ArgumentCaptor.forClass(ConfigEntity.class);

    Mockito.verify(configRepo, Mockito.times(1)).saveOrUpdate(argument.capture());

    ConfigEntity capturedConfigEntity = argument.getValue();

    assertThat(capturedConfigEntity.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(capturedConfigEntity.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(capturedConfigEntity.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(capturedConfigEntity.getConfigId()).isEqualTo(CONFIG_ID);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveConfigInvalidAccountId() {
    ConfigRequestBody request = getConfigRequestBody();

    request.setAccountId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> configService.saveConfig(builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), request,
                            builderFactory.getContext().getAccountId()))
        .withMessage("Account Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveConfigInvalidOrgId() {
    ConfigRequestBody request = getConfigRequestBody();

    request.setOrgId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> configService.saveConfig(builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), request,
                            builderFactory.getContext().getAccountId()))
        .withMessage("Org Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveConfigInvalidProjectId() {
    ConfigRequestBody request = getConfigRequestBody();

    request.setProjectId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> configService.saveConfig(builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), request,
                            builderFactory.getContext().getAccountId()))
        .withMessage("Project Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testSaveConfigInvalidConfigId() {
    ConfigRequestBody request = getConfigRequestBody();

    request.setConfigId(null);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> configService.saveConfig(builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), request,
                            builderFactory.getContext().getAccountId()))
        .withMessage("Config Id should not be null or empty");
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetConfigById() {
    Mockito.when(configRepo.findOne(any(), any(), any(), any()))
        .thenReturn(builderFactory.getConfigEntityBuilder().build());
    ConfigResponseBody configResponseBody = configService.getConfigById(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), CONFIG_ID, builderFactory.getContext().getAccountId());

    assertThat(configResponseBody.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(configResponseBody.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(configResponseBody.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(configResponseBody.getConfigId()).isEqualTo(CONFIG_ID);
    assertThat(configResponseBody.getName()).isEqualTo(CONFIG_NAME);
    assertThat(configResponseBody.getType()).isEqualTo(CONFIG_TYPE);
    assertThat(configResponseBody.getConfigInfo()).hasSize(1);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetConfigByNameAndType() {
    Mockito.when(configRepo.findByAccountIdAndProjectIdAndOrgIdAndNameAndType(any(), any(), any(), any(), any()))
        .thenReturn(builderFactory.getConfigEntityBuilder().build());
    ConfigResponseBody configResponseBody = configService.getConfigByNameAndType(
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(), CONFIG_NAME,
        CONFIG_TYPE, builderFactory.getContext().getAccountId());

    assertThat(configResponseBody.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(configResponseBody.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(configResponseBody.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(configResponseBody.getConfigId()).isEqualTo(CONFIG_ID);
    assertThat(configResponseBody.getName()).isEqualTo(CONFIG_NAME);
    assertThat(configResponseBody.getType()).isEqualTo(CONFIG_TYPE);
    assertThat(configResponseBody.getConfigInfo()).hasSize(1);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testGetConfigByIdNotFound() {
    Mockito.when(configRepo.findOne(any(), any(), any(), any())).thenReturn(null);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(()
                        -> configService.getConfigById(builderFactory.getContext().getOrgIdentifier(),
                            builderFactory.getContext().getProjectIdentifier(), CONFIG_ID,
                            builderFactory.getContext().getAccountId()))
        .withMessage(String.format("Config not found for id [%s]", CONFIG_ID));
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testListConfigs() {
    List<ConfigEntity> configEntities =
        Arrays.asList(builderFactory.getConfigEntityBuilder().configId("configId1").build(),
            builderFactory.getConfigEntityBuilder().configId("configId2").build(),
            builderFactory.getConfigEntityBuilder().configId("configId3").build());

    Page<ConfigEntity> configEntityPage = new PageImpl<>(configEntities, Pageable.ofSize(2).withPage(0), 5);

    Mockito.when(configRepo.findAll(any(), any(), any(), any())).thenReturn(configEntityPage);

    Page<ConfigResponseBody> pageConfigs = configService.listConfigs(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), builderFactory.getContext().getAccountId(),
        Pageable.ofSize(3).withPage(0));
    List<ConfigResponseBody> listConfigs = pageConfigs.get().collect(Collectors.toList());
    assertThat(listConfigs.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testDeleteConfig() {
    Mockito.when(configRepo.findOne(any(), any(), any(), any()))
        .thenReturn(builderFactory.getConfigEntityBuilder().build());

    DeleteResult deleteResult = DeleteResult.acknowledged(1);

    Mockito.when(configRepo.delete(any(), any(), any(), any())).thenReturn(deleteResult);

    configService.deleteConfigById(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), CONFIG_ID, builderFactory.getContext().getAccountId());

    Mockito.verify(configRepo, Mockito.times(1)).delete(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = SHASHWAT_SACHAN)
  @Category(UnitTests.class)
  public void testUpdateConfig() {
    ConfigRequestBody request = getConfigRequestBody();

    configService.updateConfigById(builderFactory.getContext().getOrgIdentifier(),
        builderFactory.getContext().getProjectIdentifier(), CONFIG_ID, request,
        builderFactory.getContext().getAccountId());

    ArgumentCaptor<ConfigEntity> argument = ArgumentCaptor.forClass(ConfigEntity.class);

    Mockito.verify(configRepo, Mockito.times(1)).update(argument.capture(), any());

    ConfigEntity capturedConfigEntity = argument.getValue();

    assertThat(capturedConfigEntity.getAccountId()).isEqualTo(builderFactory.getContext().getAccountId());
    assertThat(capturedConfigEntity.getOrgId()).isEqualTo(builderFactory.getContext().getOrgIdentifier());
    assertThat(capturedConfigEntity.getProjectId()).isEqualTo(builderFactory.getContext().getProjectIdentifier());
    assertThat(capturedConfigEntity.getConfigId()).isEqualTo(CONFIG_ID);
  }

  private ConfigRequestBody getConfigRequestBody() {
    return new ConfigRequestBody()
        .accountId(builderFactory.getContext().getAccountId())
        .projectId(builderFactory.getContext().getProjectIdentifier())
        .orgId(builderFactory.getContext().getOrgIdentifier())
        .configId(CONFIG_ID);
  }
}
