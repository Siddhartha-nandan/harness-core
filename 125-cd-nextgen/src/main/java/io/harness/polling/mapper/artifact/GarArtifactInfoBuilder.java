package io.harness.polling.mapper.artifact;

import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.artifact.GARArtifactInfo;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.mapper.PollingInfoBuilder;

public class GarArtifactInfoBuilder implements PollingInfoBuilder {
  @Override
  public PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData) {
    return GARArtifactInfo.builder()
        .connectorRef(pollingPayloadData.getConnectorRef())
        .pkg(pollingPayloadData.getGarPayload().getPkg())
        .region(pollingPayloadData.getGarPayload().getRegion())
        .repositoryName(pollingPayloadData.getGarPayload().getRepositoryName())
        .project(pollingPayloadData.getGarPayload().getProject())
        .build();
  }
}
