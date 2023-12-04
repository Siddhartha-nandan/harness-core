package io.harness.batch.processing.svcmetrics;

import io.harness.metrics.AutoMetricContext;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConnectorHealthContext extends AutoMetricContext {
  public ConnectorHealthContext(String accountId, String connectorId, String healthStatus) {
    put("accountId", accountId);
    put("connectorId", connectorId);
    put("healthStatus", healthStatus);
  }
}
