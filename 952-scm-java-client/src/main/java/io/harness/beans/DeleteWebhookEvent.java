/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.beans.WebhookEvent.Type.DELETE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Delete")
public class DeleteWebhookEvent implements WebhookEvent {
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public WebhookEvent.Type getType() {
    return DELETE;
  }
}
