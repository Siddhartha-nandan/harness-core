/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.steps.beans.stepnode;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.idp.steps.Constants;
import io.harness.idp.steps.beans.stepinfo.IdpCreateRepoStepInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(Constants.IDP_CREATE_REPO)
@TypeAlias(Constants.IDP_CREATE_REPO_STEP_NODE)
@OwnedBy(HarnessTeam.IDP)
@RecasterAlias("io.harness.idp.pipeline.steps.beans.stepNode.IdpCreateRepoStepNode")
public class IdpCreateRepoStepNode extends CIAbstractStepNode {
  @JsonProperty("type") private IdpCreateRepoStepNode.StepType type = StepType.IdpCreateRepo;

  @NotNull
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  private IdpCreateRepoStepInfo idpCreateRepoStepInfo;

  @Override
  public String getType() {
    return type.getName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return idpCreateRepoStepInfo;
  }

  enum StepType {
    IdpCreateRepo(CIStepInfoType.IDP_CREATE_REPO.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
