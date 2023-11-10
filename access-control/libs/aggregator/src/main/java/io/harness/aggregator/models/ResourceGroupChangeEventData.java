/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator.models;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceSelector;
import io.harness.aggregator.consumers.AccessControlChangeEventData;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class ResourceGroupChangeEventData implements AccessControlChangeEventData {
  Set<ResourceSelector> addedResourceSelectors;
  Set<ResourceSelector> removedResourceSelectors;
  ResourceGroup updatedResourceGroup;
}
