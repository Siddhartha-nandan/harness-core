/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.service.steps;

import static io.harness.cdng.service.steps.constants.ServiceStepConstants.OVERRIDE_IN_REVERSE_PRIORITY;
import static io.harness.cdng.service.steps.constants.ServiceStepConstants.SERVICE_CONFIGURATION_NOT_FOUND;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.environment.mappers.EnvironmentMapper.toNGEnvironmentConfig;
import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_GLOBAL_OVERRIDE;
import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_SERVICE_OVERRIDE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.common.VariablesSweepingOutput;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.steps.constants.ServiceStepV3Constants;
import io.harness.cdng.service.steps.helpers.ServiceStepsHelper;
import io.harness.cdng.service.steps.helpers.beans.ServiceStepV3Parameters;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.NGEnvironmentGlobalOverride;
import io.harness.ng.core.environment.services.impl.EnvironmentEntityYamlSchemaHelper;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.environment.yaml.NGEnvironmentInfoConfig;
import io.harness.ng.core.service.yaml.NGServiceConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideInfoConfig;
import io.harness.ng.core.serviceoverridev2.beans.NGServiceOverrideConfigV2;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.YamlPipelineUtils;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ServiceStepV3Helper {
  @Inject private ExecutionSweepingOutputService sweepingOutputService;
  @Inject private EnvironmentEntityYamlSchemaHelper environmentEntityYamlSchemaHelper;
  @Inject private CDExpressionResolver expressionResolver;
  @Inject private ServiceStepOverrideHelper serviceStepOverrideHelper;
  @Inject private ServiceStepsHelper serviceStepsHelper;

  public void processServiceAndEnvironmentVariables(Ambiance ambiance, ServicePartResponse servicePartResponse,
      NGLogCallback serviceStepLogCallback, EnvironmentOutcome environmentOutcome, boolean isOverridesV2Enabled,
      Map<ServiceOverridesType, NGServiceOverrideConfigV2> overridesV2Configs) {
    VariablesSweepingOutput variablesSweepingOutput = null;
    if (isOverridesV2Enabled) {
      if (servicePartResponse == null) {
        variablesSweepingOutput =
            getVariablesSweepingOutputFromOverridesV2(null, serviceStepLogCallback, overridesV2Configs);
      } else {
        variablesSweepingOutput = getVariablesSweepingOutputFromOverridesV2(
            servicePartResponse.getNgServiceConfig().getNgServiceV2InfoConfig(), serviceStepLogCallback,
            overridesV2Configs);
      }
    } else if (environmentOutcome != null) {
      if (servicePartResponse == null) {
        variablesSweepingOutput = getVariablesSweepingOutput(null, serviceStepLogCallback, environmentOutcome);
      } else {
        variablesSweepingOutput =
            getVariablesSweepingOutput(servicePartResponse.getNgServiceConfig().getNgServiceV2InfoConfig(),
                serviceStepLogCallback, environmentOutcome);
      }
    } else {
      if (servicePartResponse != null) {
        variablesSweepingOutput = getVariablesSweepingOutputForGitOps(
            servicePartResponse.getNgServiceConfig().getNgServiceV2InfoConfig(), serviceStepLogCallback);
      }
    }

    sweepingOutputService.consume(ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, null);

    if (servicePartResponse != null) {
      Object outputObj = variablesSweepingOutput.get("output");
      if (!(outputObj instanceof VariablesSweepingOutput)) {
        outputObj = new VariablesSweepingOutput();
      }

      sweepingOutputService.consume(ambiance, YAMLFieldNameConstants.SERVICE_VARIABLES,
          (VariablesSweepingOutput) outputObj, StepCategory.STAGE.name());

      saveExecutionLog(serviceStepLogCallback, "Processed service & environment variables");
    } else {
      saveExecutionLog(serviceStepLogCallback, "Processed environment variables");
    }
  }

  private VariablesSweepingOutput getVariablesSweepingOutputFromOverridesV2(NGServiceV2InfoConfig serviceV2InfoConfig,
      NGLogCallback logCallback, Map<ServiceOverridesType, NGServiceOverrideConfigV2> overridesV2Configs) {
    Map<String, Object> finalOverridesVariables = new HashMap<>();
    final Map<String, Object> overridesVariables = getAllOverridesVariables(overridesV2Configs, logCallback);
    if (isNotEmpty(overridesVariables)) {
      finalOverridesVariables.putAll(overridesVariables);
    }
    Map<String, Object> variables = getFinalVariablesMap(serviceV2InfoConfig, finalOverridesVariables, logCallback);
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(variables);
    return variablesOutcome;
  }

  private VariablesSweepingOutput getVariablesSweepingOutputForGitOps(
      NGServiceV2InfoConfig serviceV2InfoConfig, NGLogCallback logCallback) {
    Map<String, Object> variables = getFinalVariablesMap(serviceV2InfoConfig, Map.of(), logCallback);
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(variables);
    return variablesOutcome;
  }

  private VariablesSweepingOutput getVariablesSweepingOutput(
      NGServiceV2InfoConfig serviceV2InfoConfig, NGLogCallback logCallback, EnvironmentOutcome environmentOutcome) {
    // env v2 incorporating env variables into service variables
    final Map<String, Object> envVariables = new HashMap<>();
    if (isNotEmpty(environmentOutcome.getVariables())) {
      envVariables.putAll(environmentOutcome.getVariables());
    }
    Map<String, Object> variables = getFinalVariablesMap(serviceV2InfoConfig, envVariables, logCallback);
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(variables);
    return variablesOutcome;
  }

  public Map<String, Object> getAllOverridesVariables(
      Map<ServiceOverridesType, NGServiceOverrideConfigV2> overridesV2Configs, NGLogCallback logCallback) {
    // Priority Order : INFRA_SERVICE > INFRA_GLOBAL > ENV_SERVICE > ENV_GLOBAL
    Map<String, Object> finalOverridesVars = new HashMap<>();
    for (ServiceOverridesType overridesType : OVERRIDE_IN_REVERSE_PRIORITY) {
      if (overridesV2Configs.containsKey(overridesType)
          && isNotEmpty(overridesV2Configs.get(overridesType).getSpec().getVariables())) {
        finalOverridesVars.putAll(
            NGVariablesUtils.getMapOfVariables(overridesV2Configs.get(overridesType).getSpec().getVariables()));
        saveExecutionLog(logCallback, "Collecting variables from override of type " + overridesType.toString());
      }
    }
    return finalOverridesVars;
  }

  public Map<String, Object> getFinalVariablesMap(NGServiceV2InfoConfig serviceV2InfoConfig,
      Map<String, Object> envOrOverrideVariables, NGLogCallback logCallback) {
    List<NGVariable> variableList = new ArrayList<>();
    if (serviceV2InfoConfig != null) {
      variableList = serviceV2InfoConfig.getServiceDefinition().getServiceSpec().getVariables();
    }
    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> outputVariables = new VariablesSweepingOutput();
    if (isNotEmpty(variableList)) {
      Map<String, Object> originalVariables = NGVariablesUtils.getMapOfVariables(variableList);
      variables.putAll(originalVariables);
      outputVariables.putAll(originalVariables);
    }
    addEnvVariables(outputVariables, envOrOverrideVariables, logCallback);
    variables.put("output", outputVariables);
    return variables;
  }

  private void addEnvVariables(
      Map<String, Object> variables, Map<String, Object> envVariables, NGLogCallback logCallback) {
    if (isEmpty(envVariables)) {
      return;
    }
    saveExecutionLog(logCallback, "Applying environment variables and overrides");
    variables.putAll(envVariables);
  }

  public void resolve(Ambiance ambiance, Object... objects) {
    final List<Object> toResolve = new ArrayList<>(Arrays.asList(objects));
    expressionResolver.updateExpressions(ambiance, toResolve);
  }

  public NGEnvironmentConfig mergeEnvironmentInputs(String accountId, String identifier, String yaml,
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

  public void setYamlInEnvironment(Environment environment) {
    NGEnvironmentConfig ngEnvironmentConfig = toNGEnvironmentConfig(environment);
    environment.setYaml(io.harness.ng.core.environment.mappers.EnvironmentMapper.toYaml(ngEnvironmentConfig));
  }

  public NGEnvironmentConfig getNgEnvironmentConfig(
      Ambiance ambiance, ServiceStepV3Parameters stepParameters, String accountId, Optional<Environment> environment) {
    NGEnvironmentConfig ngEnvironmentConfig;
    final ParameterField<Map<String, Object>> envInputs = stepParameters.getEnvInputs();
    try {
      ngEnvironmentConfig =
          mergeEnvironmentInputs(accountId, environment.get().getIdentifier(), environment.get().getYaml(), envInputs);
    } catch (IOException ex) {
      throw new InvalidRequestException(
          "Unable to read yaml for environment: " + environment.get().getIdentifier(), ex);
    }
    resolve(ambiance, ngEnvironmentConfig);
    return ngEnvironmentConfig;
  }

  public NGEnvironmentConfig getNgEnvironmentConfig(String accountId, String identifier, String yaml)
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

  @NonNull
  public List<NGVariable> getSecretVariablesFromOverridesV2(
      Map<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs) {
    return mergedOverrideV2Configs.values()
        .stream()
        .map(NGServiceOverrideConfigV2::getSpec)
        .map(spec -> spec.getVariables())
        .filter(variables -> isNotEmpty(variables))
        .flatMap(Collection::stream)
        .filter(SecretNGVariable.class ::isInstance)
        .collect(Collectors.toList());
  }

  public NGServiceOverrideConfigV2 toOverrideConfigV2(NGEnvironmentConfig envConfig, String accountId) {
    NGEnvironmentInfoConfig ngEnvironmentInfoConfig = envConfig.getNgEnvironmentInfoConfig();
    NGEnvironmentGlobalOverride ngEnvironmentGlobalOverride = ngEnvironmentInfoConfig.getNgEnvironmentGlobalOverride();
    ServiceOverridesSpec.ServiceOverridesSpecBuilder specBuilder =
        ServiceOverridesSpec.builder().variables(ngEnvironmentInfoConfig.getVariables());
    if (ngEnvironmentGlobalOverride != null) {
      specBuilder.manifests(ngEnvironmentGlobalOverride.getManifests())
          .configFiles(ngEnvironmentGlobalOverride.getConfigFiles())
          .connectionStrings(ngEnvironmentGlobalOverride.getConnectionStrings())
          .applicationSettings(ngEnvironmentGlobalOverride.getApplicationSettings());
    }

    final String envRef =
        IdentifierRefHelper.getRefFromIdentifierOrRef(accountId, ngEnvironmentInfoConfig.getOrgIdentifier(),
            ngEnvironmentInfoConfig.getProjectIdentifier(), ngEnvironmentInfoConfig.getIdentifier());

    return NGServiceOverrideConfigV2.builder()
        .identifier(generateEnvGlobalOverrideV2Identifier(envRef))
        .environmentRef(envRef)
        .type(ENV_GLOBAL_OVERRIDE)
        .spec(specBuilder.build())
        .build();
  }

  private NGServiceOverrideConfigV2 toOverrideConfigV2(
      NGServiceOverrideConfig configV1, String accountId, NGEnvironmentConfig ngEnvironmentConfig) {
    NGServiceOverrideInfoConfig serviceOverrideInfoConfig = configV1.getServiceOverrideInfoConfig();
    NGEnvironmentInfoConfig ngEnvironmentInfoConfig = ngEnvironmentConfig.getNgEnvironmentInfoConfig();
    final String envRef =
        IdentifierRefHelper.getRefFromIdentifierOrRef(accountId, ngEnvironmentInfoConfig.getOrgIdentifier(),
            ngEnvironmentInfoConfig.getProjectIdentifier(), ngEnvironmentInfoConfig.getIdentifier());
    return NGServiceOverrideConfigV2.builder()
        .identifier(generateEnvServiceOverrideV2Identifier(
            serviceOverrideInfoConfig.getEnvironmentRef(), serviceOverrideInfoConfig.getServiceRef()))
        .environmentRef(envRef)
        .type(ENV_SERVICE_OVERRIDE)
        .spec(ServiceOverridesSpec.builder()
                  .variables(serviceOverrideInfoConfig.getVariables())
                  .manifests(serviceOverrideInfoConfig.getManifests())
                  .configFiles(serviceOverrideInfoConfig.getConfigFiles())
                  .connectionStrings(serviceOverrideInfoConfig.getConnectionStrings())
                  .applicationSettings(serviceOverrideInfoConfig.getApplicationSettings())
                  .build())
        .build();
  }

  public String generateEnvServiceOverrideV2Identifier(String envRef, String serviceRef) {
    return String.join("_", envRef, serviceRef).replace(".", "_");
  }

  public String generateEnvGlobalOverrideV2Identifier(String envRef) {
    return String.join("_", envRef).replace(".", "_");
  }

  public void saveExecutionLog(NGLogCallback logCallback, String line, LogLevel info, CommandExecutionStatus success) {
    if (logCallback != null) {
      logCallback.saveExecutionLog(line, info, success);
    }
  }

  public void saveExecutionLog(NGLogCallback logCallback, String line) {
    if (logCallback != null) {
      logCallback.saveExecutionLog(line);
    }
  }

  public void handleSecretVariables(NGEnvironmentConfig ngEnvironmentConfig, NGServiceOverrideConfig ngServiceOverrides,
      EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs, Ambiance ambiance,
      boolean isOverridesV2enabled) {
    List<NGVariable> secretNGVariables = new ArrayList<>();
    final String accountId = AmbianceUtils.getAccountId(ambiance);
    if (isOverridesV2enabled) {
      if (ngEnvironmentConfig != null && ngEnvironmentConfig.getNgEnvironmentInfoConfig() != null
          && !mergedOverrideV2Configs.containsKey(ENV_GLOBAL_OVERRIDE)) {
        mergedOverrideV2Configs.put(ENV_GLOBAL_OVERRIDE, toOverrideConfigV2(ngEnvironmentConfig, accountId));
      }
      if (ngServiceOverrides != null && ngServiceOverrides.getServiceOverrideInfoConfig() != null
          && !mergedOverrideV2Configs.containsKey(ENV_SERVICE_OVERRIDE)) {
        mergedOverrideV2Configs.put(
            ENV_SERVICE_OVERRIDE, toOverrideConfigV2(ngServiceOverrides, accountId, ngEnvironmentConfig));
      }
      secretNGVariables = getSecretVariablesFromOverridesV2(mergedOverrideV2Configs);

    } else {
      if (ngEnvironmentConfig != null && ngEnvironmentConfig.getNgEnvironmentInfoConfig() != null
          && ngEnvironmentConfig.getNgEnvironmentInfoConfig().getVariables() != null) {
        secretNGVariables.addAll(ngEnvironmentConfig.getNgEnvironmentInfoConfig()
                                     .getVariables()
                                     .stream()
                                     .filter(SecretNGVariable.class ::isInstance)
                                     .collect(Collectors.toList()));
      }

      if (ngServiceOverrides != null && ngServiceOverrides.getServiceOverrideInfoConfig() != null
          && ngServiceOverrides.getServiceOverrideInfoConfig().getVariables() != null) {
        secretNGVariables.addAll(ngServiceOverrides.getServiceOverrideInfoConfig()
                                     .getVariables()
                                     .stream()
                                     .filter(SecretNGVariable.class ::isInstance)
                                     .collect(Collectors.toList()));
      }
    }
    serviceStepsHelper.checkForAccessOrThrow(ambiance, secretNGVariables);
  }

  public void handleServiceChildrens(boolean isOverridesV2enabled, String accountId, Optional<Environment> environment,
      Ambiance ambiance, EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs,
      NGServiceOverrideConfig ngServiceOverrides, NGEnvironmentConfig ngEnvironmentConfig,
      ServicePartResponse servicePartResponse) {
    NGServiceV2InfoConfig ngServiceV2InfoConfig =
        NGServiceV2InfoConfig.builder().serviceDefinition(ServiceDefinition.builder().build()).build();
    NGServiceConfig ngServiceConfig = NGServiceConfig.builder().ngServiceV2InfoConfig(ngServiceV2InfoConfig).build();
    if (isOverridesV2enabled) {
      if (servicePartResponse != null) {
        ngServiceV2InfoConfig = servicePartResponse.getNgServiceConfig().getNgServiceV2InfoConfig();
        if (ngServiceV2InfoConfig == null) {
          throw new InvalidRequestException(SERVICE_CONFIGURATION_NOT_FOUND);
        }
      }
      final String scopedEnvironmentRef =
          IdentifierRefHelper.getRefFromIdentifierOrRef(accountId, environment.get().getOrgIdentifier(),
              environment.get().getProjectIdentifier(), environment.get().getIdentifier());
      serviceStepOverrideHelper.saveFinalManifestsToSweepingOutputV2(ngServiceV2InfoConfig, ambiance,
          ServiceStepV3Constants.SERVICE_MANIFESTS_SWEEPING_OUTPUT, mergedOverrideV2Configs, scopedEnvironmentRef);
      serviceStepOverrideHelper.saveFinalConfigFilesToSweepingOutputV2(ngServiceV2InfoConfig, mergedOverrideV2Configs,
          scopedEnvironmentRef, ambiance, ServiceStepV3Constants.SERVICE_CONFIG_FILES_SWEEPING_OUTPUT);
      serviceStepOverrideHelper.saveFinalAppSettingsToSweepingOutputV2(ngServiceV2InfoConfig, mergedOverrideV2Configs,
          ambiance, ServiceStepV3Constants.SERVICE_APP_SETTINGS_SWEEPING_OUTPUT);
      serviceStepOverrideHelper.saveFinalConnectionStringsToSweepingOutputV2(ngServiceV2InfoConfig,
          mergedOverrideV2Configs, ambiance, ServiceStepV3Constants.SERVICE_CONNECTION_STRINGS_SWEEPING_OUTPUT);
    } else {
      if (servicePartResponse != null) {
        ngServiceConfig = servicePartResponse.getNgServiceConfig();
      }
      serviceStepOverrideHelper.prepareAndSaveFinalManifestMetadataToSweepingOutput(ngServiceConfig, ngServiceOverrides,
          ngEnvironmentConfig, ambiance, ServiceStepV3Constants.SERVICE_MANIFESTS_SWEEPING_OUTPUT);

      serviceStepOverrideHelper.prepareAndSaveFinalConfigFilesMetadataToSweepingOutput(ngServiceConfig,
          ngServiceOverrides, ngEnvironmentConfig, ambiance,
          ServiceStepV3Constants.SERVICE_CONFIG_FILES_SWEEPING_OUTPUT);

      serviceStepOverrideHelper.prepareAndSaveFinalAppServiceMetadataToSweepingOutput(ngServiceConfig,
          ngServiceOverrides, ngEnvironmentConfig, ambiance,
          ServiceStepV3Constants.SERVICE_APP_SETTINGS_SWEEPING_OUTPUT);

      serviceStepOverrideHelper.prepareAndSaveFinalConnectionStringsMetadataToSweepingOutput(ngServiceConfig,
          ngServiceOverrides, ngEnvironmentConfig, ambiance,
          ServiceStepV3Constants.SERVICE_CONNECTION_STRINGS_SWEEPING_OUTPUT);
    }
  }

  @Data
  @Builder
  public static class ServicePartResponse {
    private NGServiceConfig ngServiceConfig;

    public NGServiceConfig getNgServiceConfig() {
      return ngServiceConfig;
    }
  }
}
