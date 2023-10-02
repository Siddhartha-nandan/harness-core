/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.subscription.services;

import io.harness.subscription.dto.CardDTO;
import io.harness.subscription.dto.CreditCardDTO;
import io.harness.subscription.responses.CreditCardResponse;

public interface CreditCardService {
  CreditCardResponse saveCreditCard(CreditCardDTO creditCardDTO);
  CreditCardResponse deleteCreditCard(String accountIdentifier, String creditCardIdentifier);
  boolean hasAtleastOneValidCreditCard(String accountIdentifier);
  boolean isValid(String accountIdentifier, String creditCardIdentifier);
  CardDTO getDefaultCreditCard(String accountIdentifier);
}
