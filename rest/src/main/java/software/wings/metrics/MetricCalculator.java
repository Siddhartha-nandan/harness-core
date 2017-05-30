package software.wings.metrics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.math.Stats;

import software.wings.metrics.BucketData.DataSummary;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.appdynamics.AppdynamicsMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 5/23/17.
 */
public class MetricCalculator {
  /**
   * Generate the per-BT/per-metric data.
   * @param metricDefinitions A list of MetricDefinitions that are represented in the metric data.
   * @param data A Multimap of metric names to all of the data records for that name.
   * @return A Map of BT names to a map of metric names to metric data.
   */
  public static Map<String, Map<String, BucketData>> calculateMetrics(
      List<MetricDefinition> metricDefinitions, ArrayListMultimap<String, AppdynamicsMetricDataRecord> data) {
    Map<String, Map<String, BucketData>> outputMap = new HashMap<>();
    // create a map of metric ID to metric definition
    Map<String, MetricDefinition> metricDefinitionMap = new HashMap<>();
    metricDefinitions.forEach(definition -> metricDefinitionMap.put(definition.getMetricId(), definition));

    // split the data by btName
    for (String btName : data.keySet()) {
      // subsplit the per-bt data by metric
      ArrayListMultimap<MetricDefinition, AppdynamicsMetricDataRecord> metricData = ArrayListMultimap.create();
      for (AppdynamicsMetricDataRecord record : data.get(btName)) {
        // TODO: This is temporary logic until we build the interface to let people define metrics in the UI and persist
        // them If a metric doesn't have the corresponding metric definition, instead of throwing an exception, generate
        // an appropriate definition
        MetricDefinition metricDefinition;
        if (metricDefinitionMap.containsKey(String.valueOf(record.getMetricId()))) {
          metricDefinition = metricDefinitionMap.get(String.valueOf(record.getMetricId()));
        } else {
          MetricType metricType = MetricType.COUNT;
          if (record.getMetricName().endsWith("(ms)")) {
            metricType = MetricType.TIME;
          }
          metricDefinition = AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
                                 .withAccountId(record.getAccountId())
                                 .withAppdynamicsAppId(record.getAppdAppId())
                                 .withMetricId(String.valueOf(record.getMetricId()))
                                 .withMetricName(record.getMetricName())
                                 .withMetricType(metricType)
                                 .withMediumThreshold(1.0)
                                 .withHighThreshold(2.0)
                                 .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                 .build();
        }
        metricData.put(metricDefinition, record);
      }

      Map<String, BucketData> metricDataMap = new HashMap<>();
      for (MetricDefinition metricDefinition : metricData.keySet()) {
        List<AppdynamicsMetricDataRecord> singleMetricData = metricData.get(metricDefinition);
        // subsplit the per-bt/metric data by old/new
        List<List<AppdynamicsMetricDataRecord>> splitData = splitDataIntoOldAndNew(singleMetricData);
        BucketData bucketData = parse(metricDefinition, splitData);
        metricDataMap.put(metricDefinition.getMetricName(), bucketData);
      }
      outputMap.put(btName, metricDataMap);
    }
    return outputMap;
  }

  /**
   * Divides a list of data records into those generated by the old build vs those generated by the new build.
   * @param data A list of data records for a single BT/metric.
   * @return A list of two lists; the first is a list of records for the old build, the second is the list for the new.
   */
  public static List<List<AppdynamicsMetricDataRecord>> splitDataIntoOldAndNew(List<AppdynamicsMetricDataRecord> data) {
    // TODO: Rishi
    List<List<AppdynamicsMetricDataRecord>> output = new ArrayList<>();
    output.add(data.subList(0, data.size() / 2));
    output.add(data.subList(data.size() / 2, data.size()));
    return output;
  }

