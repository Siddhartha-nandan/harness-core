package io.harness.accesscontrol.scopes;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@ApiModel(value = "ScopeResponse")
@Schema(name = "ScopeResponse")
public class ScopeResponseDTO {
  String accountIdentifier;
  String accountName;
  String orgIdentifier;
  String orgName;
  String projectIdentifier;
  String projectName;
}
