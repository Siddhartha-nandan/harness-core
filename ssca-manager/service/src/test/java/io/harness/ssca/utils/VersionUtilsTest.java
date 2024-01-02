/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.utils;

import static io.harness.rule.OwnerRule.DHRUVX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.ssca.entities.OperatorEntity;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.SSCA)
public class VersionUtilsTest extends SSCAManagerTestBase {
  @Test
  @Owner(developers = DHRUVX)
  @Category(UnitTests.class)
  public void testCompareVersions() {
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.EQUALS, "1")).isTrue();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.EQUALS, "0")).isFalse();
    assertThat(VersionUtils.compareVersions("1.2.3", OperatorEntity.STARTSWITH, "1")).isTrue();
    assertThat(VersionUtils.compareVersions("1.2.3", OperatorEntity.STARTSWITH, "2")).isFalse();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.NOTEQUALS, "0")).isTrue();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.NOTEQUALS, "1")).isFalse();
    assertThat(VersionUtils.compareVersions("1.2.3", OperatorEntity.GREATERTHAN, "1")).isTrue();
    assertThat(VersionUtils.compareVersions("1.2.3", OperatorEntity.GREATERTHAN, "2")).isFalse();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.GREATERTHANEQUALS, "0")).isTrue();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.GREATERTHANEQUALS, "1")).isTrue();
    assertThat(VersionUtils.compareVersions("1", OperatorEntity.GREATERTHANEQUALS, "2")).isFalse();
    assertThat(VersionUtils.compareVersions("2", OperatorEntity.LESSTHAN, "3")).isTrue();
    assertThat(VersionUtils.compareVersions("2", OperatorEntity.LESSTHAN, "1")).isFalse();
    assertThat(VersionUtils.compareVersions("2", OperatorEntity.LESSTHANEQUALS, "3")).isTrue();
    assertThat(VersionUtils.compareVersions("2", OperatorEntity.LESSTHANEQUALS, "2")).isTrue();
    assertThat(VersionUtils.compareVersions("2", OperatorEntity.LESSTHANEQUALS, "1")).isFalse();
  }
}
