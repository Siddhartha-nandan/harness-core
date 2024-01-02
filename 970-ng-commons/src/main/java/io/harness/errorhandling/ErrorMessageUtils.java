/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.errorhandling;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.Scope;

public class ErrorMessageUtils {
  public static String getScopeLogString(Scope scope, boolean includeAccountIdentifier, String prefix) {
    StringBuilder message = new StringBuilder();
    if (includeAccountIdentifier) {
      message.append(String.format("account [%s] ", scope.getAccountIdentifier()));
    }
    if (isNotEmpty(scope.getProjectIdentifier())) {
      message.append(String.format("org [%s], project [%s]", scope.getOrgIdentifier(), scope.getProjectIdentifier()));
    } else if (isNotEmpty(scope.getOrgIdentifier())) {
      message.append(String.format("org [%s]", scope.getOrgIdentifier()));
    }
    if (!message.isEmpty()) {
      message.insert(0, prefix);
    }
    return message.toString();
  }
}
