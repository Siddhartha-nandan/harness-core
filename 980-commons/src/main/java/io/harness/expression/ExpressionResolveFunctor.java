/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ResolveObjectResponse;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ExpressionResolveFunctor {
  String processString(String expression);

  default ResolveObjectResponse processObject(Object o) {
    return new ResolveObjectResponse(false, null);
  }

  default boolean supportsNotExpression() {
    return true;
  }
}
