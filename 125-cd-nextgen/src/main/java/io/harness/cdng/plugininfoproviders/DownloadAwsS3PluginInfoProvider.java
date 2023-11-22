/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.plugininfoproviders;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.aws.sam.AwsSamDeployStep;
import io.harness.cdng.aws.sam.AwsSamDeployStepInfo;
import io.harness.cdng.aws.sam.AwsSamDeployStepParameters;
import io.harness.cdng.aws.sam.AwsSamStepHelper;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.pipeline.executions.CDPluginInfoProvider;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.connector.services.ConnectorService;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ImageDetails;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponse;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.plan.PluginDetails;
import io.harness.pms.contracts.plan.StepInfoProto;
import io.harness.pms.sdk.core.plugin.ContainerPluginParseException;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.container.execution.plugin.StepImageConfig;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class DownloadAwsS3PluginInfoProvider implements CDPluginInfoProvider {
  @Inject private CDExpressionResolver cdExpressionResolver;
  @Inject private OutcomeService outcomeService;

  @Inject PluginExecutionConfig pluginExecutionConfig;

  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;

  @Inject private AwsSamStepHelper awsSamStepHelper;

  @Inject private AwsSamDeployStep awsSamDeployStep;

  @Override
  public PluginCreationResponseWrapper getPluginInfo(
      PluginCreationRequest request, Set<Integer> usedPorts, Ambiance ambiance) {
    String stepJsonNode = request.getStepJsonNode();
    CdAbstractStepNode cdAbstractStepNode;

    try {
      cdAbstractStepNode = YamlUtils.read(stepJsonNode, CdAbstractStepNode.class);
    } catch (IOException e) {
      throw new ContainerPluginParseException(
          format("Error in parsing CD step for step type [%s]", request.getType()), e);
    }

    DownloadAwsS3StepInfo downloadAwsS3StepInfo = (DownloadAwsS3StepInfo) cdAbstractStepNode.getStepSpecType();

    PluginDetails.Builder pluginDetailsBuilder = PluginInfoProviderHelper.buildPluginDetails(
        downloadAwsS3StepInfo.getResources(), downloadAwsS3StepInfo.getRunAsUser(), usedPorts, true);

    ImageDetails imageDetails = null;

    imageDetails = PluginInfoProviderHelper.getImageDetails(ParameterField.ofNull(),
        ParameterField.createValueField(pluginExecutionConfig.getDownloadS3Config().getImage()),
        ParameterField.ofNull());

    pluginDetailsBuilder.setImageDetails(imageDetails);

    Map<String, String> envVars;
    // We cannot provide secret environment variables to the container at runtime. So during  initialize phase, we pass
    // these secrets variables
    //    envVars = awsSamStepHelper.getEnvVarsWithSecretRef(awsSamDeployStep.getEnvironmentVariables(
    //        ambiance, (AwsSamDeployStepParameters) awsSamDeployStepInfo.getSpecParameters()));

    envVars = new HashMap<>();
    pluginDetailsBuilder.putAllEnvVariables(awsSamStepHelper.validateEnvVariables(envVars));

    PluginCreationResponse response =
        PluginCreationResponse.newBuilder().setPluginDetails(pluginDetailsBuilder.build()).build();
    StepInfoProto stepInfoProto = StepInfoProto.newBuilder()
                                      .setIdentifier(cdAbstractStepNode.getIdentifier())
                                      .setName(cdAbstractStepNode.getName())
                                      .setUuid(cdAbstractStepNode.getUuid())
                                      .build();
    return PluginCreationResponseWrapper.newBuilder().setResponse(response).setStepInfo(stepInfoProto).build();
  }

  @Override
  public boolean isSupported(String stepType) {
    if (stepType.equals(StepSpecTypeConstants.DOWNLOAD_AWS_S3)) {
      return true;
    }
    return false;
  }
}
