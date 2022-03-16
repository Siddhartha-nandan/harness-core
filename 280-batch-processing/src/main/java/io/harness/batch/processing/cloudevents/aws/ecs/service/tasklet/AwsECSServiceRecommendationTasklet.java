/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.ClusterIdAndServiceArn;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.UtilizationDataWithTime;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.dao.recommendation.ECSRecommendationDAO;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.kubernetes.client.custom.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.convertToReadableForm;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.makeResourceMap;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Optional.ofNullable;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.*;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.CPU;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.MEMORY;

@Slf4j
@Singleton
public class AwsECSServiceRecommendationTasklet implements Tasklet {
  @Autowired private CEClusterDao ceClusterDao;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  @Autowired private ECSRecommendationDAO ecsRecommendationDAO;
  @Autowired private BillingDataServiceImpl billingDataService;

  private static final int BATCH_SIZE = 20;
  private static final int AVG_UTILIZATION_WEIGHT = 2;
  private static final int RECOMMENDATION_FOR_DAYS = 7;
  private static final Set<Integer> requiredPercentiles = ImmutableSet.of(50, 80, 90, 95, 99);
  private static final String PERCENTILE_KEY = "p%d";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    final JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Instant startTime = Instant.ofEpochMilli(jobConstants.getJobStartTime());
    Instant endTime = Instant.ofEpochMilli(jobConstants.getJobEndTime());
    // Get all clusters for current account
    Map<String, String> ceClusters = ceClusterDao.getClusterIdNameMapping(accountId);
    if (CollectionUtils.isEmpty(ceClusters)) {
      return null;
    }
    List<String> clusterIds = new ArrayList<>(ceClusters.keySet());

