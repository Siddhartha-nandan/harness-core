/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.environment.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.FREEZE_EXCEPTION;
import static io.harness.ng.core.environment.mappers.EnvironmentMapper.toNGEnvironmentConfig;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.freeze.FreezeOutcome;
import io.harness.cdng.helpers.NgExpressionHelper;
import io.harness.cdng.infra.InfraStepUtils;
import io.harness.cdng.service.steps.helpers.beans.ServiceStepV3Parameters;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.eraro.Level;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.freeze.beans.FreezeEntityType;
import io.harness.freeze.beans.response.FreezeSummaryResponseDTO;
import io.harness.freeze.helpers.FreezeRBACHelper;
import io.harness.freeze.notifications.NotificationHelper;
import io.harness.freeze.service.FreezeEvaluateService;
import io.harness.freeze.service.FrozenExecutionService;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.environment.services.impl.EnvironmentEntityYamlSchemaHelper;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverridev2.beans.NGServiceOverrideConfigV2;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.plan.ExpressionMode;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rbac.CDNGRbacUtility;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.steps.executable.SyncExecutableWithRbac;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.NGFeatureFlagHelperService;
import io.harness.utils.YamlPipelineUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class CustomStageEnvironmentStep implements SyncExecutableWithRbac<ServiceStepV3Parameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.CUSTOM_STAGE_ENVIRONMENT.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private EnvironmentService environmentService;
  @Inject private NGFeatureFlagHelperService ngFeatureFlagHelperService;
  @Inject private FreezeEvaluateService freezeEvaluateService;
  @Inject private FrozenExecutionService frozenExecutionService;
  @Inject NotificationHelper notificationHelper;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject NgExpressionHelper ngExpressionHelper;
  @Inject OutcomeService outcomeService;
  @Inject private CDExpressionResolver expressionResolver;
  @Inject private EnvironmentEntityYamlSchemaHelper environmentEntityYamlSchemaHelper;
  @Inject private ExecutionSweepingOutputService sweepingOutputService;
  public static final String FREEZE_SWEEPING_OUTPUT = "freezeSweepingOutput";
  public static final String PIPELINE_EXECUTION_EXPRESSION = "<+pipeline.execution.url>";

  @Override
  public void validateResources(Ambiance ambiance, ServiceStepV3Parameters stepParameters) {
    InfraStepUtils.validateResources(accessControlClient, ambiance, stepParameters);
  }

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, ServiceStepV3Parameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    final String accountId = AmbianceUtils.getAccountId(ambiance);
    final String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    final String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    final ParameterField<String> envRef = stepParameters.getEnvRef();
    // final ParameterField<Map<String, Object>> envInputs = stepParameters.gete;
    if (ParameterField.isNull(envRef)) {
      throw new InvalidRequestException("Environment ref not found in stage yaml");
    }

    if (envRef.isExpression()) {
      resolve(ambiance, envRef);
    }
    log.info("Starting execution for Environment Step [{}]", envRef.getValue());
    EnvironmentStepsUtils.checkForEnvAccessOrThrow(accessControlClient, ambiance, envRef);
    Map<FreezeEntityType, List<String>> entityMap = new HashMap<>();
    if (envRef.fetchFinalValue() != null) {
      Optional<Environment> environment =
          environmentService.get(accountId, orgIdentifier, projectIdentifier, envRef.getValue(), false);
      if (environment.isEmpty()) {
        throw new InvalidRequestException(String.format("Environment with ref: [%s] not found", envRef.getValue()));
      }
      NGEnvironmentConfig ngEnvironmentConfig;
      // handle old environments
      if (isEmpty(environment.get().getYaml())) {
        setYamlInEnvironment(environment.get());
      }

      try {
        ngEnvironmentConfig = mergeEnvironmentInputs(
            accountId, environment.get().getIdentifier(), environment.get().getYaml(), stepParameters.getEnvInputs());
      } catch (IOException ex) {
        throw new InvalidRequestException(
            "Unable to read yaml for environment: " + environment.get().getIdentifier(), ex);
      }

      resolve(ambiance, ngEnvironmentConfig);

      entityMap.put(FreezeEntityType.ENVIRONMENT,
          Lists.newArrayList(IdentifierRefHelper.getRefFromIdentifierOrRef(environment.get().getAccountId(),
              environment.get().getOrgIdentifier(), environment.get().getProjectIdentifier(),
              environment.get().getIdentifier())));
      entityMap.put(FreezeEntityType.ENV_TYPE, Lists.newArrayList(environment.get().getType().name()));
      entityMap.put(FreezeEntityType.ORG, Lists.newArrayList(AmbianceUtils.getOrgIdentifier(ambiance)));
      entityMap.put(FreezeEntityType.PROJECT, Lists.newArrayList(AmbianceUtils.getProjectIdentifier(ambiance)));
      entityMap.put(FreezeEntityType.PIPELINE, Lists.newArrayList(AmbianceUtils.getPipelineIdentifier(ambiance)));

      NGServiceOverrideConfig ngServiceOverrides = NGServiceOverrideConfig.builder().build();
      Map<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs = new HashMap<>();

      final EnvironmentOutcome environmentOutcome = EnvironmentMapper.toEnvironmentOutcome(
          environment.get(), ngEnvironmentConfig, ngServiceOverrides, null, mergedOverrideV2Configs, false);

      sweepingOutputService.consume(
          ambiance, OutputExpressionConstants.ENVIRONMENT, environmentOutcome, StepCategory.STAGE.name());
    }
    StepResponse stepResponse = executeFreezePart(ambiance, entityMap);
    if (stepResponse != null) {
      return stepResponse;
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @VisibleForTesting
  protected String getEnviromentRef(
      EnvironmentYaml environmentYaml, ParameterField<String> environmentRef, EnvironmentOutcome environmentOutcome) {
    if (environmentRef != null && isNotBlank(environmentRef.getValue())) {
      return environmentRef.getValue();
    } else if (environmentYaml != null) {
      return environmentYaml.getIdentifier();
    }
    return environmentOutcome.getIdentifier();
  }

  @Override
  public Class<ServiceStepV3Parameters> getStepParametersClass() {
    return ServiceStepV3Parameters.class;
  }

  protected StepResponse executeFreezePart(Ambiance ambiance, Map<FreezeEntityType, List<String>> entityMap) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    if (FreezeRBACHelper.checkIfUserHasFreezeOverrideAccess(ngFeatureFlagHelperService, accountId, orgId, projectId,
            accessControlClient, CDNGRbacUtility.constructPrincipalFromAmbiance(ambiance))) {
      return null;
    }
    List<FreezeSummaryResponseDTO> globalFreezeConfigs;
    List<FreezeSummaryResponseDTO> manualFreezeConfigs;
    globalFreezeConfigs = freezeEvaluateService.anyGlobalFreezeActive(accountId, orgId, projectId);
    manualFreezeConfigs = freezeEvaluateService.getActiveManualFreezeEntities(accountId, orgId, projectId, entityMap);
    if (globalFreezeConfigs.size() + manualFreezeConfigs.size() > 0) {
      final List<StepResponse.StepOutcome> stepOutcomes = new ArrayList<>();
      FreezeOutcome freezeOutcome = FreezeOutcome.builder()
                                        .frozen(true)
                                        .manualFreezeConfigs(manualFreezeConfigs)
                                        .globalFreezeConfigs(globalFreezeConfigs)
                                        .build();
      frozenExecutionService.createFrozenExecution(ambiance, manualFreezeConfigs, globalFreezeConfigs);

      executionSweepingOutputResolver.consume(ambiance, FREEZE_SWEEPING_OUTPUT, freezeOutcome, "");
      stepOutcomes.add(StepResponse.StepOutcome.builder()
                           .name(OutcomeExpressionConstants.FREEZE_OUTCOME)
                           .outcome(freezeOutcome)
                           .group(StepCategory.STAGE.name())
                           .build());
      String executionUrl = engineExpressionService.renderExpression(
          ambiance, PIPELINE_EXECUTION_EXPRESSION, ExpressionMode.RETURN_ORIGINAL_EXPRESSION_IF_UNRESOLVED);
      String baseUrl = ngExpressionHelper.getBaseUrl(AmbianceUtils.getAccountId(ambiance));
      notificationHelper.sendNotificationForFreezeConfigs(freezeOutcome.getManualFreezeConfigs(),
          freezeOutcome.getGlobalFreezeConfigs(), ambiance, executionUrl, baseUrl);
      return StepResponse.builder()
          .stepOutcomes(stepOutcomes)
          .failureInfo(FailureInfo.newBuilder()
                           .addFailureData(FailureData.newBuilder()
                                               .addFailureTypes(FailureType.FREEZE_ACTIVE_FAILURE)
                                               .setLevel(Level.ERROR.name())
                                               .setCode(FREEZE_EXCEPTION.name())
                                               .setMessage("Pipeline Aborted due to freeze")
                                               .build())
                           .build())
          .status(Status.FREEZE_FAILED)
          .build();
    }
    return null;
  }

  private void resolve(Ambiance ambiance, Object... objects) {
    final List<Object> toResolve = new ArrayList<>(Arrays.asList(objects));
    expressionResolver.updateExpressions(ambiance, toResolve);
  }

  private void setYamlInEnvironment(Environment environment) {
    NGEnvironmentConfig ngEnvironmentConfig = toNGEnvironmentConfig(environment);
    environment.setYaml(io.harness.ng.core.environment.mappers.EnvironmentMapper.toYaml(ngEnvironmentConfig));
  }

  private NGEnvironmentConfig mergeEnvironmentInputs(String accountId, String identifier, String yaml,
      ParameterField<Map<String, Object>> environmentInputs) throws IOException {
    if (ParameterField.isNull(environmentInputs) || isEmpty(environmentInputs.getValue())) {
      return getNgEnvironmentConfig(accountId, identifier, yaml);
    }
    Map<String, Object> environmentInputYaml = new HashMap<>();
    environmentInputYaml.put(YamlTypes.ENVIRONMENT_YAML, environmentInputs);
    String resolvedYaml = MergeHelper.mergeRuntimeInputValuesAndCheckForRuntimeInOriginalYaml(
        yaml, YamlPipelineUtils.writeYamlString(environmentInputYaml), true, true);

    return getNgEnvironmentConfig(accountId, identifier, resolvedYaml);
  }

  private NGEnvironmentConfig getNgEnvironmentConfig(String accountId, String identifier, String yaml)
      throws IOException {
    try {
      return YamlUtils.read(yaml, NGEnvironmentConfig.class);
    } catch (Exception ex) {
      environmentEntityYamlSchemaHelper.validateSchema(accountId, yaml);
      log.error(String.format(
          "Environment schema validation succeeded but failed to convert environment yaml to environment config [%s]",
          identifier));
      throw ex;
    }
  }
}
