/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ServicesLicenseDailyReport implements Serializable {
  private static final long serialVersionUID = 1L;

  private String accountId;
  private LocalDate reportedDay;
  private Integer licenseCount;

  public ServicesLicenseDailyReport() {}

  public ServicesLicenseDailyReport(ServicesLicenseDailyReport value) {
    this.accountId = value.accountId;
    this.reportedDay = value.reportedDay;
    this.licenseCount = value.licenseCount;
  }

  public ServicesLicenseDailyReport(String accountId, LocalDate reportedDay, Integer licenseCount) {
    this.accountId = accountId;
    this.reportedDay = reportedDay;
    this.licenseCount = licenseCount;
  }

  /**
   * Getter for <code>public.services_license_daily_report.account_id</code>.
   */
  public String getAccountId() {
    return this.accountId;
  }

  /**
   * Setter for <code>public.services_license_daily_report.account_id</code>.
   */
  public ServicesLicenseDailyReport setAccountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  /**
   * Getter for <code>public.services_license_daily_report.reported_day</code>.
   */
  public LocalDate getReportedDay() {
    return this.reportedDay;
  }

  /**
   * Setter for <code>public.services_license_daily_report.reported_day</code>.
   */
  public ServicesLicenseDailyReport setReportedDay(LocalDate reportedDay) {
    this.reportedDay = reportedDay;
    return this;
  }

  /**
   * Getter for <code>public.services_license_daily_report.license_count</code>.
   */
  public Integer getLicenseCount() {
    return this.licenseCount;
  }

  /**
   * Setter for <code>public.services_license_daily_report.license_count</code>.
   */
  public ServicesLicenseDailyReport setLicenseCount(Integer licenseCount) {
    this.licenseCount = licenseCount;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ServicesLicenseDailyReport other = (ServicesLicenseDailyReport) obj;
    if (accountId == null) {
      if (other.accountId != null)
        return false;
    } else if (!accountId.equals(other.accountId))
      return false;
    if (reportedDay == null) {
      if (other.reportedDay != null)
        return false;
    } else if (!reportedDay.equals(other.reportedDay))
      return false;
    if (licenseCount == null) {
      if (other.licenseCount != null)
        return false;
    } else if (!licenseCount.equals(other.licenseCount))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.accountId == null) ? 0 : this.accountId.hashCode());
    result = prime * result + ((this.reportedDay == null) ? 0 : this.reportedDay.hashCode());
    result = prime * result + ((this.licenseCount == null) ? 0 : this.licenseCount.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ServicesLicenseDailyReport (");

    sb.append(accountId);
    sb.append(", ").append(reportedDay);
    sb.append(", ").append(licenseCount);

    sb.append(")");
    return sb.toString();
  }
}
