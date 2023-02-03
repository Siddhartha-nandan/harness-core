/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AccountTrustLevel {
  Logger log = LoggerFactory.getLogger(AccountTrustLevel.class);

  Integer UNINITIALIZED = -1;
  Integer NEW_USER = 0;
  Integer BASIC_USER = 1;
  Integer MEMBER = 2;
  Integer REGULAR = 3;
}