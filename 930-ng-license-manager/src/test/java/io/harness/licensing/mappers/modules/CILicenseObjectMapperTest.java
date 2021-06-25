package io.harness.licensing.mappers.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CILicenseObjectMapperTest extends CategoryTest {
  @InjectMocks CILicenseObjectMapper objectMapper;
  private CIModuleLicense moduleLicense;
  private CIModuleLicenseDTO moduleLicenseDTO;
  private static final int DEFAULT_NUMBER_OF_COMMITTERS = 10;
  private static final int START_TIME = 10;
  private static final int EXPIRY_TIME = 11;

  @Before
  public void setUp() {
    initMocks(this);
    moduleLicense = CIModuleLicense.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build();
    moduleLicenseDTO = CIModuleLicenseDTO.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO result = objectMapper.toDTO(moduleLicense);
    assertThat(result).isEqualTo(moduleLicenseDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    ModuleLicense result = objectMapper.toEntity(moduleLicenseDTO);
    assertThat(result).isEqualTo(moduleLicense);
  }
}
