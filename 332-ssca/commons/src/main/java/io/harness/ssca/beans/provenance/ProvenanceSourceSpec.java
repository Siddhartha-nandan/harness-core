/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans.provenance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(HarnessTeam.SSCA)
@JsonSubTypes({
  @JsonSubTypes.Type(value = DockerSourceSpec.class, name = ProvenanceSourceConstants.DOCKER)
  , @JsonSubTypes.Type(value = GcrSourceSpec.class, name = ProvenanceSourceConstants.GCR)
})
public interface ProvenanceSourceSpec {
  ParameterField<String> getConnector();
}
