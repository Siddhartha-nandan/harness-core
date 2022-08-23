/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.manifest;

import static io.harness.cdng.creator.plan.manifest.ManifestsPlanCreator.SERVICE_ENTITY_DEFINITION_TYPE_KEY;
import static io.harness.cdng.manifest.ManifestType.HELM_SUPPORTED_MANIFEST_TYPES;
import static io.harness.cdng.manifest.ManifestType.K8S_SUPPORTED_MANIFEST_TYPES;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.ManifestsListConfigWrapper;
import io.harness.cdng.manifest.steps.ManifestStepParameters;
import io.harness.cdng.manifest.steps.ManifestsStep;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.NativeHelmServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.fork.ForkStepParameters;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDC)
public class ManifestPlanCreatorTest extends CDNGTestBase {
  @Inject private KryoSerializer kryoSerializer;
  @Inject @InjectMocks ManifestsPlanCreator manifestsPlanCreator;

  private static final String ACCOUNT_ID = "account_id";
  private static final String ORG_IDENTIFIER = "orgId";
  private static final String PROJ_IDENTIFIER = "projId";
  private static final String SERVICE_IDENTIFIER = "service1";

  private YamlField getYamlFieldFromGivenFileName(String file) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream(file);
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField yamlField = YamlUtils.readTree(yaml);
    return yamlField;
  }

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldNotAllowDuplicateManifestIdentifiers() throws IOException {
    ManifestConfigWrapper k8sManifest =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("test").type(ManifestConfigType.K8_MANIFEST).build())
            .build();
    ManifestConfigWrapper valuesManifest =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("test").type(ManifestConfigType.VALUES).build())
            .build();

    ServiceConfig serviceConfig =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().manifests(Arrays.asList(k8sManifest, valuesManifest)).build())
                    .build())
            .build();

    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(
        YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceConfig)));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    YamlField manifestsYamlField = getYamlFieldFromGivenFileName("cdng/plan/manifests/manifests.yml");
    PlanCreationContext ctx =
        PlanCreationContext.builder().currentField(manifestsYamlField).dependency(dependency).build();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> manifestsPlanCreator.createPlanForChildrenNodes(ctx, null))
        .withMessageContaining("Duplicate identifier: [test] in manifests");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldCreateWithProperOrder() throws IOException {
    ServiceConfig serviceConfig =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(KubernetesServiceSpec.builder()
                                     .manifests(Arrays.asList(manifestWith("m1", ManifestConfigType.K8_MANIFEST),
                                         manifestWith("m2", ManifestConfigType.VALUES),
                                         manifestWith("m3", ManifestConfigType.VALUES)))
                                     .build())
                    .build())
            .stageOverrides(
                StageOverridesConfig.builder()
                    .manifests(Arrays.asList(manifestWith("m1", ManifestConfigType.K8_MANIFEST),
                        manifestWith("m4", ManifestConfigType.VALUES), manifestWith("m2", ManifestConfigType.VALUES),
                        manifestWith("m5", ManifestConfigType.VALUES), manifestWith("m6", ManifestConfigType.VALUES),
                        manifestWith("m3", ManifestConfigType.VALUES)))
                    .build())
            .build();

    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(
        YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceConfig)));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    YamlField manifestsYamlField = getYamlFieldFromGivenFileName("cdng/plan/manifests/manifests.yml");

    PlanCreationContext ctx =
        PlanCreationContext.builder().currentField(manifestsYamlField).dependency(dependency).build();

    List<String> expectedListrOfIdentifier = Arrays.asList("m1", "m2", "m3", "m4", "m5", "m6");
    List<String> actualListOfIdentifier = new ArrayList<>();
    LinkedHashMap<String, PlanCreationResponse> response = manifestsPlanCreator.createPlanForChildrenNodes(ctx, null);
    assertThat(response.size()).isEqualTo(6);
    for (Map.Entry<String, PlanCreationResponse> entry : response.entrySet()) {
      actualListOfIdentifier.add(fetchManifestIdentifier(entry.getKey(), entry.getValue()));
    }
    assertThat(expectedListrOfIdentifier).isEqualTo(actualListOfIdentifier);
  }

  private String fetchManifestIdentifier(String uuid, PlanCreationResponse planCreationResponse) {
    Map<String, ByteString> metadataMap =
        planCreationResponse.getDependencies().getDependencyMetadataMap().get(uuid).getMetadataMap();
    assertThat(metadataMap.size()).isEqualTo(2);
    assertThat(metadataMap.containsKey(YamlTypes.UUID)).isEqualTo(true);
    assertThat(metadataMap.containsKey(PlanCreatorConstants.MANIFEST_STEP_PARAMETER)).isEqualTo(true);

    ManifestStepParameters stepParameters = (ManifestStepParameters) kryoSerializer.asInflatedObject(
        metadataMap.get(PlanCreatorConstants.MANIFEST_STEP_PARAMETER).toByteArray());
    return stepParameters.getIdentifier();
  }

  private ManifestConfigWrapper manifestWith(String identifier, ManifestConfigType type) {
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder().identifier(identifier).type(type).build())
        .build();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    assertThat(manifestsPlanCreator.getFieldClass()).isEqualTo(ManifestsListConfigWrapper.class);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetSupportedTypes() {
    Map<String, Set<String>> supportedTypes = manifestsPlanCreator.getSupportedTypes();
    assertThat(supportedTypes.containsKey(YamlTypes.MANIFEST_LIST_CONFIG)).isEqualTo(true);
    assertThat(supportedTypes.get(YamlTypes.MANIFEST_LIST_CONFIG).contains(PlanCreatorUtils.ANY_TYPE)).isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetParentNode() {
    List<String> childrenNodeId = Arrays.asList("child1", "child2");
    HashMap<String, ByteString> metadataDependency = new HashMap<>();
    String uuid = UUIDGenerator.generateUuid();
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(uuid)));
    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    PlanCreationContext ctx = PlanCreationContext.builder().dependency(dependency).build();
    PlanNode planForParentNode = manifestsPlanCreator.createPlanForParentNode(ctx, null, childrenNodeId);
    assertThat(planForParentNode.getUuid()).isEqualTo(uuid);
    assertThat(planForParentNode.getStepType()).isEqualTo(ManifestsStep.STEP_TYPE);
    assertThat(planForParentNode.getName()).isEqualTo(PlanCreatorConstants.MANIFESTS_NODE_NAME);
    assertThat(planForParentNode.getStepParameters())
        .isEqualTo(ForkStepParameters.builder().parallelNodeIds(childrenNodeId).build());
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAddDependenciesForIndividualManifests() throws IOException {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/manifests/manifests.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField manifestsListYamlField = YamlUtils.readTree(yaml);

    List<YamlNode> yamlNodes = Optional.of(manifestsListYamlField.getNode().asArray()).orElse(Collections.emptyList());
    Map<String, YamlNode> mapIdentifierWithYamlNode = yamlNodes.stream().collect(
        Collectors.toMap(e -> e.getField(YamlTypes.MANIFEST_CONFIG).getNode().getIdentifier(), k -> k));

    String manifestIdentifier = yamlNodes.get(0).getField("manifest").getNode().getIdentifier();
    PlanCreationContext ctx = PlanCreationContext.builder().currentField(manifestsListYamlField).build();
    ManifestStepParameters manifestStepParameters = ManifestStepParameters.builder().build();
    String nodeUuid = manifestsPlanCreator.addDependenciesForIndividualManifest(ctx.getCurrentField(),
        manifestIdentifier, manifestStepParameters, mapIdentifierWithYamlNode, planCreationResponseMap);

    assertThat(planCreationResponseMap.size()).isEqualTo(1);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    PlanCreationResponse planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid)).isEqualTo("[0]/manifest");
    assertThat(planCreationResponse1.getYamlUpdates()).isNull();

    // For yamlNode1
    manifestIdentifier = yamlNodes.get(1).getField("manifest").getNode().getIdentifier();
    nodeUuid = manifestsPlanCreator.addDependenciesForIndividualManifest(ctx.getCurrentField(), manifestIdentifier,
        manifestStepParameters, mapIdentifierWithYamlNode, planCreationResponseMap);

    assertThat(planCreationResponseMap.size()).isEqualTo(2);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid)).isEqualTo("[1]/manifest");
    assertThat(planCreationResponse1.getYamlUpdates()).isNull();

    // case3: yamlNode which is not present in map
    nodeUuid = manifestsPlanCreator.addDependenciesForIndividualManifest(ctx.getCurrentField(), "identifier",
        manifestStepParameters, mapIdentifierWithYamlNode, planCreationResponseMap);

    assertThat(planCreationResponseMap.size()).isEqualTo(3);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid)).isEqualTo("[0]/manifest");
    assertThat(planCreationResponse1.getYamlUpdates()).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreatePlanForChildrenNodesDuplicateK8sManifests() throws IOException {
    ServiceSpec serviceSpec = KubernetesServiceSpec.builder()
                                  .manifests(Arrays.asList(manifestWith("manifest1", ManifestConfigType.K8_MANIFEST),
                                      manifestWith("manifest2", ManifestConfigType.KUSTOMIZE),
                                      manifestWith("manifest3", ManifestConfigType.OPEN_SHIFT_TEMPLATE),
                                      manifestWith("manifest4", ManifestConfigType.HELM_CHART),
                                      manifestWith("manifest5", ManifestConfigType.K8_MANIFEST)))
                                  .build();

    testCreatePlanForChildrenNodesDuplicateDeploymentsManifests(
        ServiceDefinitionType.KUBERNETES, serviceSpec, K8S_SUPPORTED_MANIFEST_TYPES);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreatePlanForChildrenNodesDuplicateNativeHelmManifests() throws IOException {
    ServiceSpec serviceSpec = NativeHelmServiceSpec.builder()
                                  .manifests(Arrays.asList(manifestWith("manifest1", ManifestConfigType.HELM_CHART),
                                      manifestWith("manifest2", ManifestConfigType.HELM_CHART)))
                                  .build();

    testCreatePlanForChildrenNodesDuplicateDeploymentsManifests(
        ServiceDefinitionType.NATIVE_HELM, serviceSpec, HELM_SUPPORTED_MANIFEST_TYPES);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testCreatePlanForChildrenNodesWithServiceEntity() throws IOException {
    final ManifestConfigWrapper valuesManifest1 =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("values_test1").type(ManifestConfigType.VALUES).build())
            .build();
    final ManifestConfigWrapper valuesManifest2 =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("values_test2").type(ManifestConfigType.VALUES).build())
            .build();
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(SERVICE_ENTITY_DEFINITION_TYPE_KEY,
        ByteString.copyFrom(kryoSerializer.asDeflatedBytes(ServiceDefinitionType.KUBERNETES)));
    metadataDependency.put(YamlTypes.MANIFEST_LIST_CONFIG,
        ByteString.copyFrom(kryoSerializer.asDeflatedBytes(Arrays.asList(valuesManifest1, valuesManifest2))));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    YamlField manifestsYamlField = getYamlFieldFromGivenFileName("cdng/plan/manifests/manifests.yml");

    Map<String, PlanCreationContextValue> globalContext = new HashMap<>();
    globalContext.put("metadata",
        PlanCreationContextValue.newBuilder()
            .setAccountIdentifier(ACCOUNT_ID)
            .setOrgIdentifier(ORG_IDENTIFIER)
            .setProjectIdentifier(PROJ_IDENTIFIER)
            .build());
    PlanCreationContext ctx = PlanCreationContext.builder()
                                  .currentField(manifestsYamlField)
                                  .globalContext(globalContext)
                                  .dependency(dependency)
                                  .build();
    List<String> expectedListOfIdentifier = Arrays.asList("values_test1", "values_test2");
    List<String> actualListOfIdentifier = new ArrayList<>();
    LinkedHashMap<String, PlanCreationResponse> response = manifestsPlanCreator.createPlanForChildrenNodes(ctx, null);
    assertThat(response.size()).isEqualTo(2);
    for (Map.Entry<String, PlanCreationResponse> entry : response.entrySet()) {
      actualListOfIdentifier.add(fetchManifestIdentifier(entry.getKey(), entry.getValue()));
    }
    assertThat(expectedListOfIdentifier).isEqualTo(actualListOfIdentifier);
  }

  private void testCreatePlanForChildrenNodesDuplicateDeploymentsManifests(
      ServiceDefinitionType type, ServiceSpec serviceSpec, Set<String> expectedSupport) throws IOException {
    ServiceConfig serviceConfig =
        ServiceConfig.builder()
            .serviceDefinition(ServiceDefinition.builder().type(type).serviceSpec(serviceSpec).build())
            .build();

    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(
        YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceConfig)));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    YamlField manifestsYamlField = getYamlFieldFromGivenFileName("cdng/plan/manifests/manifests.yml");

    PlanCreationContext ctx =
        PlanCreationContext.builder().currentField(manifestsYamlField).dependency(dependency).build();
    assertThatThrownBy(() -> manifestsPlanCreator.createPlanForChildrenNodes(ctx, null))
        .isInstanceOf(InvalidRequestException.class)
        .matches(exception -> {
          String message = exception.getMessage();
          String expectedMessage =
              String.format("%s deployment support only one manifest of one of types: %s. Remove all unused manifests",
                  type.getYamlName(), String.join(", ", expectedSupport));
          assertThat(message).endsWith(expectedMessage);
          serviceSpec.getManifests()
              .stream()
              .map(manifest
                  -> manifest.getManifest().getIdentifier() + " : " + manifest.getManifest().getType().getDisplayName())
              .forEach(manifestIdAndType -> assertThat(message).contains(manifestIdAndType));
          return true;
        });
  }
}
