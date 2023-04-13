/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ipallowlist.resource;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.MEENAKSHI;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ipallowlist.IPAllowlistResourceUtils;
import io.harness.ipallowlist.entity.IPAllowlistEntity;
import io.harness.ipallowlist.service.IPAllowlistService;
import io.harness.rule.Owner;
import io.harness.spec.server.ng.v1.model.AllowedSourceType;
import io.harness.spec.server.ng.v1.model.IPAllowlistConfig;
import io.harness.spec.server.ng.v1.model.IPAllowlistConfigRequest;
import io.harness.spec.server.ng.v1.model.IPAllowlistConfigResponse;

import java.util.List;
import javax.validation.Validator;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class IpAllowlistApiImplTest extends CategoryTest {
  @Mock private AccessControlClient accessControlClient;
  @Mock private IPAllowlistService ipAllowlistService;

  private IPAllowlistResourceUtils ipAllowlistResourceUtil;
  private Validator validator;
  private IpAllowlistApiImpl ipAllowlistApi;
  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  private static final String ACCOUNT_IDENTIFIER = randomAlphabetic(10);

  private static final String IDENTIFIER = randomAlphabetic(10);
  private static final String NAME = randomAlphabetic(10);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    validator = mock(Validator.class);
    this.ipAllowlistResourceUtil = new IPAllowlistResourceUtils(validator);
    this.ipAllowlistApi = new IpAllowlistApiImpl(ipAllowlistService, ipAllowlistResourceUtil, accessControlClient);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void testCreateIpAllowlistConfig() {
    IPAllowlistConfigRequest request = getIpAllowlistConfigRequest();
    IPAllowlistEntity ipAllowlistEntity = getIPAllowlistEntity();
    when(ipAllowlistService.create(ipAllowlistEntity)).thenReturn(ipAllowlistEntity);

    Response result = ipAllowlistApi.createIpAllowlistConfig(request, ACCOUNT_IDENTIFIER);
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(201);
    assertThat(result.getEntity()).isEqualTo(getIpAllowlistConfigResponse());
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void testGetIpAllowlistConfig() {
    IPAllowlistEntity ipAllowlistEntity = getIPAllowlistEntity();
    when(ipAllowlistService.get(ACCOUNT_IDENTIFIER, IDENTIFIER)).thenReturn(ipAllowlistEntity);
    Response result = ipAllowlistApi.getIpAllowlistConfig(IDENTIFIER, ACCOUNT_IDENTIFIER);
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(200);
    assertThat(result.getEntity()).isEqualTo(getIpAllowlistConfigResponse());
  }

  private IPAllowlistConfigRequest getIpAllowlistConfigRequest() {
    IPAllowlistConfigRequest ipAllowlistConfigRequest = new IPAllowlistConfigRequest();

    ipAllowlistConfigRequest.ipAllowlistConfig(getIpAllowlistConfig());
    return ipAllowlistConfigRequest;
  }

  private IPAllowlistConfigResponse getIpAllowlistConfigResponse() {
    IPAllowlistConfigResponse ipAllowlistConfigResponse = new IPAllowlistConfigResponse();

    ipAllowlistConfigResponse.ipAllowlistConfig(getIpAllowlistConfig());
    ipAllowlistConfigResponse.created(null);
    ipAllowlistConfigResponse.updated(null);
    return ipAllowlistConfigResponse;
  }

  private IPAllowlistConfig getIpAllowlistConfig() {
    IPAllowlistConfig ipAllowlistConfig = new IPAllowlistConfig();
    ipAllowlistConfig.identifier(IDENTIFIER);
    ipAllowlistConfig.name(NAME);
    ipAllowlistConfig.description("description");
    ipAllowlistConfig.ipAddress("1.2.3.4");

    ipAllowlistConfig.allowedSourceType(List.of(AllowedSourceType.UI));
    ipAllowlistConfig.enabled(true);
    return ipAllowlistConfig;
  }

  private IPAllowlistEntity getIPAllowlistEntity() {
    return IPAllowlistEntity.builder()
        .id(null)
        .accountIdentifier(ACCOUNT_IDENTIFIER)
        .identifier(IDENTIFIER)
        .name(NAME)
        .description("description")
        .allowedSourceType(List.of(AllowedSourceType.UI))
        .enabled(true)
        .ipAddress("1.2.3.4")
        .created(null)
        .updated(null)
        .lastUpdatedBy(null)
        .createdBy(null)
        .build();
  }
}