    for (List<String> ceClustersPartition: Lists.partition(clusterIds, BATCH_SIZE)) {
      // Get utilization data for all clusters in this batch for a day
      Map<ClusterIdAndServiceArn, List<UtilizationDataWithTime>> utilMap =
          utilizationDataService.getUtilizationDataForECSClusters(accountId, ceClustersPartition,
              startTime.toString(), endTime.toString());

      for (ClusterIdAndServiceArn clusterIdAndServiceArn : utilMap.keySet()) {
        // Get service resource details cpuUnits and memoryMb
        int cpuUnits = 2048;
        int memoryMb = 7764;
        List<UtilizationDataWithTime> utilData = utilMap.get(clusterIdAndServiceArn);
        Instant today = Instant.ofEpochMilli(jobConstants.getJobStartTime());
        String clusterId = clusterIdAndServiceArn.getClusterId();
        String clusterName = ceClusters.get(clusterId);
        String serviceArn = clusterIdAndServiceArn.getServiceArn();
        String serviceName = serviceNameFromServiceArn(serviceArn);
        // 3. Create Partial Histogram for a day for this service
        ECSPartialRecommendationHistogram partialRecommendationHistogram = ECSPartialRecommendationHistogram.builder()
            .accountId(accountId)
            .clusterId(clusterId)
            .clusterName(clusterName)
            .serviceArn(serviceArn)
            .serviceName(serviceName)
            .date(today)
            .lastUpdateTime(today)
            .cpuHistogram(cpuHistogramCheckpointFromUtilData(utilData, cpuUnits))
            .memoryHistogram(memoryHistogramCheckpointFromUtilData(utilData, memoryMb))
            .firstSampleStart(utilData.isEmpty() ? null : utilData.get(0).getStartTime())
            .lastSampleStart(utilData.isEmpty() ? null : utilData.get(utilData.size() - 1).getStartTime())
            .totalSamplesCount(utilData.size())
            .windowEnd(utilData.isEmpty() ? null : utilData.get(utilData.size()-1).getEndTime())
            .version(1)
            .build();
        ecsRecommendationDAO.savePartialRecommendation(partialRecommendationHistogram);

        // 4. Get Partial recommendations for last 6 days
        List<ECSPartialRecommendationHistogram> partialHistograms =
            ecsRecommendationDAO.fetchPartialRecommendationHistograms(accountId, clusterId, clusterName, serviceName,
                serviceArn, today.minus(Duration.ofDays(RECOMMENDATION_FOR_DAYS + 1)), today.minusSeconds(1));
        // add today's histogram to the list
        partialHistograms.add(partialRecommendationHistogram);

        // 5. Create ECSServiceRecommendation
        ECSServiceRecommendation recommendation =
            ecsRecommendationDAO.fetchServiceRecommendation(accountId, clusterId, clusterName, serviceName, serviceArn);
        Histogram cpuHistogram = newCpuHistogramV2();
        Histogram memoryHistogram = newMemoryHistogramV2();
        Instant firstSampleStart = Instant.now();
        Instant lastSampleStart = Instant.EPOCH;
        Instant windowEnd = Instant.EPOCH;
        int totalSamplesCount = 0;
        long memoryPeak = 0;
        for (ECSPartialRecommendationHistogram partialHistogram: partialHistograms) {
          cpuHistogram.merge(loadFromCheckpointV2(partialHistogram.getCpuHistogram()));
          Histogram partialMemoryHistogram = newMemoryHistogramV2();
          partialMemoryHistogram.loadFromCheckPoint(partialHistogram.getMemoryHistogram());
          memoryHistogram.merge(partialMemoryHistogram);
          if (partialHistogram.getFirstSampleStart().isBefore(firstSampleStart))
            firstSampleStart = partialHistogram.getFirstSampleStart();
          if (partialHistogram.getLastSampleStart().isAfter(lastSampleStart))
            lastSampleStart = partialHistogram.getLastSampleStart();
          if (partialHistogram.getWindowEnd().isAfter(windowEnd))
            windowEnd = partialHistogram.getWindowEnd();
          totalSamplesCount += partialHistogram.getTotalSamplesCount();
          memoryPeak = Math.max(partialHistogram.getMemoryPeak(), memoryPeak);
        }
        recommendation.setCpuHistogram(cpuHistogram.saveToCheckpoint());
        recommendation.setMemoryHistogram(memoryHistogram.saveToCheckpoint());
        recommendation.setCurrentResourceRequirements(
            convertToReadableForm(makeResourceMap(cpuUnits, (long) memoryMb * 1024 * 1024)));
        recommendation.setFirstSampleStart(firstSampleStart);
        recommendation.setLastSampleStart(lastSampleStart);
        recommendation.setWindowEnd(windowEnd);
        recommendation.setTotalSamplesCount(totalSamplesCount);
        recommendation.setMemoryPeak(memoryPeak);
        recommendation.setLastReceivedUtilDataAt(lastSampleStart);
        recommendation.setLastComputedRecommendationAt(today);
        recommendation.setNumDays(partialHistograms.size());

        Map<String, Map<String, String>> computedPercentiles = new HashMap<>();
        for (Integer percentile : requiredPercentiles) {
          computedPercentiles.put(
              String.format(PERCENTILE_KEY, percentile),
              convertToReadableForm(makeResourceMap((long) cpuHistogram.getPercentile(percentile),
                      (long) (memoryHistogram.getPercentile(percentile * (double) 1024 * (double) 1024)))));
        }
        recommendation.setPercentileBasedResourceRecommendation(computedPercentiles);

        Cost lastDayCost = billingDataService.getECSServiceLastAvailableDayCost(accountId, clusterId, serviceName,
            today.minus(Duration.ofDays(RECOMMENDATION_FOR_DAYS + 1)));
        if (lastDayCost != null) {
          recommendation.setLastDayCost(lastDayCost);
          recommendation.setLastDayCostAvailable(true);
          BigDecimal monthlySavings = estimateMonthlySavings(recommendation.getCurrentResourceRequirements(),
              recommendation.getPercentileBasedResourceRecommendation().get(String.format(PERCENTILE_KEY, 90)),
              lastDayCost);
          recommendation.setEstimatedSavings(monthlySavings);
        } else {
          recommendation.setLastDayCostAvailable(false);
          log.debug("Unable to get lastDayCost for serviceArn: {}", serviceArn);
        }

        ecsRecommendationDAO.saveRecommendation(recommendation);
      }
    }

