/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ticketserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.sto.beans.entities.TicketServiceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.STO)
public class TicketServiceClientModule extends AbstractModule {
  TicketServiceConfig ticketServiceConfig;

  @Inject
  public TicketServiceClientModule(TicketServiceConfig ticketServiceConfig) {
    this.ticketServiceConfig = ticketServiceConfig;
  }

  @Override
  protected void configure() {
    this.bind(TicketServiceConfig.class).toInstance(this.ticketServiceConfig);
    this.bind(TicketServiceClient.class).toProvider(TicketServiceClientFactory.class).in(Scopes.SINGLETON);
  }
}
