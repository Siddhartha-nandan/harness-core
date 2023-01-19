/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.apache.cxf.annotations.EvaluateAllEndpoints;

@Value
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomChangeWebhookPayload {
  @NotNull String eventIdentifier;
  @NotNull long startTime;
  @NotNull long endTime;
  @NotNull String user;
  @NotNull CustomChangeWebhookEventDetail eventDetail;
  @Value
  @Builder
  public static class CustomChangeWebhookEventDetail {
    @NotNull String description;
    String changeEventDetailsLink;
    String internalLinkToEntity;
    @NotNull String name;
  }
}