    return null;
  }

  BigDecimal estimateMonthlySavings(Map<String, String> current, Map<String, String> recommendation, Cost lastDayCost) {
    BigDecimal cpuChangePercent = resourceChangePercent(current, recommendation, CPU);
    BigDecimal memoryChangePercent = resourceChangePercent(current, recommendation, MEMORY);
    return getMonthlySavings(lastDayCost, cpuChangePercent, memoryChangePercent);
  }

  public static BigDecimal getMonthlySavings(Cost lastDayCost, BigDecimal cpuChangePercent, BigDecimal memoryChangePercent) {
    BigDecimal monthlySavings = null;
    if (cpuChangePercent != null || memoryChangePercent != null) {
      BigDecimal costChangeForDay = BigDecimal.ZERO;
      if (cpuChangePercent != null && lastDayCost.getCpu() != null) {
        costChangeForDay = costChangeForDay.add(cpuChangePercent.multiply(lastDayCost.getCpu()));
      }
      if (memoryChangePercent != null && lastDayCost.getMemory() != null) {
        costChangeForDay = costChangeForDay.add(memoryChangePercent.multiply(lastDayCost.getMemory()));
      }
      monthlySavings = costChangeForDay.multiply(BigDecimal.valueOf(-30)).setScale(2, HALF_UP);
    }
    return monthlySavings;
  }

  static BigDecimal resourceChangePercent(Map<String, String> current,  Map<String, String> recommendation, String resource) {
    BigDecimal currentValue = getResourceValue(current, resource, BigDecimal.ZERO);
    BigDecimal recommendedValue = getResourceValue(recommendation, resource, BigDecimal.ZERO);
    if (currentValue.compareTo(BigDecimal.ZERO) != 0) {
      BigDecimal change = recommendedValue.subtract(currentValue);
      return change.setScale(3, HALF_UP).divide(currentValue, HALF_UP);
    }
    return null;
  }

  static BigDecimal getResourceValue(Map<String, String> resourceRequirement, String resource, BigDecimal defaultValue) {
    return ofNullable(resourceRequirement)
        .map(r -> r.get(resource))
        .map(Quantity::fromString)
        .map(Quantity::getNumber)
        .orElse(defaultValue);
  }

  private HistogramCheckpoint cpuHistogramCheckpointFromUtilData
      (List<UtilizationDataWithTime> utilizationForDay, int cpuUnits) {
    Histogram histogram = cpuHistogramFromUtilData(utilizationForDay, cpuUnits);
    return histogram.saveToCheckpoint();
  }

  private HistogramCheckpoint memoryHistogramCheckpointFromUtilData
      (List<UtilizationDataWithTime> utilizationForDay, int memoryMb) {
    Histogram histogram = memoryHistogramFromUtilData(utilizationForDay, memoryMb);
    return histogram.saveToCheckpoint();
  }

  private Histogram cpuHistogramFromUtilData(List<UtilizationDataWithTime> utilizationForDay, int cpuUnits) {
    Histogram histogram = newCpuHistogramV2();
    // utilization data is in percentage
    for (UtilizationDataWithTime utilizationForHour: utilizationForDay) {
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgCpuUtilization() * cpuUnits, 2,
          utilizationForHour.getStartTime());
      histogram.addSample(utilizationForHour.getUtilizationData().getMaxCpuUtilization() * cpuUnits, 1,
          utilizationForHour.getStartTime());
    }
    return histogram;
  }

  private Histogram memoryHistogramFromUtilData(List<UtilizationDataWithTime> utilizationForDay, int memoryMb) {
    Histogram histogram = newMemoryHistogramV2();
    // utilization data is in percentage
    for (UtilizationDataWithTime utilizationForHour: utilizationForDay) {
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgMemoryUtilization() * memoryMb, 2,
          utilizationForHour.getStartTime());
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgMemoryUtilization() * memoryMb, 1,
          utilizationForHour.getStartTime());
    }
    return histogram;
  }

  private String serviceNameFromServiceArn(String serviceArn) {
    return serviceArn.substring(serviceArn.lastIndexOf('/') + 1);
  }
}
