/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_GLOBAL_OVERRIDE;
import static io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType.ENV_SERVICE_OVERRIDE;
import static io.harness.rule.OwnerRule.SOURABH;
import static io.harness.telemetry.helpers.OverrideInstrumentationHelper.MANIFEST_OVERRIDE;
import static io.harness.telemetry.helpers.OverrideInstrumentationHelper.OVERRIDE_EVENT;
import static io.harness.telemetry.helpers.OverrideInstrumentationHelper.VARIABLE_OVERRIDE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideInfoConfig;
import io.harness.ng.core.serviceoverridev2.beans.NGServiceOverrideConfigV2;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesSpec;
import io.harness.ng.core.serviceoverridev2.beans.ServiceOverridesType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OverrideInstrumentationHelperTest extends CategoryTest {
  @InjectMocks OverrideInstrumentationHelper overrideInstrumentationHelper;
  @Mock TelemetryReporter telemetryReporter;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_ID = "org_id";
  private final String PROJECT_ID = "project_id";
  private final String SVC_REF = "svc_ref";
  private final String ENV_REF = "env_ref";
  private final String INFRA_REF = "infra_ref";

  private static final ManifestConfigWrapper k8sManifest =
      ManifestConfigWrapper.builder()
          .manifest(ManifestConfig.builder().identifier("k8s_test1").type(ManifestConfigType.K8_MANIFEST).build())
          .build();
  private static final ManifestConfigWrapper valuesManifest =
      ManifestConfigWrapper.builder()
          .manifest(ManifestConfig.builder().identifier("values_test1").type(ManifestConfigType.VALUES).build())
          .build();

  private static final NGVariable stringVariable =
      StringNGVariable.builder().defaultValue("ss").type(NGVariableType.STRING).build();

  private static final NGVariable numberVariable =
      StringNGVariable.builder().defaultValue("0").type(NGVariableType.NUMBER).build();
  private static final NGVariable numberVariable1 =
      StringNGVariable.builder().defaultValue("1").type(NGVariableType.NUMBER).build();

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testAddTelemetryEventForOverrideV1() {
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    NGServiceOverrideConfig serviceOverrideConfig =
        NGServiceOverrideConfig.builder()
            .serviceOverrideInfoConfig(NGServiceOverrideInfoConfig.builder()
                                           .serviceRef(SVC_REF)
                                           .environmentRef(ENV_REF)
                                           .manifests(Arrays.asList(k8sManifest, valuesManifest))
                                           .variables(Arrays.asList(stringVariable, numberVariable, numberVariable1))
                                           .build())
            .build();

    overrideInstrumentationHelper.addTelemetryEventsForOverrideV1(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, serviceOverrideConfig);
    verify(telemetryReporter)
        .sendTrackEvent(eq(OVERRIDE_EVENT), anyString(), any(), captor.capture(), any(), any(), any());

    HashMap<String, Object> propertiesMap = captor.getValue();
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ORG_ID)).isEqualTo(ORG_ID);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.PROJECT_ID)).isEqualTo(PROJECT_ID);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ENVIRONMENT_REF)).isEqualTo(ENV_REF);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.SERVICE_REF)).isEqualTo(SVC_REF);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.OVERRIDE_V2)).isEqualTo(false);
    assertThat(
        propertiesMap.get(ManifestConfigType.K8_MANIFEST.toString().toLowerCase(Locale.ROOT) + "_" + MANIFEST_OVERRIDE))
        .isEqualTo(1);
    assertThat(propertiesMap.get(NGVariableType.STRING.toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE))
        .isEqualTo(1);
    assertThat(propertiesMap.get(NGVariableType.NUMBER.toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE))
        .isEqualTo(2);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testAddTelemetryEventForOverrideV1Empty() {
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    NGServiceOverrideConfig serviceOverrideConfig =
        NGServiceOverrideConfig.builder()
            .serviceOverrideInfoConfig(NGServiceOverrideInfoConfig.builder()
                                           .serviceRef(SVC_REF)
                                           .environmentRef(ENV_REF)
                                           .manifests(Arrays.asList(k8sManifest, valuesManifest))
                                           .variables(Arrays.asList(stringVariable, numberVariable, numberVariable1))
                                           .build())
            .build();

    overrideInstrumentationHelper.addTelemetryEventsForOverrideV1(ACCOUNT_ID, ORG_ID, PROJECT_ID, null);
    verify(telemetryReporter, times(0))
        .sendTrackEvent(eq(OVERRIDE_EVENT), anyString(), any(), captor.capture(), any(), any(), any());
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testAddTelemetryEventForEnvSvcOverrideV2() {
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    overrideInstrumentationHelper.addTelemetryEventsForOverrideV2(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, getOverridesConfigsV2MapForEnvSvcTestData());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(OVERRIDE_EVENT), anyString(), any(), captor.capture(), any(), any(), any());

    HashMap<String, Object> propertiesMap = captor.getValue();
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ORG_ID)).isEqualTo(ORG_ID);
    assertForPropertyMap(propertiesMap, ENV_REF, SVC_REF, null, ENV_SERVICE_OVERRIDE, null, 1, 1);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testAddTelemetryEventForGlobalEnvOverrideV2() {
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    overrideInstrumentationHelper.addTelemetryEventsForOverrideV2(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, getOverridesConfigsV2MapForEnvTestData());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(OVERRIDE_EVENT), anyString(), any(), captor.capture(), any(), any(), any());

    HashMap<String, Object> propertiesMap = captor.getValue();
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ORG_ID)).isEqualTo(ORG_ID);
    assertForPropertyMap(propertiesMap, ENV_REF, null, null, ENV_GLOBAL_OVERRIDE, 3, 3, null);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testAddTelemetryEventForGlobalEnvOverrideV2Empty() {
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    overrideInstrumentationHelper.addTelemetryEventsForOverrideV2(ACCOUNT_ID, ORG_ID, PROJECT_ID, null);
    verify(telemetryReporter, times(0))
        .sendTrackEvent(eq(OVERRIDE_EVENT), anyString(), any(), captor.capture(), any(), any(), any());
  }

  private void assertForPropertyMap(HashMap<String, Object> propertiesMap, String envRef, String svcRef, String infraId,
      ServiceOverridesType type, Integer manifestCount, Integer stringCount, Integer numberCount) {
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ORG_ID)).isEqualTo(ORG_ID);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.PROJECT_ID)).isEqualTo(PROJECT_ID);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.OVERRIDE_TYPE)).isEqualTo(type);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.ENVIRONMENT_REF)).isEqualTo(envRef);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.SERVICE_REF)).isEqualTo(svcRef);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.INFRA_IDENTIFIER)).isEqualTo(infraId);
    assertThat(propertiesMap.get(OverrideInstrumentationHelper.OVERRIDE_V2)).isEqualTo(true);
    assertThat(
        propertiesMap.get(ManifestConfigType.K8_MANIFEST.toString().toLowerCase(Locale.ROOT) + "_" + MANIFEST_OVERRIDE))
        .isEqualTo(manifestCount);
    assertThat(propertiesMap.get(NGVariableType.STRING.toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE))
        .isEqualTo(stringCount);
    assertThat(propertiesMap.get(NGVariableType.NUMBER.toString().toLowerCase(Locale.ROOT) + "_" + VARIABLE_OVERRIDE))
        .isEqualTo(numberCount);
  }

  private EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> getOverridesConfigsV2MapForEnvSvcTestData() {
    EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs =
        new EnumMap<>(ServiceOverridesType.class);

    mergedOverrideV2Configs.put(ENV_SERVICE_OVERRIDE,
        NGServiceOverrideConfigV2.builder()
            .identifier("id1")
            .environmentRef(ENV_REF)
            .serviceRef(SVC_REF)
            .type(ENV_SERVICE_OVERRIDE)
            .spec(ServiceOverridesSpec.builder()
                      .variables(List.of(NumberNGVariable.builder()
                                             .name("numbervar1")
                                             .value(ParameterField.createValueField(123D))
                                             .type(NGVariableType.NUMBER)
                                             .build(),
                          StringNGVariable.builder()
                              .name("envServiceStringVar")
                              .value(ParameterField.createValueField("envServiceStringVal"))
                              .type(NGVariableType.STRING)
                              .build()))
                      .build())
            .build());

    return mergedOverrideV2Configs;
  }

  private EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> getOverridesConfigsV2MapForEnvTestData() {
    EnumMap<ServiceOverridesType, NGServiceOverrideConfigV2> mergedOverrideV2Configs =
        new EnumMap<>(ServiceOverridesType.class);
    mergedOverrideV2Configs.put(ENV_GLOBAL_OVERRIDE,
        NGServiceOverrideConfigV2.builder()
            .identifier("id0")
            .environmentRef(ENV_REF)
            .type(ENV_GLOBAL_OVERRIDE)
            .spec(ServiceOverridesSpec.builder()
                      .variables(List.of(StringNGVariable.builder()
                                             .name("stringvar")
                                             .value(ParameterField.createValueField("stringVarFromEnv"))
                                             .type(NGVariableType.STRING)
                                             .build(),
                          SecretNGVariable.builder()
                              .name("envSecretVar")
                              .value(ParameterField.createValueField(
                                  SecretRefData.builder()
                                      .scope(Scope.PROJECT)
                                      .identifier("secretFromEnv")
                                      .decryptedValue("secretValueFromEnv".toCharArray())
                                      .build()))
                              .type(NGVariableType.STRING)
                              .build(),
                          StringNGVariable.builder()
                              .name("envStringVar")
                              .value(ParameterField.createValueField("envStringVal"))
                              .type(NGVariableType.STRING)
                              .build()))
                      .manifests(Arrays.asList(k8sManifest, k8sManifest, k8sManifest))
                      .build())
            .build());
    return mergedOverrideV2Configs;
  }
}
