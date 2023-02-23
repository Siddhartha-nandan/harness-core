/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.audit.ResourceTypeConstants.SERVICE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.utils.ResourceScopeUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PIPELINE)
@Getter
@Builder
@AllArgsConstructor
public class ServiceUpdateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private ServiceEntity newService;
  private ServiceEntity oldService;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return ResourceScopeUtils.getEntityScope(newService);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newService.getName());
    return Resource.builder().identifier(newService.getIdentifier()).type(SERVICE).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.SERVICE_UPDATED;
  }
}
