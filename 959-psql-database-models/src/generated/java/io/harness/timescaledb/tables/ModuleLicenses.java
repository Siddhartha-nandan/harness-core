/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Keys;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.ModuleLicensesRecord;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ModuleLicenses extends TableImpl<ModuleLicensesRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.module_licenses</code>
   */
  public static final ModuleLicenses MODULE_LICENSES = new ModuleLicenses();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<ModuleLicensesRecord> getRecordType() {
    return ModuleLicensesRecord.class;
  }

  /**
   * The column <code>public.module_licenses.id</code>.
   */
  public final TableField<ModuleLicensesRecord, String> ID =
      createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.module_licenses.account_identifier</code>.
   */
  public final TableField<ModuleLicensesRecord, String> ACCOUNT_IDENTIFIER =
      createField(DSL.name("account_identifier"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.module_licenses.module_type</code>.
   */
  public final TableField<ModuleLicensesRecord, String> MODULE_TYPE =
      createField(DSL.name("module_type"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.module_licenses.edition</code>.
   */
  public final TableField<ModuleLicensesRecord, String> EDITION =
      createField(DSL.name("edition"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.module_licenses.license_type</code>.
   */
  public final TableField<ModuleLicensesRecord, String> LICENSE_TYPE =
      createField(DSL.name("license_type"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.module_licenses.expiry_time</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> EXPIRY_TIME =
      createField(DSL.name("expiry_time"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.start_time</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> START_TIME =
      createField(DSL.name("start_time"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.premium_support</code>.
   */
  public final TableField<ModuleLicensesRecord, Boolean> PREMIUM_SUPPORT =
      createField(DSL.name("premium_support"), SQLDataType.BOOLEAN, this, "");

  /**
   * The column <code>public.module_licenses.trial_extended</code>.
   */
  public final TableField<ModuleLicensesRecord, Boolean> TRIAL_EXTENDED =
      createField(DSL.name("trial_extended"), SQLDataType.BOOLEAN, this, "");

  /**
   * The column <code>public.module_licenses.self_service</code>.
   */
  public final TableField<ModuleLicensesRecord, Boolean> SELF_SERVICE =
      createField(DSL.name("self_service"), SQLDataType.BOOLEAN, this, "");

  /**
   * The column <code>public.module_licenses.created_at</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CREATED_AT =
      createField(DSL.name("created_at"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.last_updated_at</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> LAST_UPDATED_AT =
      createField(DSL.name("last_updated_at"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.chaos_total_experiment_runs</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CHAOS_TOTAL_EXPERIMENT_RUNS =
      createField(DSL.name("chaos_total_experiment_runs"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.chaos_total_infrastructures</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CHAOS_TOTAL_INFRASTRUCTURES =
      createField(DSL.name("chaos_total_infrastructures"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.cd_license_type</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CD_LICENSE_TYPE =
      createField(DSL.name("cd_license_type"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.cd_workloads</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CD_WORKLOADS =
      createField(DSL.name("cd_workloads"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.cd_service_instances</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CD_SERVICE_INSTANCES =
      createField(DSL.name("cd_service_instances"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.ce_spend_limit</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CE_SPEND_LIMIT =
      createField(DSL.name("ce_spend_limit"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.cf_number_of_users</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CF_NUMBER_OF_USERS =
      createField(DSL.name("cf_number_of_users"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.cf_number_of_client_maus</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CF_NUMBER_OF_CLIENT_MAUS =
      createField(DSL.name("cf_number_of_client_maus"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.ci_number_of_committers</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CI_NUMBER_OF_COMMITTERS =
      createField(DSL.name("ci_number_of_committers"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.ci_cache_allowance</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CI_CACHE_ALLOWANCE =
      createField(DSL.name("ci_cache_allowance"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.ci_hosting_credits</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> CI_HOSTING_CREDITS =
      createField(DSL.name("ci_hosting_credits"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.srm_number_of_services</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> SRM_NUMBER_OF_SERVICES =
      createField(DSL.name("srm_number_of_services"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.sto_number_of_developers</code>.
   */
  public final TableField<ModuleLicensesRecord, Long> STO_NUMBER_OF_DEVELOPERS =
      createField(DSL.name("sto_number_of_developers"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.module_licenses.status</code>.
   */
  public final TableField<ModuleLicensesRecord, String> STATUS =
      createField(DSL.name("status"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.created_by_uuid</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CREATED_BY_UUID =
      createField(DSL.name("created_by_uuid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.created_by_name</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CREATED_BY_NAME =
      createField(DSL.name("created_by_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.created_by_email</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CREATED_BY_EMAIL =
      createField(DSL.name("created_by_email"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.created_by_external_user_id</code>.
   */
  public final TableField<ModuleLicensesRecord, String> CREATED_BY_EXTERNAL_USER_ID =
      createField(DSL.name("created_by_external_user_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.last_updated_by_uuid</code>.
   */
  public final TableField<ModuleLicensesRecord, String> LAST_UPDATED_BY_UUID =
      createField(DSL.name("last_updated_by_uuid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.last_updated_by_name</code>.
   */
  public final TableField<ModuleLicensesRecord, String> LAST_UPDATED_BY_NAME =
      createField(DSL.name("last_updated_by_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.last_updated_by_email</code>.
   */
  public final TableField<ModuleLicensesRecord, String> LAST_UPDATED_BY_EMAIL =
      createField(DSL.name("last_updated_by_email"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.module_licenses.last_updated_by_external_user_id</code>.
   */
  public final TableField<ModuleLicensesRecord, String> LAST_UPDATED_BY_EXTERNAL_USER_ID =
      createField(DSL.name("last_updated_by_external_user_id"), SQLDataType.CLOB, this, "");

  private ModuleLicenses(Name alias, Table<ModuleLicensesRecord> aliased) {
    this(alias, aliased, null);
  }

  private ModuleLicenses(Name alias, Table<ModuleLicensesRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.module_licenses</code> table reference
   */
  public ModuleLicenses(String alias) {
    this(DSL.name(alias), MODULE_LICENSES);
  }

  /**
   * Create an aliased <code>public.module_licenses</code> table reference
   */
  public ModuleLicenses(Name alias) {
    this(alias, MODULE_LICENSES);
  }

  /**
   * Create a <code>public.module_licenses</code> table reference
   */
  public ModuleLicenses() {
    this(DSL.name("module_licenses"), null);
  }

  public <O extends Record> ModuleLicenses(Table<O> child, ForeignKey<O, ModuleLicensesRecord> key) {
    super(child, key, MODULE_LICENSES);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public UniqueKey<ModuleLicensesRecord> getPrimaryKey() {
    return Keys.MODULE_LICENSES_PKEY;
  }

  @Override
  public List<UniqueKey<ModuleLicensesRecord>> getKeys() {
    return Arrays.<UniqueKey<ModuleLicensesRecord>>asList(Keys.MODULE_LICENSES_PKEY);
  }

  @Override
  public ModuleLicenses as(String alias) {
    return new ModuleLicenses(DSL.name(alias), this);
  }

  @Override
  public ModuleLicenses as(Name alias) {
    return new ModuleLicenses(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public ModuleLicenses rename(String name) {
    return new ModuleLicenses(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public ModuleLicenses rename(Name name) {
    return new ModuleLicenses(name, null);
  }
}