  /**
   * Generate stats, display value, and risk level for a set of data records.
   * The output will be unreliable for Count metrics if there was partial data;
   * for example, if we had data for nodes 1, 2, 3 in minute 1 and nodes 1, 3
   * in minute 2, nodeCount will be 3 and the count per minute will look low
   * since it's dividing two nodes' worth of count by three.
   * @param metricDefinition The MetricDefinition for the metric in these records.
   * @param records The records.
   * @return A BucketData that contains the summary for the old and new datasets.
   */
  public static BucketData parse(MetricDefinition metricDefinition, List<List<AppdynamicsMetricDataRecord>> records) {
    DataSummary oldSummary = parsePartial(metricDefinition, records.get(0));
    DataSummary newSummary = parsePartial(metricDefinition, records.get(1));
    long startTimeMillis = Math.min(records.get(0).get(0).getStartTime(), records.get(1).get(0).getStartTime());
    long endTimeMillis = Math.max(records.get(0).get(records.get(0).size() - 1).getStartTime(),
                             records.get(1).get(records.get(1).size() - 1).getStartTime())
        + TimeUnit.MINUTES.toMillis(1);
    RiskLevel risk = RiskLevel.LOW;
    // default thresholds: 1-2x = medium, >2x high
    double ratio = 0.0;
    if (metricDefinition.getMetricType() == MetricType.COUNT) {
      ratio = (newSummary.getStats().sum() / newSummary.getNodeCount())
          / (oldSummary.getStats().sum() / oldSummary.getNodeCount());
    } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE
        || metricDefinition.getMetricType() == MetricType.TIME) {
      ratio = newSummary.getStats().mean() / oldSummary.getStats().mean();
    }
    if (metricDefinition.getThresholdType() == ThresholdType.ALERT_WHEN_HIGHER) {
      if (ratio >= metricDefinition.getHighThreshold()) {
        risk = RiskLevel.HIGH;
      } else if (ratio >= metricDefinition.getMediumThreshold() && ratio < metricDefinition.getHighThreshold()) {
        risk = RiskLevel.MEDIUM;
      }
    } else if (metricDefinition.getThresholdType() == ThresholdType.ALERT_WHEN_LOWER) {
      if (ratio <= metricDefinition.getHighThreshold()) {
        risk = RiskLevel.HIGH;
      } else if (ratio <= metricDefinition.getMediumThreshold() && ratio > metricDefinition.getHighThreshold()) {
        risk = RiskLevel.MEDIUM;
      }
    }
    BucketData bucketData = BucketData.Builder.aBucketData()
                                .withStartTimeMillis(startTimeMillis)
                                .withEndTimeMillis(endTimeMillis)
                                .withRisk(risk)
                                .withOldData(oldSummary)
                                .withNewData(newSummary)
                                .build();
    return bucketData;
  }

  /**
   * Parses a single metric's records into the stats for that set of values.
   * @param metricDefinition The MetricDefinition for the metric in these records.
   * @param records The records.
   * @return A DataSummary containing the stats, the display value, and whether any data was missing.
   */
  public static DataSummary parsePartial(MetricDefinition metricDefinition, List<AppdynamicsMetricDataRecord> records) {
    TreeMap<Long, Double> valueMap = new TreeMap<>();
    TreeMap<Long, List<Double>> tempValueMap = new TreeMap<>();
    HashSet<String> nodeSet = new HashSet<>();
    for (AppdynamicsMetricDataRecord record : records) {
      Long startTime = record.getStartTime();
      nodeSet.add(record.getNodeName());
      // this combines the values across all nodes for each time period
      if (tempValueMap.containsKey(startTime)) {
        tempValueMap.get(startTime).add(record.getValue());
      } else {
        tempValueMap.put(startTime, new ArrayList<>(Arrays.asList(record.getValue())));
      }
    }
    int nodeCount = nodeSet.size();
    for (Long key : tempValueMap.keySet()) {
      Stats tempStats = Stats.of(tempValueMap.get(key));
      if (metricDefinition.getMetricType() == MetricType.COUNT) {
        valueMap.put(key, tempStats.sum());
      } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
        valueMap.put(key, tempStats.mean());
      } else if (metricDefinition.getMetricType() == MetricType.TIME) {
        valueMap.put(key, tempStats.mean());
      }
    }
    Stats stats = Stats.of(valueMap.values());
    long startTime = valueMap.firstKey();
    long endTime = valueMap.lastKey() + TimeUnit.MINUTES.toMillis(1);

    /*
     * If the number of time values (the number of keys in valueMap) isn't equal to the number of minutes between
     * the start and end times, or if the number of time values multiplied by the number of nodes in the data isn't
     * equal to the number of records in the input list, there's at least one metric value missing.
     */
    boolean missingData = false;
    long expectedValueCount = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    if ((valueMap.size() != expectedValueCount) || (valueMap.size() * nodeCount != records.size())) {
      missingData = true;
    }

    String displayValue = "";
    if (metricDefinition.getMetricType() == MetricType.COUNT) {
      displayValue = String.valueOf(stats.sum());
    } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
      displayValue = String.valueOf(stats.mean());
    } else if (metricDefinition.getMetricType() == MetricType.TIME) {
      displayValue = String.valueOf(stats.mean());
    }
    return new BucketData().new DataSummary(nodeCount, new ArrayList<>(nodeSet), stats, displayValue, missingData);
  }
}
