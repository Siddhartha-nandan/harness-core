/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration.timescale;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

@CodePulse(
    module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PLG_LICENSING})
@OwnedBy(CDP)
public class CreateServicesLicenseDailyReport extends NGAbstractTimeScaleMigration {
  private static final String SERVICES_LICENSE_DAILY_REPORT_TABLE_SQL_FILE =
      "timescale/create_services_license_daily_report.sql";

  @Override
  public String getFileName() {
    return SERVICES_LICENSE_DAILY_REPORT_TABLE_SQL_FILE;
  }
}
