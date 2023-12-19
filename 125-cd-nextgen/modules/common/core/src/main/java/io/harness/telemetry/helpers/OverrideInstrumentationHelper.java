/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_SERVICE_OVERRIDE;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.azure.config.yaml.ApplicationSettingsConfiguration;
import io.harness.cdng.azure.config.yaml.ConnectionStringsConfiguration;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideInfoConfig;
import io.harness.ng.core.serviceoverridev2.beans.NGServiceOverrideConfigV2;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.yaml.core.variables.NGVariable;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class OverrideInstrumentationHelper extends InstrumentationHelper {
  public static final String ORG_ID = "org_id";
  public static final String PROJECT_ID = "project_id";
  public static final String OVERRIDE_EVENT = "override";
  public static final String OVERRIDE_TYPE = "override_type";
  public static final String SERVICE_REF = "service_ref";
  public static final String ENVIRONMENT_REF = "environment_ref";
  public static final String INFRA_IDENTIFIER = "infra_id";
  public static final String OVERRIDE_V2 = "override_v2";
  public static final String MANIFEST_OVERRIDE = "manifest_override";
  public static final String VARIABLE_OVERRIDE = "variable_override";
  public static final String APPLICATION_SETTINGS = "application_settings";
  public static final String CONNECTION_STRINGS = "connection_strings";
  public static final String CONFIG_FILES = "config_files";

  public void addTelemetryEventsForOverrideV2(String accountId, String orgIdentifier, String projectIdentifier,
      EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs) {
    try {
      List<NGServiceOverrideConfigV2> ngServiceOverrideConfigV2List = new ArrayList<>(mergedOverrideV2Configs.values());
      ngServiceOverrideConfigV2List.forEach(ngServiceOverrideConfigV2
          -> publishOverrideV2Event(accountId, orgIdentifier, projectIdentifier, ngServiceOverrideConfigV2));
    } catch (Exception e) {
      log.error("Override Telemetry event failed for accountID = " + accountId, e);
    }
  }

  public void addTelemetryEventsForOverrideV1(
      String accountId, String orgIdentifier, String projectIdentifier, NGServiceOverrideConfig ngServiceOverrides) {
    try {
      if (ngServiceOverrides != null && ngServiceOverrides.getServiceOverrideInfoConfig() != null) {
        publishOverrideV1Event(
            accountId, orgIdentifier, projectIdentifier, ngServiceOverrides.getServiceOverrideInfoConfig());
      }
    } catch (Exception e) {
      log.error("Override Telemetry event failed for accountID = " + accountId, e);
    }
  }

  private void publishOverrideV1Event(String accountId, String orgIdentifier, String projectIdentifier,
      NGServiceOverrideInfoConfig ngServiceOverrideInfoConfig) {
    HashMap<String, Object> eventPropertiesMap = new HashMap<>();
    eventPropertiesMap.put(ORG_ID, orgIdentifier);
    eventPropertiesMap.put(OVERRIDE_V2, false);
    eventPropertiesMap.put(PROJECT_ID, projectIdentifier);
    eventPropertiesMap.put(OVERRIDE_TYPE, ENV_SERVICE_OVERRIDE);
    eventPropertiesMap.put(SERVICE_REF, ngServiceOverrideInfoConfig.getServiceRef());
    eventPropertiesMap.put(ENVIRONMENT_REF, ngServiceOverrideInfoConfig.getEnvironmentRef());

    addManifestCountToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getManifests());
    addVariableCountToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getVariables());
    addApplicationSettingToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getApplicationSettings());
    addConfigFilesToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getConfigFiles());
    addConnectionStringsConfigToMap(eventPropertiesMap, ngServiceOverrideInfoConfig.getConnectionStrings());

    sendEvent(OVERRIDE_EVENT, accountId, eventPropertiesMap);
  }

  private void publishOverrideV2Event(String accountId, String orgIdentifier, String projectIdentifier,
      NGServiceOverrideConfigV2 ngServiceOverrideConfigV2) {
    if (ngServiceOverrideConfigV2 != null) {
      ServiceOverridesSpec spec = ngServiceOverrideConfigV2.getSpec();

      if (isNull(spec.getVariables()) && isNull(spec.getApplicationSettings()) && isNull(spec.getManifests())
          && isNull(spec.getConnectionStrings()) && isNull(spec.getConfigFiles())) {
        return;
      }

      HashMap<String, Object> eventPropertiesMap = new HashMap<>();
      eventPropertiesMap.put(ORG_ID, orgIdentifier);
      eventPropertiesMap.put(OVERRIDE_V2, true);
      eventPropertiesMap.put(PROJECT_ID, projectIdentifier);
      eventPropertiesMap.put(OVERRIDE_TYPE, ngServiceOverrideConfigV2.getType());
      eventPropertiesMap.put(SERVICE_REF, ngServiceOverrideConfigV2.getServiceRef());
      eventPropertiesMap.put(ENVIRONMENT_REF, ngServiceOverrideConfigV2.getEnvironmentRef());
      eventPropertiesMap.put(INFRA_IDENTIFIER, ngServiceOverrideConfigV2.getInfraId());

      addManifestCountToMap(eventPropertiesMap, spec.getManifests());
      addVariableCountToMap(eventPropertiesMap, spec.getVariables());
      addApplicationSettingToMap(eventPropertiesMap, spec.getApplicationSettings());
      addConfigFilesToMap(eventPropertiesMap, spec.getConfigFiles());
      addConnectionStringsConfigToMap(eventPropertiesMap, spec.getConnectionStrings());

      sendEvent(OVERRIDE_EVENT, accountId, eventPropertiesMap);
    }
  }

  private void addConfigFilesToMap(HashMap<String, Object> eventPropertiesMap, List<ConfigFileWrapper> configFiles) {
    if (configFiles != null) {
      eventPropertiesMap.put(CONFIG_FILES, configFiles.size());
    }
  }

  private void addConnectionStringsConfigToMap(
      HashMap<String, Object> eventPropertiesMap, ConnectionStringsConfiguration connectionStrings) {
    if (connectionStrings != null) {
      eventPropertiesMap.put(CONNECTION_STRINGS, true);
    }
  }

  private void addApplicationSettingToMap(
      HashMap<String, Object> eventPropertiesMap, ApplicationSettingsConfiguration applicationSettings) {
    if (applicationSettings != null) {
      eventPropertiesMap.put(APPLICATION_SETTINGS, true);
    }
  }

  private void addVariableCountToMap(HashMap<String, Object> eventPropertiesMap, List<NGVariable> variables) {
    Map<String, Integer> variableCount = new HashMap<>();
    if (variables != null) {
      variables.forEach(variable -> {
        String variableKey = variable.getType().toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE;
        variableCount.put(variableKey, variableCount.getOrDefault(variableKey, 0) + 1);
      });
    }
    eventPropertiesMap.putAll(variableCount);
  }

  private void addManifestCountToMap(
      HashMap<String, Object> eventPropertiesMap, List<ManifestConfigWrapper> manifestConfigWrappers) {
    Map<String, Integer> manifestCount = new HashMap<>();
    if (manifestConfigWrappers != null) {
      manifestConfigWrappers.forEach(manifestConfigWrapper -> {
        if (manifestConfigWrapper.getManifest() != null) {
          String manifestKey = manifestConfigWrapper.getManifest().getType().toString().toLowerCase(Locale.ROOT) + "_"
              + MANIFEST_OVERRIDE;
          manifestCount.put(manifestKey, manifestCount.getOrDefault(manifestKey, 0) + 1);
        }
      });
    }
    eventPropertiesMap.putAll(manifestCount);
  }
}
