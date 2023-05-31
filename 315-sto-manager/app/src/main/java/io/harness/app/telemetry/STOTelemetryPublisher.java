/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.telemetry;

import static io.harness.annotations.dev.HarnessTeam.STO;
import static io.harness.configuration.DeployVariant.DEPLOY_VERSION;
import static io.harness.telemetry.Destination.ALL;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.stoserviceclient.STOServiceUtils;
import io.harness.telemetry.TelemetryOption;
import io.harness.telemetry.TelemetryReporter;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(STO)
public class STOTelemetryPublisher {
  @Inject TelemetryReporter telemetryReporter;
  @Inject private STOServiceUtils stoServiceUtils;
  String LICENSE_USAGE = "sto_license_usage";
  String ACCOUNT_DEPLOY_TYPE = "account_deploy_type";
  private static final String ACCOUNT = "Account";
  private static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  private static final String GROUP_TYPE = "group_type";
  private static final String GROUP_ID = "group_id";

  public void recordTelemetry() {
    log.info("STOTelemetryPublisher recordTelemetry execute started.");
    try {
      long timestamp = Instant.now().toEpochMilli();
      final Gson gson = new Gson();
      STOUsageReport allUsage =
          gson.fromJson(stoServiceUtils.getUsageAllAcounts(GLOBAL_ACCOUNT_ID, timestamp), STOUsageReport.class);
      log.info("Size of the account list is {} ", allUsage.usage.size());

      for (STOUsage usage : allUsage.usage) {
        if (EmptyPredicate.isNotEmpty(usage.accountId) && !usage.accountId.equals(GLOBAL_ACCOUNT_ID)) {
          HashMap<String, Object> map = new HashMap<>();
          map.put(GROUP_TYPE, ACCOUNT);
          map.put(GROUP_ID, usage.accountId);
          map.put(ACCOUNT_DEPLOY_TYPE, System.getenv().get(DEPLOY_VERSION));
          map.put(LICENSE_USAGE, max(ceil(usage.scanCount / 100.0), usage.developerCount));
          telemetryReporter.sendGroupEvent(usage.accountId, null, map, Collections.singletonMap(ALL, true),
              TelemetryOption.builder().sendForCommunity(false).build());
          log.info("Scheduled STOTelemetryPublisher event sent! for account {}", usage.accountId);
        }
      }
    } catch (Exception e) {
      log.error("STOTelemetryPublisher recordTelemetry execute failed.", e);
    } finally {
      log.info("STOTelemetryPublisher recordTelemetry execute finished.");
    }
  }
}

class STOUsageReport {
  int timestamp;
  List<STOUsage> usage;
}
class STOUsage {
  String accountId;
  int developerCount;
  int scanCount;
}
