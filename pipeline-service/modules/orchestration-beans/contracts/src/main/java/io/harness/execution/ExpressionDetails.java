/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpressionDetails {
  // This will be the relative path from which we will be using expression
  String scope;

  // The list of expressions we want to resolve in that block
  String expressionBlock;

  // Description to help the users in using the expression.
  String description;
}
