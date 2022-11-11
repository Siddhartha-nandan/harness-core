package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.servicelevelobjective.entities.AbstractServiceLevelObjective;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleSLODetails {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String serviceLevelObjectiveRef;
}
