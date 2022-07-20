package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.scopes.ScopeSelector;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "UserRoleAssignmentFilter")
@Schema(name = "UserRoleAssignmentFilter")
@OwnedBy(PL)
public class UserRoleAssignmentFilter {
  @Schema(description = "Filter role assignments based on resource group identifiers") Set<String> resourceGroupFilter;
  @Schema(description = "Filter role assignments based on role identifiers") Set<String> roleFilter;
  @Schema(description = "Filter role assignments based on scope filters")
  Set<ScopeSelector> scopeFilters = new HashSet<>();
  @Schema(description = "Filter role assignments based on principals") Set<PrincipalDTO> principalFilter;
}
