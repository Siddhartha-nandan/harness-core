/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt. (Test)
 */

package io.harness;

import io.harness.rule.CiBeansRule;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CiBeansTestBase extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CiBeansRule orchestrationRule = new CiBeansRule(lifecycleRule.getClosingFactory());
}
