/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.exemption;

import static io.harness.rule.OwnerRule.DHRUVX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.ssca.entities.EnforcementResultEntity;
import io.harness.ssca.entities.OperatorEntity;
import io.harness.ssca.entities.exemption.Exemption;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.SSCA)
public class ExemptionHelperTest extends SSCAManagerTestBase {
  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetUniqueComponentKeyFromEnforcementResultEntity() {
    EnforcementResultEntity entity1 = EnforcementResultEntity.builder().name("name").build();
    String uniqueComponentKey1 = ExemptionHelper.getUniqueComponentKeyFromEnforcementResultEntity(entity1);
    assertThat(uniqueComponentKey1).isEqualTo("name");

    EnforcementResultEntity entity2 = EnforcementResultEntity.builder().name("name").version("version").build();
    String uniqueComponentKey2 = ExemptionHelper.getUniqueComponentKeyFromEnforcementResultEntity(entity2);
    assertThat(uniqueComponentKey2).isEqualTo("name,version");
  }

  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testGetExemptedComponents() {
    String uniqueEntity1 = "name1,v1";
    String uniqueEntity2 = "name2";
    String uniqueEntity3 = "name3,1";
    Exemption exemption1 = Exemption.builder()
                               .uuid("uuid1")
                               .componentName("name1")
                               .componentVersion("v1")
                               .versionOperator(OperatorEntity.EQUALS)
                               .build();
    Exemption exemption2 = Exemption.builder().uuid("uuid2").componentName("name2").build();
    Exemption exemption3 = Exemption.builder()
                               .uuid("uuid3")
                               .componentName("name3")
                               .componentVersion("2")
                               .versionOperator(OperatorEntity.EQUALS)
                               .build();
    Exemption exemption4 = Exemption.builder().uuid("uuid4").componentName("name4").build();

    Map<String, String> exemptedComponents = ExemptionHelper.getExemptedComponents(
        Set.of(uniqueEntity1, uniqueEntity2, uniqueEntity3), List.of(exemption1, exemption2, exemption3, exemption4));
    assertThat(exemptedComponents).hasSize(2);
    assertThat(exemptedComponents).containsEntry(uniqueEntity1, exemption1.getUuid());
    assertThat(exemptedComponents).containsEntry(uniqueEntity2, exemption2.getUuid());
  }
}
