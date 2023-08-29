/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.utils.transformers;

import io.harness.SSCAManagerTestBase;
import io.harness.spec.server.ssca.v1.model.EnforcementResultDTO;
import io.harness.ssca.entities.EnforcementResultEntity;

import java.util.Arrays;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class EnforcementResultTransformerTest extends SSCAManagerTestBase {
  private EnforcementResultDTO dto;
  private EnforcementResultEntity entity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    entity = EnforcementResultEntity.builder()
                 .accountId("accountId")
                 .purl("purl")
                 .enforcementId("enforcementId")
                 .artifactId("artifactId")
                 .imageName("imageName")
                 .license(Arrays.asList("license1", "license2"))
                 .name("name")
                 .orchestrationId("orchestrationId")
                 .orgIdentifier("orgIdentifier")
                 .packageManager("packageManager")
                 .projectIdentifier("projectIdentifier")
                 .supplier("supplier")
                 .supplierType("supplierType")
                 .tag("tag")
                 .version("version")
                 .violationDetails("violationDetails")
                 .violationType("violationType")
                 .build();

    dto = new EnforcementResultDTO()
              .accountId("accountId")
              .purl("purl")
              .enforcementId("enforcementId")
              .artifactId("artifactId")
              .imageName("imageName")
              .license(Arrays.asList("license1", "license2"))
              .name("name")
              .orchestrationId("orchestrationId")
              .orgIdentifier("orgIdentifier")
              .packageManager("packageManager")
              .projectIdentifier("projectIdentifier")
              .supplier("supplier")
              .supplierType("supplierType")
              .tag("tag")
              .version("version")
              .violationDetails("violationDetails")
              .violationType("violationType");

    // transformer = new EnforcementResultTransformer();
  }

  /*@Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testToEntity() {
    EnforcementResultEntity enforcementResultEntity = transformer.toEntity(dto);
    assertThat(enforcementResultEntity.equals(entity)).isEqualTo(true);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testToDTO() {
    EnforcementResultDTO enforcementResultDTO = transformer.toDTO(entity);
    assertThat(enforcementResultDTO.equals(dto)).isEqualTo(true);
  }*/
}
