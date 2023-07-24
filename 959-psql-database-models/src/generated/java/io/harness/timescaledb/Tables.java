/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.Anomalies;
import io.harness.timescaledb.tables.BillingData;
import io.harness.timescaledb.tables.CeRecommendations;
import io.harness.timescaledb.tables.Environments;
import io.harness.timescaledb.tables.KubernetesUtilizationData;
import io.harness.timescaledb.tables.ModuleLicenses;
import io.harness.timescaledb.tables.NgInstanceStats;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.NodePoolAggregated;
import io.harness.timescaledb.tables.PipelineExecutionSummary;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCi;
import io.harness.timescaledb.tables.Pipelines;
import io.harness.timescaledb.tables.PodInfo;
import io.harness.timescaledb.tables.ServiceInfraInfo;
import io.harness.timescaledb.tables.ServiceInstancesLicenseDailyReport;
import io.harness.timescaledb.tables.Services;
import io.harness.timescaledb.tables.ServicesLicenseDailyReport;
import io.harness.timescaledb.tables.UtilizationData;
import io.harness.timescaledb.tables.WorkloadInfo;

/**
 * Convenience access to all tables in public.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Tables {
  /**
   * The table <code>public.anomalies</code>.
   */
  public static final Anomalies ANOMALIES = Anomalies.ANOMALIES;

  /**
   * The table <code>public.billing_data</code>.
   */
  public static final BillingData BILLING_DATA = BillingData.BILLING_DATA;

  /**
   * The table <code>public.ce_recommendations</code>.
   */
  public static final CeRecommendations CE_RECOMMENDATIONS = CeRecommendations.CE_RECOMMENDATIONS;

  /**
   * The table <code>public.environments</code>.
   */
  public static final Environments ENVIRONMENTS = Environments.ENVIRONMENTS;

  /**
   * The table <code>public.kubernetes_utilization_data</code>.
   */
  public static final KubernetesUtilizationData KUBERNETES_UTILIZATION_DATA =
      KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA;

  /**
   * The table <code>public.module_licenses</code>.
   */
  public static final ModuleLicenses MODULE_LICENSES = ModuleLicenses.MODULE_LICENSES;

  /**
   * The table <code>public.ng_instance_stats</code>.
   */
  public static final NgInstanceStats NG_INSTANCE_STATS = NgInstanceStats.NG_INSTANCE_STATS;

  /**
   * The table <code>public.node_info</code>.
   */
  public static final NodeInfo NODE_INFO = NodeInfo.NODE_INFO;

  /**
   * The table <code>public.node_pool_aggregated</code>.
   */
  public static final NodePoolAggregated NODE_POOL_AGGREGATED = NodePoolAggregated.NODE_POOL_AGGREGATED;

  /**
   * The table <code>public.pipeline_execution_summary</code>.
   */
  public static final PipelineExecutionSummary PIPELINE_EXECUTION_SUMMARY =
      PipelineExecutionSummary.PIPELINE_EXECUTION_SUMMARY;

  /**
   * The table <code>public.service_instances_license_daily_report</code>.
   */
  public static final ServiceInstancesLicenseDailyReport SERVICE_INSTANCES_LICENSE_DAILY_REPORT =
      ServiceInstancesLicenseDailyReport.SERVICE_INSTANCES_LICENSE_DAILY_REPORT;

  /**
   * The table <code>public.pipeline_execution_summary_cd</code>.
   */
  public static final PipelineExecutionSummaryCd PIPELINE_EXECUTION_SUMMARY_CD =
      PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD;

  /**
   * The table <code>public.services_license_daily_report</code>.
   */
  public static final ServicesLicenseDailyReport SERVICES_LICENSE_DAILY_REPORT =
      ServicesLicenseDailyReport.SERVICES_LICENSE_DAILY_REPORT;

  /**
   * The table <code>public.pipeline_execution_summary_ci</code>.
   */
  public static final PipelineExecutionSummaryCi PIPELINE_EXECUTION_SUMMARY_CI =
      PipelineExecutionSummaryCi.PIPELINE_EXECUTION_SUMMARY_CI;

  /**
   * The table <code>public.pipelines</code>.
   */
  public static final Pipelines PIPELINES = Pipelines.PIPELINES;

  /**
   * The table <code>public.pod_info</code>.
   */
  public static final PodInfo POD_INFO = PodInfo.POD_INFO;

  /**
   * The table <code>public.service_infra_info</code>.
   */
  public static final ServiceInfraInfo SERVICE_INFRA_INFO = ServiceInfraInfo.SERVICE_INFRA_INFO;

  /**
   * The table <code>public.services</code>.
   */
  public static final Services SERVICES = Services.SERVICES;

  /**
   * The table <code>public.utilization_data</code>.
   */
  public static final UtilizationData UTILIZATION_DATA = UtilizationData.UTILIZATION_DATA;

  /**
   * The table <code>public.workload_info</code>.
   */
  public static final WorkloadInfo WORKLOAD_INFO = WorkloadInfo.WORKLOAD_INFO;
}
