/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifacts.gar.service;

import static io.harness.rule.OwnerRule.ABHISHEK;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.HintException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GARUtilsTest {
  @Test
  @Owner(developers = ABHISHEK)
  @Category(UnitTests.class)
  public void checkIfResponseNullTest() {
    assertThatThrownBy(() -> { GARUtils.checkIfResponseNull(null); }).isInstanceOf(HintException.class);
  }
}