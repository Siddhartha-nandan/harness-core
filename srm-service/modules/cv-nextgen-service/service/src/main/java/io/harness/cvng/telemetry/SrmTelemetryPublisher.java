/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.telemetry;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.telemetry.Destination.ALL;

import io.harness.ModuleType;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.usage.impl.SRMLicenseUsageDTO;
import io.harness.cvng.usage.impl.SRMLicenseUsageImpl;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.remote.client.CGRestUtils;
import io.harness.telemetry.TelemetryOption;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CV)
public class SrmTelemetryPublisher {
  @Inject private SRMLicenseUsageImpl srmLicenseUsageService;
  @Inject private TelemetryReporter telemetryReporter;
  @Inject private AccountClient accountClient;

  private static final String COUNT_ACTIVE_SERVICES_LICENSES = "srm_license_services_monitored";
  // Locking for a bit less than one day. It's ok to send a bit more than less considering downtime/etc
  //(24*60*60*1000)-(10*60*1000)
  private static final long A_DAY_MINUS_TEN_MINS_IN_MILLIS = 85800000;
  private static final String ACCOUNT = "Account";
  private static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  private static final String GROUP_TYPE = "group_type";
  private static final String GROUP_ID = "group_id";

  public void recordTelemetry() {
    log.info("SrmTelemetryPublisher recordTelemetry execute started.");
    try {
      List<AccountDTO> accountDTOList = getAllAccounts();
      for (AccountDTO accountDTO : accountDTOList) {
        String accountId = accountDTO.getIdentifier();
        try {
          sendEvent(accountId);
        } catch (Exception e) {
          log.error("Failed to send telemetry event for account {}", accountId, e);
        }
      }
    } catch (Exception e) {
      log.error("SrmTelemetryPublisher recordTelemetry execute failed.", e);
    } finally {
      log.info("SrmTelemetryPublisher recordTelemetry execute finished.");
    }
  }

  private void sendEvent(String accountId) {
    if (EmptyPredicate.isNotEmpty(accountId) && !accountId.equals(GLOBAL_ACCOUNT_ID)) {
      HashMap<String, Object> map = new HashMap<>();
      map.put(GROUP_TYPE, ACCOUNT);
      map.put(GROUP_ID, accountId);

      SRMLicenseUsageDTO srmLicenseUsageDTO =
          srmLicenseUsageService.getLicenseUsage(accountId, ModuleType.SRM, new Date().getTime(), null);
      map.put(COUNT_ACTIVE_SERVICES_LICENSES, srmLicenseUsageDTO.getActiveServices());

      telemetryReporter.sendGroupEvent(accountId, null, map, Collections.singletonMap(ALL, true),
          TelemetryOption.builder().sendForCommunity(true).build());
      log.info("Scheduled CdTelemetryPublisher event sent for account [{}], with values: [{}]", accountId, map);
    }
  }

  List<AccountDTO> getAllAccounts() {
    return CGRestUtils.getResponse(accountClient.getAllAccounts());
  }
}
