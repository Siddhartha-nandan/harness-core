/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddIsSmpAccountToAccount implements Migration {
  private static final String IS_SMP_ACCOUNT_FIELD = "isSmpAccount";

  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      for (Account account : accounts) {
        if (!Objects.isNull(account) && !account.isSmpAccount()) {
          try {
            wingsPersistence.updateField(Account.class, account.getUuid(), IS_SMP_ACCOUNT_FIELD, Boolean.FALSE);
          } catch (RuntimeException e) {
            log.error("Unable to update isSmpAccount field of account {}", account.getUuid(), e);
          }
        }
      }
    }
  }
}
