package io.harness.batch.processing.svcmetrics;

import io.harness.metrics.AutoMetricContext;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClusterHealthContext extends AutoMetricContext {
  public ClusterHealthContext(String accountId, String clusterId, String healthStatus) {
    put("accountId", accountId);
    put("clusterId", clusterId);
    put("healthStatus", healthStatus);
  }
}
