/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.DELEGATE_NOT_AVAILABLE;
import static io.harness.eraro.ErrorCode.NO_AVAILABLE_DELEGATES;

public class NoAvailableDelegatesException extends NoDelegatesException {
  public NoAvailableDelegatesException() {
    super("Delegates are not available", NO_AVAILABLE_DELEGATES);
  }

  public NoAvailableDelegatesException(String message) {
    super(message, DELEGATE_NOT_AVAILABLE);
  }
}
