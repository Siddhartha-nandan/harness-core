/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless.container.steps;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.cdng.pipeline.steps.CDAbstractStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.ServerlessAwsLambdaDeployStepV2InfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.extended.ci.container.ContainerResource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = ServerlessAwsLambdaDeployStepV2InfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK_V2)
@TypeAlias("serverlessAwsLambdaDeployStepV2Info")
@RecasterAlias("io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaDeployStepV2Info")
public class ServerlessAwsLambdaDeployStepV2Info
    extends ServerlessAwsLambdaContainerBaseStepInfo implements CDAbstractStepInfo, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @JsonIgnore String downloadManifestsFqn;

  @Builder(builderMethodName = "infoBuilder")
  public ServerlessAwsLambdaDeployStepV2Info(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                             ParameterField<Map<String, JsonNode>> settings, ParameterField<String> image, ParameterField<String> connectorRef,
                                             ContainerResource resources, ParameterField<Map<String, String>> envVariables, ParameterField<Boolean> privileged,
                                             ParameterField<Integer> runAsUser, ParameterField<ImagePullPolicy> imagePullPolicy,
                                             ParameterField<String> serverlessVersion, String downloadManifestsFqn) {
    super(delegateSelectors, settings, image, connectorRef, resources, envVariables, privileged, runAsUser,
        imagePullPolicy, serverlessVersion);
    this.downloadManifestsFqn = downloadManifestsFqn;
  }
  @Override
  public StepType getStepType() {
    return ServerlessAwsLambdaDeployV2Step.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return ServerlessAwsLambdaDeployStepV2Parameters.infoBuilder()
        .image(getImage())
        .envVariables(getEnvVariables())
        .delegateSelectors(this.getDelegateSelectors())
        .build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}
