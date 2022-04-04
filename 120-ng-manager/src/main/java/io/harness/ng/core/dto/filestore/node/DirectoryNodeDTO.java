/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore.node;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.filestore.NGFileType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
@Schema(name = "DirectoryNode", description = "This contains directory details")
public class DirectoryNodeDTO extends NodeDTO {
  @NotNull String directoryIdentifier;
  @NotNull String directoryName;

  @Builder
  public DirectoryNodeDTO(String directoryIdentifier, String directoryName) {
    super(NGFileType.DIRECTORY);
    this.directoryIdentifier = directoryIdentifier;
    this.directoryName = directoryName;
  }

  public NodeDTO addChild(NodeDTO child) {
    children.add(child);
    return child;
  }
}
