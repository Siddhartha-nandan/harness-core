/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.views.helper;

import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_ACCOUNT_FIELD;
import static io.harness.ccm.views.utils.ClusterTableKeys.DEFAULT_DOUBLE_VALUE;

import io.harness.ccm.commons.service.intf.EntityMetadataService;
import io.harness.ccm.views.businessMapping.entities.CostTarget;
import io.harness.ccm.views.businessMapping.entities.SharedCost;
import io.harness.ccm.views.businessMapping.entities.SharedCostParameters;
import io.harness.ccm.views.dto.DataPoint;
import io.harness.ccm.views.dto.DataPoint.DataPointBuilder;
import io.harness.ccm.views.dto.Reference;
import io.harness.ccm.views.dto.TimeSeriesDataPoints;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;

import com.google.cloud.Timestamp;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerspectiveTimeSeriesResponseHelper {
  @Inject EntityMetadataService entityMetadataService;
  private static final String OTHERS = "Others";

  public Map<Timestamp, List<DataPoint>> getUpdatedDataPointsMap(Map<Timestamp, List<DataPoint>> costDataPointsMap,
      List<String> entityIds, String harnessAccountId, String fieldName) {
    Map<String, String> entityIdToName =
        entityMetadataService.getEntityIdToNameMapping(entityIds, harnessAccountId, fieldName);
    Map<Timestamp, List<DataPoint>> updatedDataPointsMap = new TreeMap<>();
    log.info("getUpdatedDataPointsMap costDataPointsMap: {}", costDataPointsMap);
    if (entityIdToName != null) {
      costDataPointsMap.keySet().forEach(timestamp
          -> updatedDataPointsMap.put(
              timestamp, getUpdatedDataPoints(costDataPointsMap.get(timestamp), entityIdToName, fieldName)));
      return updatedDataPointsMap;
    } else {
      return costDataPointsMap;
    }
  }

  private List<DataPoint> getUpdatedDataPoints(
      List<DataPoint> dataPoints, Map<String, String> entityIdToName, String fieldName) {
    List<DataPoint> updatedDataPoints = new ArrayList<>();

    dataPoints.forEach(dataPoint -> {
      String name = entityIdToName.getOrDefault(dataPoint.getKey().getName(), dataPoint.getKey().getName());
      if (AWS_ACCOUNT_FIELD.equals(fieldName)) {
        name = AwsAccountFieldHelper.mergeAwsAccountIdAndName(
            dataPoint.getKey().getName(), entityIdToName.get(dataPoint.getKey().getName()));
      }
      updatedDataPoints.add(DataPoint.builder()
                                .value(dataPoint.getValue())
                                .key(getReference(dataPoint.getKey().getId(), name, dataPoint.getKey().getType()))
                                .build());
    });
    return updatedDataPoints;
  }

  public static void addDataPointToMap(String id, String name, String type, double value,
      Map<Timestamp, List<DataPoint>> dataPointsMap, Timestamp startTimeTruncatedTimestamp) {
    if (value != DEFAULT_DOUBLE_VALUE) {
      DataPointBuilder dataPointBuilder = DataPoint.builder();
      dataPointBuilder.key(getReference(id, name, type));
      dataPointBuilder.value(getRoundedDoubleValue(value));
      List<DataPoint> dataPoints = dataPointsMap.getOrDefault(startTimeTruncatedTimestamp, new ArrayList<>());
      dataPoints.add(dataPointBuilder.build());
      dataPointsMap.put(startTimeTruncatedTimestamp, dataPoints);
    }
  }

  public static List<TimeSeriesDataPoints> convertTimeSeriesPointsMapToList(
      Map<Timestamp, List<DataPoint>> timeSeriesDataPointsMap) {
    return timeSeriesDataPointsMap.entrySet()
        .stream()
        .map(e
            -> TimeSeriesDataPoints.builder().time(e.getKey().toSqlTimestamp().getTime()).values(e.getValue()).build())
        .collect(Collectors.toList());
  }

  public void updateSharedCostMap(Map<String, Map<Timestamp, Double>> sharedCostFromGroupBy, Double sharedCostValue,
      String sharedCostName, Timestamp timeStamp) {
    if (!sharedCostFromGroupBy.containsKey(sharedCostName)) {
      sharedCostFromGroupBy.put(sharedCostName, new HashMap<>());
    }
    if (!sharedCostFromGroupBy.get(sharedCostName).containsKey(timeStamp)) {
      sharedCostFromGroupBy.get(sharedCostName).put(timeStamp, 0.0);
    }
    Double currentValue = sharedCostFromGroupBy.get(sharedCostName).get(timeStamp);
    sharedCostFromGroupBy.get(sharedCostName).put(timeStamp, currentValue + sharedCostValue);
  }

  public Map<Timestamp, List<DataPoint>> addSharedCostsFromFiltersAndRules(
      Map<Timestamp, List<DataPoint>> costDataPointsMap,
      Map<String, Map<Timestamp, Double>> sharedCostsFromRulesAndFilters) {
    Map<Timestamp, List<DataPoint>> updatedDataPointsMap = new TreeMap<>();
    costDataPointsMap.keySet().forEach(timestamp -> {
      updatedDataPointsMap.put(timestamp,
          addSharedCostsToDataPoint(costDataPointsMap.get(timestamp), sharedCostsFromRulesAndFilters, timestamp));
    });

    return updatedDataPointsMap;
  }

  public Map<Timestamp, List<DataPoint>> addSharedCosts(
      Map<Timestamp, List<DataPoint>> costDataPointsMap, SharedCostParameters sharedCostParameters) {
    Map<Timestamp, List<DataPoint>> updatedDataPointsMap = new TreeMap<>();
    costDataPointsMap.keySet().forEach(timestamp
        -> updatedDataPointsMap.put(
            timestamp, addSharedCostsToDataPoint(costDataPointsMap.get(timestamp), sharedCostParameters, timestamp)));
    return updatedDataPointsMap;
  }

  private List<DataPoint> addSharedCostsToDataPoint(List<DataPoint> dataPoints,
      Map<String, Map<Timestamp, Double>> sharedCostsFromRulesAndFilters, Timestamp timestamp) {
    List<DataPoint> updatedDataPoints = new ArrayList<>();
    Map<String, Boolean> entityDataPointAdded = new HashMap<>();

    dataPoints.forEach(dataPoint -> {
      String name = dataPoint.getKey().getName();
      entityDataPointAdded.put(name, true);
      double updatedCost = dataPoint.getValue().doubleValue();
      if (sharedCostsFromRulesAndFilters.containsKey(name)) {
        updatedCost += sharedCostsFromRulesAndFilters.get(name).getOrDefault(timestamp, 0.0);
      }
      updatedDataPoints.add(DataPoint.builder()
                                .value(getRoundedDoubleValue(updatedCost))
                                .key(getReference(dataPoint.getKey().getId(), name, dataPoint.getKey().getType()))
                                .build());
    });

    sharedCostsFromRulesAndFilters.keySet().forEach(entity -> {
      if (!entityDataPointAdded.containsKey(entity)) {
        entityDataPointAdded.put(entity, true);
        double cost = sharedCostsFromRulesAndFilters.get(entity).getOrDefault(timestamp, 0.0);
        updatedDataPoints.add(
            DataPoint.builder().value(getRoundedDoubleValue(cost)).key(getReference(entity, entity, "")).build());
      }
    });

    return updatedDataPoints;
  }

  private List<DataPoint> addSharedCostsToDataPoint(
      List<DataPoint> dataPoints, SharedCostParameters sharedCostParameters, Timestamp timestamp) {
    Set<String> entities = sharedCostParameters.getCostPerEntity().keySet();
    Map<String, Boolean> entityDataPointAdded = new HashMap<>();
    List<DataPoint> updatedDataPoints = new ArrayList<>();
    List<SharedCost> sharedCostBucketsFromGroupBy = sharedCostParameters.getBusinessMappingFromGroupBy() != null
        ? sharedCostParameters.getBusinessMappingFromGroupBy().getSharedCosts()
        : Collections.emptyList();
    List<String> costTargets = sharedCostParameters.getBusinessMappingFromGroupBy().getCostTargets() != null
        ? sharedCostParameters.getBusinessMappingFromGroupBy()
              .getCostTargets()
              .stream()
              .map(CostTarget::getName)
              .collect(Collectors.toList())
        : Collections.emptyList();

    dataPoints.forEach(dataPoint -> {
      String name = dataPoint.getKey().getName();
      entityDataPointAdded.put(name, true);
      double sharedCostForEntity = 0.0;

      if (costTargets.contains(name)) {
        sharedCostForEntity =
            calculateSharedCost(sharedCostParameters, timestamp, sharedCostParameters.getCostPerEntity().get(name),
                sharedCostBucketsFromGroupBy, sharedCostParameters.getSharedCostFromGroupBy());
      }

      updatedDataPoints.add(DataPoint.builder()
                                .value(getRoundedDoubleValue(dataPoint.getValue().doubleValue() + sharedCostForEntity))
                                .key(getReference(dataPoint.getKey().getId(), name, dataPoint.getKey().getType()))
                                .build());
    });

    entities.forEach(entity -> {
      if (!entityDataPointAdded.containsKey(entity)) {
        entityDataPointAdded.put(entity, true);
        double sharedCostForEntity = 0.0;
        if (costTargets.contains(entity)) {
          sharedCostForEntity =
              calculateSharedCost(sharedCostParameters, timestamp, sharedCostParameters.getCostPerEntity().get(entity),
                  sharedCostBucketsFromGroupBy, sharedCostParameters.getSharedCostFromGroupBy());
        }
        updatedDataPoints.add(DataPoint.builder()
                                  .value(getRoundedDoubleValue(sharedCostForEntity))
                                  .key(sharedCostParameters.getEntityReference().get(entity))
                                  .build());
      }
    });

    return updatedDataPoints;
  }

  private double calculateSharedCost(SharedCostParameters sharedCostParameters, Timestamp timestamp, Double entityCost,
      List<SharedCost> sharedCostBuckets, Map<String, Map<Timestamp, Double>> sharedCosts) {
    double sharedCost = 0.0;
    for (SharedCost sharedCostBucket : sharedCostBuckets) {
      double sharedCostForGivenTimestamp =
          sharedCosts.get(modifyStringToComplyRegex(sharedCostBucket.getName())).get(timestamp);
      switch (sharedCostBucket.getStrategy()) {
        case PROPORTIONAL:
          sharedCost += sharedCostForGivenTimestamp * (entityCost / sharedCostParameters.getTotalCost());
          break;
        case FIXED:
        default:
          sharedCost += sharedCostForGivenTimestamp * (1.0 / sharedCostParameters.getNumberOfEntities());
          break;
      }
    }
    return sharedCost;
  }

  public static String getEntityGroupByFieldName(final List<QLCEViewGroupBy> groupBy) {
    String entityGroupByFieldName = OTHERS;
    final Optional<String> groupByFieldName = groupBy.stream()
                                                  .filter(entry -> Objects.nonNull(entry.getEntityGroupBy()))
                                                  .map(entry -> entry.getEntityGroupBy().getFieldName())
                                                  .findFirst();
    if (groupByFieldName.isPresent()) {
      entityGroupByFieldName = "No " + groupByFieldName.get();
    }
    return entityGroupByFieldName;
  }

  public static Reference getReference(String id, String name, String type) {
    return Reference.builder().id(id).name(name).type(type).build();
  }

  private String modifyStringToComplyRegex(String value) {
    return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  private static double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }
}
