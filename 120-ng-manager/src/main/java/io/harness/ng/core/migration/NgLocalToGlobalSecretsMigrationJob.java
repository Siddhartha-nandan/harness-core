/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NgLocalToGlobalSecretsMigrationJob implements Managed {
  private Future<?> ngLocalToGlobalSecretsMigrationFuture;
  private final ScheduledExecutorService executorService;
  private static final String DEBUG_MESSAGE = "NgLocalToGlobalSecretsMigrationJob: ";
  private final NgLocalToGlobalSecretsMigrationService ngLocalToGlobalSecretsMigrationService;

  @Inject
  public NgLocalToGlobalSecretsMigrationJob(
      NgLocalToGlobalSecretsMigrationService ngLocalToGlobalSecretsMigrationService) {
    this.ngLocalToGlobalSecretsMigrationService = ngLocalToGlobalSecretsMigrationService;
    String threadName = "ng-secrets-migration-local-to-GcpKms-thread";
    this.executorService =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(threadName).build());
  }

  @Override
  public void start() throws Exception {
    log.info(DEBUG_MESSAGE + "started...");
    Random random = new Random();
    int delay = random.nextInt(2) + 15;
    ngLocalToGlobalSecretsMigrationFuture =
        executorService.scheduleWithFixedDelay(ngLocalToGlobalSecretsMigrationService, delay, 720, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    log.info(DEBUG_MESSAGE + "stopping...");
    ngLocalToGlobalSecretsMigrationFuture.cancel(false);
    executorService.shutdown();
  }
}
