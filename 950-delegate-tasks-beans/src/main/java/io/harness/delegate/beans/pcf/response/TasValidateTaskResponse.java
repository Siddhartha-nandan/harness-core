package io.harness.delegate.beans.pcf.response;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TasValidateTaskResponse implements TasDelegateTaskResponse {
  private ConnectorValidationResult connectorValidationResult;
  private DelegateMetaInfo delegateMetaInfo;
}
