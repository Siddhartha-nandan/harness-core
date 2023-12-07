/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng;

import io.harness.ngcertificates.services.NgCertificateService;
import io.harness.ngcertificates.services.impl.NgCertificateServiceImpl;

import com.google.inject.AbstractModule;

public class NgCertificateModule extends AbstractModule {
  private final NextGenConfiguration config;

  public NgCertificateModule(NextGenConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(NextGenConfiguration.class).toInstance(config);
    bind(NgCertificateService.class).to(NgCertificateServiceImpl.class);
  }
}
