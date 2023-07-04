/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.subscription.services.impl;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.CreditCardRepository;
import io.harness.repositories.StripeCustomerRepository;
import io.harness.subscription.dto.CardDTO;
import io.harness.subscription.dto.CreditCardDTO;
import io.harness.subscription.dto.PaymentMethodCollectionDTO;
import io.harness.subscription.entities.CreditCard;
import io.harness.subscription.entities.StripeCustomer;
import io.harness.subscription.helpers.StripeHelper;
import io.harness.subscription.responses.CreditCardResponse;
import io.harness.subscription.services.CreditCardService;

import com.google.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreditCardServiceImpl implements CreditCardService {
  private final CreditCardRepository creditCardRepository;
  private final StripeCustomerRepository stripeCustomerRepository;
  private final StripeHelper stripeHelper;

  private final String DUPLICATE_CARD = "Credit card already exists.";
  private final String SAVE_CARD_FAILED = "Could not save credit card.";
  private final String CUSTOMER_DOES_NOT_EXIST = "Customer with account identifier %s does not exist.";
  private final String CARD_NOT_FOUND = "No primary credit card found for account %s";

  @Inject
  public CreditCardServiceImpl(CreditCardRepository creditCardRepository,
      StripeCustomerRepository stripeCustomerRepository, StripeHelper stripeHelper) {
    this.creditCardRepository = creditCardRepository;
    this.stripeCustomerRepository = stripeCustomerRepository;
    this.stripeHelper = stripeHelper;
  }

  @Override
  public CreditCardResponse saveCreditCard(CreditCardDTO creditCardRequest) {
    CreditCard creditCard = creditCardRepository.findByFingerprint(creditCardRequest.getFingerprint());

    if (creditCard != null) {
      if (creditCard.getAccountIdentifier().equals(creditCardRequest.getAccountIdentifier())) {
        log.error(DUPLICATE_CARD);
        throw new DuplicateFieldException(DUPLICATE_CARD);
      } else {
        stripeHelper.deleteCard(creditCardRequest.getCustomerIdentifier(), creditCardRequest.getCreditCardIdentifier());
        log.error(SAVE_CARD_FAILED);
        throw new BadRequestException(SAVE_CARD_FAILED);
      }
    }

    PaymentMethodCollectionDTO paymentMethodCollectionDTO =
        stripeHelper.listPaymentMethods(creditCardRequest.getCustomerIdentifier());

    if (paymentMethodCollectionDTO != null) {
      paymentMethodCollectionDTO.getPaymentMethods()
          .stream()
          .filter((CardDTO stripeCardDtO) -> !stripeCardDtO.getId().equals(creditCardRequest.getCreditCardIdentifier()))
          .forEach((CardDTO stripeCardDTO)
                       -> stripeHelper.deleteCard(creditCardRequest.getCustomerIdentifier(), stripeCardDTO.getId()));
    }
    return toCreditCardResponse(
        creditCardRepository.save(CreditCard.builder()
                                      .accountIdentifier(creditCardRequest.getAccountIdentifier())
                                      .fingerprint(creditCardRequest.getFingerprint())
                                      .build()));
  }

  @Override
  public boolean hasValidCard(String accountIdentifier) {
    StripeCustomer stripeCustomer = stripeCustomerRepository.findByAccountIdentifier(accountIdentifier);
    if (stripeCustomer == null) {
      String errorMessage = String.format(CUSTOMER_DOES_NOT_EXIST, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
    List<CardDTO> creditCards = stripeHelper.listPaymentMethods(stripeCustomer.getCustomerId()).getPaymentMethods();

    return !creditCards.isEmpty() && hasAtLeastOneUnexpiredCard(creditCards);
  }

  @Override
  public CardDTO getDefaultCreditCard(String accountIdentifier) {
    StripeCustomer stripeCustomer = stripeCustomerRepository.findByAccountIdentifier(accountIdentifier);
    if (stripeCustomer == null) {
      String errorMessage = String.format(CUSTOMER_DOES_NOT_EXIST, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }

    PaymentMethodCollectionDTO paymentMethodCollectionDTO =
        stripeHelper.listPaymentMethods(stripeCustomer.getCustomerId());

    if (paymentMethodCollectionDTO == null) {
      String errorMessage = String.format(CARD_NOT_FOUND, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidArgumentsException(errorMessage);
    }

    Optional<CardDTO> primaryCardDTO = paymentMethodCollectionDTO.getPaymentMethods()
                                           .stream()
                                           .filter((CardDTO cardDTO) -> cardDTO.getIsDefaultCard())
                                           .findFirst();
    if (primaryCardDTO.isEmpty()) {
      String errorMessage = String.format(CARD_NOT_FOUND, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidArgumentsException(errorMessage);
    }

    return primaryCardDTO.get();
  }

  private CreditCardResponse toCreditCardResponse(CreditCard creditCard) {
    return CreditCardResponse.builder()
        .creditCardDTO(CreditCardDTO.builder()
                           .accountIdentifier(creditCard.getAccountIdentifier())
                           .fingerprint(creditCard.getFingerprint())
                           .build())
        .createdAt(creditCard.getCreatedAt())
        .lastUpdatedAt(creditCard.getLastUpdatedAt())
        .build();
  }

  private boolean hasAtLeastOneUnexpiredCard(List<CardDTO> creditCards) {
    return creditCards.stream().anyMatch((CardDTO cardDTO)
                                             -> cardDTO.getExpireYear() > LocalDate.now().getYear()
            || (cardDTO.getExpireYear() == LocalDate.now().getYear()
                && cardDTO.getExpireMonth() >= LocalDate.now().getMonth().getValue()));
  }
}
