/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.ClickHouseClusterDataService;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.entities.ClusterDataDetails;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperServiceImpl;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.configuration.DeployMode;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@OwnedBy(HarnessTeam.CE)
@Slf4j
public class DataCheckBigqueryAndTimescaleTasklet implements Tasklet {
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private BigQueryHelperServiceImpl bigQueryHelperService;
  @Autowired private BatchMainConfig config;

  @Autowired private ClickHouseClusterDataService clusterDataService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    final JobConstants jobConstants = new CCMJobConstants(chunkContext);
    ClusterDataDetails clusterDataDetails;
    if (DeployMode.isOnPrem(config.getDeployMode().name()) && config.isClickHouseEnabled()) {
      clusterDataDetails =
          clusterDataService.getClusterDataEntriesDetails(jobConstants.getAccountId(), jobConstants.getJobStartTime());
    } else {
      clusterDataDetails = bigQueryHelperService.getClusterDataDetails(
          jobConstants.getAccountId(), Instant.ofEpochMilli(jobConstants.getJobStartTime()));
    }
    ClusterDataDetails timeScaleClusterData = billingDataService.getTimeScaleClusterData(
        jobConstants.getAccountId(), Instant.ofEpochMilli(jobConstants.getJobStartTime()));

    if (timeScaleClusterData != null && clusterDataDetails != null) {
      log.info("Timescale Billing data entries count {} , Timescale Sum of billing amount: {}",
          timeScaleClusterData.getEntriesCount(), timeScaleClusterData.getBillingAmountSum());
      log.info("Bigquery Billing data entries count {} , Bigquery Sum of billing amount: {}",
          clusterDataDetails.getEntriesCount(), clusterDataDetails.getBillingAmountSum());
      double timeScaleSum = timeScaleClusterData.getBillingAmountSum();
      double bigQuerySum = clusterDataDetails.getBillingAmountSum();
      double differenceSum = Math.abs(timeScaleSum - bigQuerySum);
      if (timeScaleClusterData.getEntriesCount() == clusterDataDetails.getEntriesCount() && differenceSum < 0.1) {
        log.info("Time Scale data matches with big query data");
      } else {
        log.error("TimeScale data doesn't  match with BigQuery data");
      }
    } else if (timeScaleClusterData == null) {
      log.info("Failed to retrieve TimeScale data");
      if (clusterDataDetails == null) {
        log.info("Failed to retrieve BigQuery data");
      } else {
        log.info("Bigquery Billing data entries count {} , Bigquery Sum of billing amount: {}",
            clusterDataDetails.getEntriesCount(), clusterDataDetails.getBillingAmountSum());
      }
    } else {
      log.info("Timescale Billing data entries count {} , Timescale Sum of billing amount: {}",
          timeScaleClusterData.getEntriesCount(), timeScaleClusterData.getBillingAmountSum());
    }
    return null;
  }
}
