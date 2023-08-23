/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.subscription.entities.CreditCard;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface CreditCardRepository extends CrudRepository<CreditCard, String> {
  CreditCard findByFingerprint(String fingerprint);
  CreditCard findByCreditCardIdentifier(String creditCardIdentifier);
}