/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.migration;

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
@OwnedBy(HarnessTeam.CDC)
public class RolesResourceMigrationService implements Managed {
  private Future<?> roleResourceJobFuture;
  private final ScheduledExecutorService executorService;
  private static final String DEBUG_MESSAGE = "RoleResourceMigrationService: ";
  private final RoleResourceMigration roleResourceMigration;

  @Inject
  public RolesResourceMigrationService(RoleResourceMigration roleResourceMigration) {
    this.roleResourceMigration = roleResourceMigration;
    String threadName = "role-resource-migration-service-thread";
    this.executorService =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(threadName).build());
  }

  @Override
  public void start() throws Exception {
    log.info(DEBUG_MESSAGE + "started...");
    Random random = new Random();
    int delay = random.nextInt(15) + 15;
    roleResourceJobFuture =
        executorService.scheduleWithFixedDelay(roleResourceMigration, delay, 1440, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    log.info(DEBUG_MESSAGE + "stopping...");
    roleResourceJobFuture.cancel(false);
    executorService.shutdown();
  }
}
