/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.events;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.PROJECT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.ProjectDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ProjectMoveEvent implements Event {
  private String accountIdentifier;
  ProjectDTO newProject;
  ProjectDTO oldProject;

  public ProjectMoveEvent(String accountIdentifier, ProjectDTO newProject, ProjectDTO oldProject) {
    this.newProject = newProject;
    this.oldProject = oldProject;
    this.accountIdentifier = accountIdentifier;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return new ProjectScope(
        accountIdentifier, newProject.getOrgIdentifier(), newProject.getIdentifier(), newProject.getParentUniqueId());
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newProject.getName());

    return Resource.builder()
        .identifier(newProject.getIdentifier())
        .uniqueId(newProject.getUniqueId())
        .type(PROJECT)
        .labels(labels)
        .build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return "ProjectMoved";
  }
}
