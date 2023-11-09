/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator.models;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.aggregator.consumers.AccessControlChangeEventData;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class RoleAssignmentChangeEventData implements AccessControlChangeEventData {
  RoleAssignment newRoleAssignment;
  RoleAssignment deletedRoleAssignment;
  RoleAssignment updatedRoleAssignment;
}
