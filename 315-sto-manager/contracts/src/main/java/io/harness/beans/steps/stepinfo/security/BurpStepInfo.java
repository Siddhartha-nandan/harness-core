/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps.stepinfo.security;

import static io.harness.annotations.dev.HarnessTeam.STO;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.security.shared.STOGenericStepInfo;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlAuth;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlBurpToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlInstance;
import io.harness.yaml.sto.variables.STOYamlBurpConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("Burp")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("burpStepInfo")
@OwnedBy(STO)
@RecasterAlias("io.harness.beans.steps.stepinfo.security.BurpStepInfo")
public class BurpStepInfo extends STOGenericStepInfo {
  @JsonProperty protected STOYamlInstance instance;

  @JsonProperty("tool") protected STOYamlBurpToolData tool;

  @NotNull
  @ApiModelProperty(dataType = "io.harness.yaml.sto.variables.STOYamlBurpConfig")
  @Field(name = "burpConfig")
  protected STOYamlBurpConfig config;

  @JsonProperty protected STOYamlAuth auth;
}
