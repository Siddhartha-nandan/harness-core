/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.beans.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.expression;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.SwaggerConstants;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.cdng.aws.sam.AwsSamBaseStepInfo;
import io.harness.cdng.aws.sam.AwsSamSpecParameters;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.extended.ci.container.ContainerResource;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("downloadAwsS3StepParameters")
@RecasterAlias("io.harness.cdng.containerStepGroup.DownloadAwsS3StepParameters")
public class DownloadAwsS3StepParameters
    extends ContainerStepGroupBaseStepInfo implements SpecParameters, StepParameters {
  ParameterField<String> connectorRef;

  ParameterField<String> downloadPath;

  ParameterField<String> bucketName;

  ParameterField<String> region;

  ParameterField<List<String>> outputFilePathsContent;

  ParameterField<List<String>> paths;

  @Builder(builderMethodName = "infoBuilder")
  public DownloadAwsS3StepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      ParameterField<String> connectorRef, ContainerResource resources, ParameterField<List<String>> paths,
      ParameterField<Integer> runAsUser, ParameterField<String> downloadPath, ParameterField<String> bucketName,
      ParameterField<String> region, ParameterField<List<String>> outputFilePathsContent) {
    super(delegateSelectors, runAsUser, resources);
    this.connectorRef = connectorRef;
    this.downloadPath = downloadPath;
    this.bucketName = bucketName;
    this.paths = paths;
    this.region = region;
    this.outputFilePathsContent = outputFilePathsContent;
  }
}
