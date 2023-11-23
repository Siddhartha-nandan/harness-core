/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.IdentifierRef;
import io.harness.callback.DelegateCallbackToken;
import io.harness.cdng.aws.sam.AwsSamDeployStepParameters;
import io.harness.cdng.aws.sam.AwsSamStepHelper;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.infra.beans.AwsSamInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.AwsSamDirectoryManifestOutcome;
import io.harness.cdng.plugininfoproviders.PluginExecutionConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.helper.SerializedResponseDataHelper;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plugin.AbstractContainerStepV2;
import io.harness.pms.sdk.core.plugin.ContainerUnitStepUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DownloadAwsS3Step extends AbstractContainerStepV2<StepElementParameters> {
  @Inject Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Inject DownloadAwsS3StepHelper downloadAwsS3StepHelper;

  @Inject PluginExecutionConfig pluginExecutionConfig;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.DOWNLOAD_AWS_S3.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public long getTimeout(Ambiance ambiance, StepElementParameters stepElementParameters) {
    return Timeout.fromString((String) stepElementParameters.getTimeout().fetchFinalValue()).getTimeoutInMillis();
  }

  @Override
  public UnitStep getSerialisedStep(Ambiance ambiance, StepElementParameters stepElementParameters, String accountId,
      String logKey, long timeout, String parkedTaskId) {
    DownloadAwsS3StepParameters downloadAwsS3StepParameters =
        (DownloadAwsS3StepParameters) stepElementParameters.getSpec();

    Map<String, String> envVars = downloadAwsS3StepHelper.getEnvironmentVariables(
        ambiance, downloadAwsS3StepParameters, stepElementParameters.getIdentifier());
    downloadAwsS3StepHelper.removeAllEnvVarsWithSecretRef(envVars);
    downloadAwsS3StepHelper.validateEnvVariables(envVars);

    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    String completeStepIdentifier = getCompleteStepIdentifier(ambiance, stepIdentifier);

    return ContainerUnitStepUtils.serializeStepWithStepParameters(getPort(ambiance, stepIdentifier), parkedTaskId,
        logKey, completeStepIdentifier, getTimeout(ambiance, stepElementParameters), accountId,
        stepElementParameters.getName(), delegateCallbackTokenSupplier, ambiance, envVars,
        pluginExecutionConfig.getDownloadAwsS3Config().getImage(), Collections.EMPTY_LIST);
  }

  public String getCompleteStepIdentifier(Ambiance ambiance, String stepIdentifier) {
    StringBuilder identifier = new StringBuilder();
    for (Level level : ambiance.getLevelsList()) {
      if (level.getStepType().getType().equals("STEP_GROUP")) {
        identifier.append(level.getIdentifier());
        identifier.append('_');
      }
    }
    identifier.append(stepIdentifier);
    return identifier.toString();
  }

  @Override
  public StepResponse.StepOutcome getAnyOutComeForStep(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return null;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // we need to check if rbac check is req or not.
  }
}
