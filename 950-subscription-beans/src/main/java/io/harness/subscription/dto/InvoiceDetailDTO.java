/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceDetailDTO {
  private List<ItemDTO> items;
  private String subscriptionId;
  private String invoiceId;
  private Long totalAmount;
  private Long periodStart;
  private Long periodEnd;
  private Long nextPaymentAttempt;
  private Long amountDue;
  private String clientSecret;
  private PaymentIntentDetailDTO paymentIntent;
}
