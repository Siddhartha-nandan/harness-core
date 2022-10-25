/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.vm.steps;

import io.harness.reflection.util.ExpressionReflectionUtils.NestedAnnotationResolver;

public interface VmStepInfo extends NestedAnnotationResolver {
  enum Type { RUN, PLUGIN, RUN_TEST, BACKGROUND }
  Type getType();
}
