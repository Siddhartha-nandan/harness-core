/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Keys;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.PipelineExecutionSummaryCiRecord;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
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
public class PipelineExecutionSummaryCi extends TableImpl<PipelineExecutionSummaryCiRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.pipeline_execution_summary_ci</code>
   */
  public static final PipelineExecutionSummaryCi PIPELINE_EXECUTION_SUMMARY_CI = new PipelineExecutionSummaryCi();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<PipelineExecutionSummaryCiRecord> getRecordType() {
    return PipelineExecutionSummaryCiRecord.class;
  }

  /**
   * The column <code>public.pipeline_execution_summary_ci.id</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> ID =
      createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.accountid</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.orgidentifier</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> ORGIDENTIFIER =
      createField(DSL.name("orgidentifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.projectidentifier</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> PROJECTIDENTIFIER =
      createField(DSL.name("projectidentifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.pipelineidentifier</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> PIPELINEIDENTIFIER =
      createField(DSL.name("pipelineidentifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.name</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> NAME =
      createField(DSL.name("name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.status</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> STATUS =
      createField(DSL.name("status"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_type</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_TYPE =
      createField(DSL.name("moduleinfo_type"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_event</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_EVENT =
      createField(DSL.name("moduleinfo_event"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_author_id</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_AUTHOR_ID =
      createField(DSL.name("moduleinfo_author_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_repository</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_REPOSITORY =
      createField(DSL.name("moduleinfo_repository"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_branch_name</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_BRANCH_NAME =
      createField(DSL.name("moduleinfo_branch_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_branch_commit_id</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_BRANCH_COMMIT_ID =
      createField(DSL.name("moduleinfo_branch_commit_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_branch_commit_message</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> MODULEINFO_BRANCH_COMMIT_MESSAGE =
      createField(DSL.name("moduleinfo_branch_commit_message"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.author_name</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> AUTHOR_NAME =
      createField(DSL.name("author_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.author_avatar</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> AUTHOR_AVATAR =
      createField(DSL.name("author_avatar"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.startts</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, Long> STARTTS =
      createField(DSL.name("startts"), SQLDataType.BIGINT.nullable(false), this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.endts</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, Long> ENDTS =
      createField(DSL.name("endts"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.planexecutionid</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> PLANEXECUTIONID =
      createField(DSL.name("planexecutionid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.errormessage</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> ERRORMESSAGE =
      createField(DSL.name("errormessage"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.trigger_type</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> TRIGGER_TYPE =
      createField(DSL.name("trigger_type"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.source_branch</code>.
   */
  public final TableField<PipelineExecutionSummaryCiRecord, String> SOURCE_BRANCH =
      createField(DSL.name("source_branch"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.pipeline_execution_summary_ci.pr</code>. The Pull Request number
   */
  public final TableField<PipelineExecutionSummaryCiRecord, Integer> PR =
      createField(DSL.name("pr"), SQLDataType.INTEGER, this, "The Pull Request number");

  /**
   * The column <code>public.pipeline_execution_summary_ci.moduleinfo_is_private</code>. Is the cloned repo private
   */
  public final TableField<PipelineExecutionSummaryCiRecord, Boolean> MODULEINFO_IS_PRIVATE = createField(
      DSL.name("moduleinfo_is_private"), SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)),
      this, "Is the cloned repo private");

  private PipelineExecutionSummaryCi(Name alias, Table<PipelineExecutionSummaryCiRecord> aliased) {
    this(alias, aliased, null);
  }

  private PipelineExecutionSummaryCi(
      Name alias, Table<PipelineExecutionSummaryCiRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.pipeline_execution_summary_ci</code> table reference
   */
  public PipelineExecutionSummaryCi(String alias) {
    this(DSL.name(alias), PIPELINE_EXECUTION_SUMMARY_CI);
  }

  /**
   * Create an aliased <code>public.pipeline_execution_summary_ci</code> table reference
   */
  public PipelineExecutionSummaryCi(Name alias) {
    this(alias, PIPELINE_EXECUTION_SUMMARY_CI);
  }

  /**
   * Create a <code>public.pipeline_execution_summary_ci</code> table reference
   */
  public PipelineExecutionSummaryCi() {
    this(DSL.name("pipeline_execution_summary_ci"), null);
  }

  public <O extends Record> PipelineExecutionSummaryCi(
      Table<O> child, ForeignKey<O, PipelineExecutionSummaryCiRecord> key) {
    super(child, key, PIPELINE_EXECUTION_SUMMARY_CI);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.PIPELINE_EXECUTION_SUMMARY_CI_STARTTS_IDX);
  }

  @Override
  public UniqueKey<PipelineExecutionSummaryCiRecord> getPrimaryKey() {
    return Keys.PIPELINE_EXECUTION_SUMMARY_CI_PKEY;
  }

  @Override
  public List<UniqueKey<PipelineExecutionSummaryCiRecord>> getKeys() {
    return Arrays.<UniqueKey<PipelineExecutionSummaryCiRecord>>asList(Keys.PIPELINE_EXECUTION_SUMMARY_CI_PKEY);
  }

  @Override
  public PipelineExecutionSummaryCi as(String alias) {
    return new PipelineExecutionSummaryCi(DSL.name(alias), this);
  }

  @Override
  public PipelineExecutionSummaryCi as(Name alias) {
    return new PipelineExecutionSummaryCi(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public PipelineExecutionSummaryCi rename(String name) {
    return new PipelineExecutionSummaryCi(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public PipelineExecutionSummaryCi rename(Name name) {
    return new PipelineExecutionSummaryCi(name, null);
  }
}
