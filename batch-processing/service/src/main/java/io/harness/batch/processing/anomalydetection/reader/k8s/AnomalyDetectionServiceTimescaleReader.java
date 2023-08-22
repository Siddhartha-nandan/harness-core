/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.anomalydetection.reader.k8s;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.K8sQueryMetaData;
import io.harness.batch.processing.anomalydetection.TimeSeriesMetaData;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.tasklet.support.HarnessEntitiesService;
import io.harness.ccm.anomaly.entities.EntityType;
import io.harness.ccm.anomaly.entities.TimeGranularity;

import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AnomalyDetectionServiceTimescaleReader extends AnomalyDetectionTimescaleReader {
  @Autowired HarnessEntitiesService harnessEntitiesService;
  @Override
  public void beforeStep(StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
    accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant endTime = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);

    List<DbColumn> selectColumns = new ArrayList<>();
    List<QLCCMAggregationFunction> aggregationList = new ArrayList<>();
    List<QLBillingDataFilter> filtersList = new ArrayList<>();
    List<QLCCMEntityGroupBy> groupByList = new ArrayList<>();
    List<QLBillingSortCriteria> sortCriteria = new ArrayList<>();

    K8sQueryMetaData k8sQueryMetaData = K8sQueryMetaData.builder()
                                            .accountId(accountId)
                                            .filtersList(filtersList)
                                            .aggregationList(aggregationList)
                                            .sortCriteria(sortCriteria)
                                            .groupByList(groupByList)
                                            .selectColumns(selectColumns)
                                            .build();

    TimeSeriesMetaData timeSeriesMetaData =
        TimeSeriesMetaData.builder()
            .accountId(accountId)
            .trainStart(endTime.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS))
            .trainEnd(endTime.minus(1, ChronoUnit.DAYS))
            .testStart(endTime.minus(1, ChronoUnit.DAYS))
            .testEnd(endTime)
            .timeGranularity(TimeGranularity.DAILY)
            .entityType(EntityType.SERVICE)
            .k8sQueryMetaData(k8sQueryMetaData)
            .build();

    log.info("train start time : {}", endTime.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS));
    log.info("train end time : {}", endTime.minus(1, ChronoUnit.DAYS));
    log.info("test start time : {}", endTime.minus(1, ChronoUnit.DAYS));
    log.info("test end time : {}", endTime);

    // cost aggreation
    aggregationList.add(QLCCMAggregationFunction.builder()
                            .operationType(QLCCMAggregateOperation.SUM)
                            .columnName(tableSchema.getBillingAmount().getColumnNameSQL())
                            .build());

    // filters
    // Start Time
    filtersList.add(QLBillingDataFilter.builder()
                        .startTime(QLTimeFilter.builder()
                                       .operator(QLTimeOperator.AFTER)
                                       .value(timeSeriesMetaData.getTrainStart().toEpochMilli())
                                       .build())
                        .build());

    // End Time
    filtersList.add(QLBillingDataFilter.builder()
                        .endTime(QLTimeFilter.builder()
                                     .operator(QLTimeOperator.BEFORE)
                                     .value(timeSeriesMetaData.getTestEnd().toEpochMilli())
                                     .build())
                        .build());

    List<String> instanceType = new ArrayList<>();
    instanceType.add("K8S_POD");
    // TODO add fargate here
    filtersList.add(
        QLBillingDataFilter.builder()
            .instanceType(
                QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(instanceType.toArray(new String[0])).build())
            .build());

    // groupby
    groupByList.add(QLCCMEntityGroupBy.Cluster);
    groupByList.add(QLCCMEntityGroupBy.ClusterName);
    groupByList.add(QLCCMEntityGroupBy.Service);
    groupByList.add(QLCCMEntityGroupBy.StartTime);

    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Cluster).sortOrder(QLSortOrder.ASCENDING).build());
    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Service).sortOrder(QLSortOrder.ASCENDING).build());
    sortCriteria.add(
        QLBillingSortCriteria.builder().sortType(QLBillingSortType.Time).sortOrder(QLSortOrder.ASCENDING).build());

    log.info("Anomaly Detection batch job of type : {} , time granularity : {}, for accountId : {} , endtime : {}",
        EntityType.CLUSTER.toString(), TimeGranularity.DAILY.toString(), accountId, endTime.toString());

    listAnomalyDetectionTimeSeries = dataService.readData(timeSeriesMetaData);

    for (AnomalyDetectionTimeSeries anomalyDetectionTimeSeries : listAnomalyDetectionTimeSeries) {
      String servicename = harnessEntitiesService.fetchEntityName(
          HarnessEntitiesService.HarnessEntities.SERVICE, anomalyDetectionTimeSeries.getService());

      log.info("The service name is {}", servicename);

      anomalyDetectionTimeSeries.setServiceName(servicename);

      log.info("Yes the service name is finally available and is {}", anomalyDetectionTimeSeries.getServiceName());
    }

    log.info("successfully read {} no of {}", listAnomalyDetectionTimeSeries.size(),
        timeSeriesMetaData.getEntityType().toString());
    timeSeriesIndex = 0;
  }
}
