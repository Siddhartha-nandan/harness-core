package io.harness.cdng.service.steps;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestConstants;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ServiceStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks ServiceStep serviceStep;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateServiceOutcome() {
    K8sManifest k8Manifest =
        K8sManifest.builder()
            .identifier("m1")
            .storeConfigWrapper(
                StoreConfigWrapper.builder()
                    .storeConfig(GitStore.builder()
                                     .paths(ParameterField.createValueField(Collections.singletonList("path")))
                                     .connectorRef(ParameterField.createValueField("g1"))
                                     .gitFetchType(FetchType.BRANCH)
                                     .branch(ParameterField.createValueField("master"))
                                     .build())
                    .build())
            .build();

    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder()
                                                .identifier(k8Manifest.getIdentifier())
                                                .store(k8Manifest.getStoreConfigWrapper())
                                                .build();

    K8sManifest k8Manifest1 =
        K8sManifest.builder()
            .identifier("m2")
            .storeConfigWrapper(
                StoreConfigWrapper.builder()
                    .storeConfig(GitStore.builder()
                                     .paths(ParameterField.createValueField(Collections.singletonList("path1")))
                                     .connectorRef(ParameterField.createValueField("g1"))
                                     .gitFetchType(FetchType.BRANCH)
                                     .branch(ParameterField.createValueField("master"))
                                     .build())
                    .build())
            .build();

    K8sManifestOutcome k8sManifestOutcome1 = K8sManifestOutcome.builder()
                                                 .identifier(k8Manifest1.getIdentifier())
                                                 .store(k8Manifest1.getStoreConfigWrapper())
                                                 .build();

    ManifestsOutcome manifestsOutcome =
        ManifestsOutcome.builder().manifestOutcomeList(Arrays.asList(k8sManifestOutcome, k8sManifestOutcome1)).build();
    StepOutcome stepOutcome = StepOutcome.builder().outcome(manifestsOutcome).name(ManifestConstants.MANIFESTS).build();

    ServiceOutcome serviceOutcome = serviceStep.createServiceOutcome(
        ServiceConfig.builder()
            .identifier(ParameterField.createValueField("s1"))
            .name(ParameterField.createValueField("s1"))
            .serviceDefinition(ServiceDefinition.builder().serviceSpec(KubernetesServiceSpec.builder().build()).build())
            .build(),
        Collections.singletonList(stepOutcome));

    assertThat(serviceOutcome.getManifests()).isNotEmpty();
    assertThat(serviceOutcome.getManifests().keySet().size()).isEqualTo(2);
    assertThat(serviceOutcome.getManifests().get(k8Manifest.getIdentifier())).isEqualTo(k8sManifestOutcome);
    assertThat(serviceOutcome.getManifests().get(k8Manifest1.getIdentifier())).isEqualTo(k8sManifestOutcome1);
  }
}
