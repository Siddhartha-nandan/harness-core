package io.harness.connector.stats;

import io.harness.ConnectorConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ConnectorStatisticsKeys")
@ApiModel("ConnectorStatistics")
@Value
@Builder
@AllArgsConstructor
@Schema(description = "This has the count for all Connector Types and Status defined in Harness")
public class ConnectorStatistics {
  @Schema(description = ConnectorConstants.CONNECTOR_TYPE_STATS) List<ConnectorTypeStats> typeStats;
  @Schema(description = ConnectorConstants.CONNECTOR_STATUS_STATS) List<ConnectorStatusStats> statusStats;
}
