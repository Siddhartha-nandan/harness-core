/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.UtilizationData;

import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.Record20;
import org.jooq.Row20;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class UtilizationDataRecord extends TableRecordImpl<UtilizationDataRecord>
    implements Record20<OffsetDateTime, OffsetDateTime, String, String, String, String, Double, Double, Double, Double,
        Double, Double, Double, Double, String, Double, Double, Double, Double, Double> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.utilization_data.starttime</code>.
   */
  public UtilizationDataRecord setStarttime(OffsetDateTime value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return (OffsetDateTime) get(0);
  }

  /**
   * Setter for <code>public.utilization_data.endtime</code>.
   */
  public UtilizationDataRecord setEndtime(OffsetDateTime value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.endtime</code>.
   */
  public OffsetDateTime getEndtime() {
    return (OffsetDateTime) get(1);
  }

  /**
   * Setter for <code>public.utilization_data.accountid</code>.
   */
  public UtilizationDataRecord setAccountid(String value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.utilization_data.settingid</code>.
   */
  public UtilizationDataRecord setSettingid(String value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.settingid</code>.
   */
  public String getSettingid() {
    return (String) get(3);
  }

  /**
   * Setter for <code>public.utilization_data.instanceid</code>.
   */
  public UtilizationDataRecord setInstanceid(String value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.instanceid</code>.
   */
  public String getInstanceid() {
    return (String) get(4);
  }

  /**
   * Setter for <code>public.utilization_data.instancetype</code>.
   */
  public UtilizationDataRecord setInstancetype(String value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.instancetype</code>.
   */
  public String getInstancetype() {
    return (String) get(5);
  }

  /**
   * Setter for <code>public.utilization_data.maxcpu</code>.
   */
  public UtilizationDataRecord setMaxcpu(Double value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxcpu</code>.
   */
  public Double getMaxcpu() {
    return (Double) get(6);
  }

  /**
   * Setter for <code>public.utilization_data.maxmemory</code>.
   */
  public UtilizationDataRecord setMaxmemory(Double value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxmemory</code>.
   */
  public Double getMaxmemory() {
    return (Double) get(7);
  }

  /**
   * Setter for <code>public.utilization_data.avgcpu</code>.
   */
  public UtilizationDataRecord setAvgcpu(Double value) {
    set(8, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgcpu</code>.
   */
  public Double getAvgcpu() {
    return (Double) get(8);
  }

  /**
   * Setter for <code>public.utilization_data.avgmemory</code>.
   */
  public UtilizationDataRecord setAvgmemory(Double value) {
    set(9, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgmemory</code>.
   */
  public Double getAvgmemory() {
    return (Double) get(9);
  }

  /**
   * Setter for <code>public.utilization_data.maxcpuvalue</code>.
   */
  public UtilizationDataRecord setMaxcpuvalue(Double value) {
    set(10, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxcpuvalue</code>.
   */
  public Double getMaxcpuvalue() {
    return (Double) get(10);
  }

  /**
   * Setter for <code>public.utilization_data.maxmemoryvalue</code>.
   */
  public UtilizationDataRecord setMaxmemoryvalue(Double value) {
    set(11, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxmemoryvalue</code>.
   */
  public Double getMaxmemoryvalue() {
    return (Double) get(11);
  }

  /**
   * Setter for <code>public.utilization_data.avgcpuvalue</code>.
   */
  public UtilizationDataRecord setAvgcpuvalue(Double value) {
    set(12, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgcpuvalue</code>.
   */
  public Double getAvgcpuvalue() {
    return (Double) get(12);
  }

  /**
   * Setter for <code>public.utilization_data.avgmemoryvalue</code>.
   */
  public UtilizationDataRecord setAvgmemoryvalue(Double value) {
    set(13, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgmemoryvalue</code>.
   */
  public Double getAvgmemoryvalue() {
    return (Double) get(13);
  }

  /**
   * Setter for <code>public.utilization_data.clusterid</code>.
   */
  public UtilizationDataRecord setClusterid(String value) {
    set(14, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.clusterid</code>.
   */
  public String getClusterid() {
    return (String) get(14);
  }

  /**
   * Setter for <code>public.utilization_data.avgstoragerequestvalue</code>.
   */
  public UtilizationDataRecord setAvgstoragerequestvalue(Double value) {
    set(15, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgstoragerequestvalue</code>.
   */
  public Double getAvgstoragerequestvalue() {
    return (Double) get(15);
  }

  /**
   * Setter for <code>public.utilization_data.avgstorageusagevalue</code>.
   */
  public UtilizationDataRecord setAvgstorageusagevalue(Double value) {
    set(16, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgstorageusagevalue</code>.
   */
  public Double getAvgstorageusagevalue() {
    return (Double) get(16);
  }

  /**
   * Setter for <code>public.utilization_data.avgstoragecapacityvalue</code>.
   */
  public UtilizationDataRecord setAvgstoragecapacityvalue(Double value) {
    set(17, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.avgstoragecapacityvalue</code>.
   */
  public Double getAvgstoragecapacityvalue() {
    return (Double) get(17);
  }

  /**
   * Setter for <code>public.utilization_data.maxstoragerequestvalue</code>.
   */
  public UtilizationDataRecord setMaxstoragerequestvalue(Double value) {
    set(18, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxstoragerequestvalue</code>.
   */
  public Double getMaxstoragerequestvalue() {
    return (Double) get(18);
  }

  /**
   * Setter for <code>public.utilization_data.maxstorageusagevalue</code>.
   */
  public UtilizationDataRecord setMaxstorageusagevalue(Double value) {
    set(19, value);
    return this;
  }

  /**
   * Getter for <code>public.utilization_data.maxstorageusagevalue</code>.
   */
  public Double getMaxstorageusagevalue() {
    return (Double) get(19);
  }

  // -------------------------------------------------------------------------
  // Record20 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row20<OffsetDateTime, OffsetDateTime, String, String, String, String, Double, Double, Double, Double, Double,
      Double, Double, Double, String, Double, Double, Double, Double, Double>
  fieldsRow() {
    return (Row20) super.fieldsRow();
  }

  @Override
  public Row20<OffsetDateTime, OffsetDateTime, String, String, String, String, Double, Double, Double, Double, Double,
      Double, Double, Double, String, Double, Double, Double, Double, Double>
  valuesRow() {
    return (Row20) super.valuesRow();
  }

  @Override
  public Field<OffsetDateTime> field1() {
    return UtilizationData.UTILIZATION_DATA.STARTTIME;
  }

  @Override
  public Field<OffsetDateTime> field2() {
    return UtilizationData.UTILIZATION_DATA.ENDTIME;
  }

  @Override
  public Field<String> field3() {
    return UtilizationData.UTILIZATION_DATA.ACCOUNTID;
  }

  @Override
  public Field<String> field4() {
    return UtilizationData.UTILIZATION_DATA.SETTINGID;
  }

  @Override
  public Field<String> field5() {
    return UtilizationData.UTILIZATION_DATA.INSTANCEID;
  }

  @Override
  public Field<String> field6() {
    return UtilizationData.UTILIZATION_DATA.INSTANCETYPE;
  }

  @Override
  public Field<Double> field7() {
    return UtilizationData.UTILIZATION_DATA.MAXCPU;
  }

  @Override
  public Field<Double> field8() {
    return UtilizationData.UTILIZATION_DATA.MAXMEMORY;
  }

  @Override
  public Field<Double> field9() {
    return UtilizationData.UTILIZATION_DATA.AVGCPU;
  }

  @Override
  public Field<Double> field10() {
    return UtilizationData.UTILIZATION_DATA.AVGMEMORY;
  }

  @Override
  public Field<Double> field11() {
    return UtilizationData.UTILIZATION_DATA.MAXCPUVALUE;
  }

  @Override
  public Field<Double> field12() {
    return UtilizationData.UTILIZATION_DATA.MAXMEMORYVALUE;
  }

  @Override
  public Field<Double> field13() {
    return UtilizationData.UTILIZATION_DATA.AVGCPUVALUE;
  }

  @Override
  public Field<Double> field14() {
    return UtilizationData.UTILIZATION_DATA.AVGMEMORYVALUE;
  }

  @Override
  public Field<String> field15() {
    return UtilizationData.UTILIZATION_DATA.CLUSTERID;
  }

  @Override
  public Field<Double> field16() {
    return UtilizationData.UTILIZATION_DATA.AVGSTORAGEREQUESTVALUE;
  }

  @Override
  public Field<Double> field17() {
    return UtilizationData.UTILIZATION_DATA.AVGSTORAGEUSAGEVALUE;
  }

  @Override
  public Field<Double> field18() {
    return UtilizationData.UTILIZATION_DATA.AVGSTORAGECAPACITYVALUE;
  }

  @Override
  public Field<Double> field19() {
    return UtilizationData.UTILIZATION_DATA.MAXSTORAGEREQUESTVALUE;
  }

  @Override
  public Field<Double> field20() {
    return UtilizationData.UTILIZATION_DATA.MAXSTORAGEUSAGEVALUE;
  }

  @Override
  public OffsetDateTime component1() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime component2() {
    return getEndtime();
  }

  @Override
  public String component3() {
    return getAccountid();
  }

  @Override
  public String component4() {
    return getSettingid();
  }

  @Override
  public String component5() {
    return getInstanceid();
  }

  @Override
  public String component6() {
    return getInstancetype();
  }

  @Override
  public Double component7() {
    return getMaxcpu();
  }

  @Override
  public Double component8() {
    return getMaxmemory();
  }

  @Override
  public Double component9() {
    return getAvgcpu();
  }

  @Override
  public Double component10() {
    return getAvgmemory();
  }

  @Override
  public Double component11() {
    return getMaxcpuvalue();
  }

  @Override
  public Double component12() {
    return getMaxmemoryvalue();
  }

  @Override
  public Double component13() {
    return getAvgcpuvalue();
  }

  @Override
  public Double component14() {
    return getAvgmemoryvalue();
  }

  @Override
  public String component15() {
    return getClusterid();
  }

  @Override
  public Double component16() {
    return getAvgstoragerequestvalue();
  }

  @Override
  public Double component17() {
    return getAvgstorageusagevalue();
  }

  @Override
  public Double component18() {
    return getAvgstoragecapacityvalue();
  }

  @Override
  public Double component19() {
    return getMaxstoragerequestvalue();
  }

  @Override
  public Double component20() {
    return getMaxstorageusagevalue();
  }

  @Override
  public OffsetDateTime value1() {
    return getStarttime();
  }

  @Override
  public OffsetDateTime value2() {
    return getEndtime();
  }

  @Override
  public String value3() {
    return getAccountid();
  }

  @Override
  public String value4() {
    return getSettingid();
  }

  @Override
  public String value5() {
    return getInstanceid();
  }

  @Override
  public String value6() {
    return getInstancetype();
  }

  @Override
  public Double value7() {
    return getMaxcpu();
  }

  @Override
  public Double value8() {
    return getMaxmemory();
  }

  @Override
  public Double value9() {
    return getAvgcpu();
  }

  @Override
  public Double value10() {
    return getAvgmemory();
  }

  @Override
  public Double value11() {
    return getMaxcpuvalue();
  }

  @Override
  public Double value12() {
    return getMaxmemoryvalue();
  }

  @Override
  public Double value13() {
    return getAvgcpuvalue();
  }

  @Override
  public Double value14() {
    return getAvgmemoryvalue();
  }

  @Override
  public String value15() {
    return getClusterid();
  }

  @Override
  public Double value16() {
    return getAvgstoragerequestvalue();
  }

  @Override
  public Double value17() {
    return getAvgstorageusagevalue();
  }

  @Override
  public Double value18() {
    return getAvgstoragecapacityvalue();
  }

  @Override
  public Double value19() {
    return getMaxstoragerequestvalue();
  }

  @Override
  public Double value20() {
    return getMaxstorageusagevalue();
  }

  @Override
  public UtilizationDataRecord value1(OffsetDateTime value) {
    setStarttime(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value2(OffsetDateTime value) {
    setEndtime(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value3(String value) {
    setAccountid(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value4(String value) {
    setSettingid(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value5(String value) {
    setInstanceid(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value6(String value) {
    setInstancetype(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value7(Double value) {
    setMaxcpu(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value8(Double value) {
    setMaxmemory(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value9(Double value) {
    setAvgcpu(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value10(Double value) {
    setAvgmemory(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value11(Double value) {
    setMaxcpuvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value12(Double value) {
    setMaxmemoryvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value13(Double value) {
    setAvgcpuvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value14(Double value) {
    setAvgmemoryvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value15(String value) {
    setClusterid(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value16(Double value) {
    setAvgstoragerequestvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value17(Double value) {
    setAvgstorageusagevalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value18(Double value) {
    setAvgstoragecapacityvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value19(Double value) {
    setMaxstoragerequestvalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord value20(Double value) {
    setMaxstorageusagevalue(value);
    return this;
  }

  @Override
  public UtilizationDataRecord values(OffsetDateTime value1, OffsetDateTime value2, String value3, String value4,
      String value5, String value6, Double value7, Double value8, Double value9, Double value10, Double value11,
      Double value12, Double value13, Double value14, String value15, Double value16, Double value17, Double value18,
      Double value19, Double value20) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    value6(value6);
    value7(value7);
    value8(value8);
    value9(value9);
    value10(value10);
    value11(value11);
    value12(value12);
    value13(value13);
    value14(value14);
    value15(value15);
    value16(value16);
    value17(value17);
    value18(value18);
    value19(value19);
    value20(value20);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached UtilizationDataRecord
   */
  public UtilizationDataRecord() {
    super(UtilizationData.UTILIZATION_DATA);
  }

  /**
   * Create a detached, initialised UtilizationDataRecord
   */
  public UtilizationDataRecord(OffsetDateTime starttime, OffsetDateTime endtime, String accountid, String settingid,
      String instanceid, String instancetype, Double maxcpu, Double maxmemory, Double avgcpu, Double avgmemory,
      Double maxcpuvalue, Double maxmemoryvalue, Double avgcpuvalue, Double avgmemoryvalue, String clusterid,
      Double avgstoragerequestvalue, Double avgstorageusagevalue, Double avgstoragecapacityvalue,
      Double maxstoragerequestvalue, Double maxstorageusagevalue) {
    super(UtilizationData.UTILIZATION_DATA);

    setStarttime(starttime);
    setEndtime(endtime);
    setAccountid(accountid);
    setSettingid(settingid);
    setInstanceid(instanceid);
    setInstancetype(instancetype);
    setMaxcpu(maxcpu);
    setMaxmemory(maxmemory);
    setAvgcpu(avgcpu);
    setAvgmemory(avgmemory);
    setMaxcpuvalue(maxcpuvalue);
    setMaxmemoryvalue(maxmemoryvalue);
    setAvgcpuvalue(avgcpuvalue);
    setAvgmemoryvalue(avgmemoryvalue);
    setClusterid(clusterid);
    setAvgstoragerequestvalue(avgstoragerequestvalue);
    setAvgstorageusagevalue(avgstorageusagevalue);
    setAvgstoragecapacityvalue(avgstoragecapacityvalue);
    setMaxstoragerequestvalue(maxstoragerequestvalue);
    setMaxstorageusagevalue(maxstorageusagevalue);
  }
}
