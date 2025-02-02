/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.mappers.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.helpers.ModuleLicenseHelper;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CFLicenseObjectMapperTest extends CategoryTest {
  @Mock ModuleLicenseHelper moduleLicenseHelper;
  @InjectMocks CFLicenseObjectMapper objectMapper;
  private CFModuleLicense moduleLicense;
  private CFModuleLicenseDTO moduleLicenseDTO;
  private static final int DEFAULT_USER_NUMBER = 2;
  private static final long DEFAULT_CLIENT_MAU = 5000;

  @Before
  public void setUp() {
    initMocks(this);
    when(moduleLicenseHelper.isDeveloperLicensingFeatureEnabled(any())).thenReturn(true);

    moduleLicense =
        CFModuleLicense.builder().numberOfUsers(DEFAULT_USER_NUMBER).numberOfClientMAUs(DEFAULT_CLIENT_MAU).build();
    moduleLicenseDTO =
        CFModuleLicenseDTO.builder().numberOfUsers(DEFAULT_USER_NUMBER).numberOfClientMAUs(DEFAULT_CLIENT_MAU).build();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO result = objectMapper.toDTO(moduleLicense);
    assertThat(result).isEqualTo(moduleLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    ModuleLicense result = objectMapper.toEntity(moduleLicenseDTO);
    assertThat(result).isEqualTo(moduleLicense);
  }
}
