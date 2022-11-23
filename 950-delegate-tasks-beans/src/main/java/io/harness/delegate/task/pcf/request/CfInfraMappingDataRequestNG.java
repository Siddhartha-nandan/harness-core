package io.harness.delegate.task.pcf.request;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.task.pcf.CfCommandTypeNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class CfInfraMappingDataRequestNG implements CfCommandRequestNG {
  String accountId;
  CfCommandTypeNG cfCommandTypeNG;
  String commandName;
  CommandUnitsProgress commandUnitsProgress;
  TasInfraConfig tasInfraConfig;
  Integer timeoutIntervalInMin;
  String host;
  String domain;
  String path;
  Integer port;
  boolean useRandomPort;
  boolean tcpRoute;
  String applicationNamePrefix;
  CfDataFetchActionType actionType;
  @Override
  public TasInfraConfig getTasInfraConfig() {
    return tasInfraConfig;
  }
}
