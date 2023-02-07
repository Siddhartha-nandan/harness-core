/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.yaml;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.merger.YamlConfig;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class YamlUtilsTest extends CategoryTest {
  private static String EXPECTED_YAML_WITHOUT_RUNTIME_INPUTS = "pipeline:\n"
      + "  identifier: \"managerServiceDeployment\"\n"
      + "  name: \"Manager Service Deployment\"\n"
      + "  variables:\n"
      + "  - name: \"v1\"\n"
      + "    type: \"Number\"\n"
      + "    default: 1\n"
      + "    value: \"<+input>\"\n"
      + "  stages:\n"
      + "  - stage:\n"
      + "      identifier: \"qaStage\"\n"
      + "      type: \"Deployment\"\n"
      + "      name: \"qa stage\"\n"
      + "      spec:\n"
      + "        service:\n"
      + "          identifier: \"manager\"\n"
      + "          serviceDefinition:\n"
      + "            type: \"Kubernetes\"\n"
      + "            spec:\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: \"Dockerhub\"\n"
      + "                  spec:\n"
      + "                    connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                    imagePath: \"library/nginx\"\n"
      + "                    tag: \"1.19\"\n"
      + "              manifests:\n"
      + "              - manifest:\n"
      + "                  identifier: \"baseValues\"\n"
      + "                  type: \"K8sManifest\"\n"
      + "                  spec:\n"
      + "                    store:\n"
      + "                      type: \"Git\"\n"
      + "                      spec:\n"
      + "                        connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                        gitFetchType: \"Branch\"\n"
      + "                        branch: \"master\"\n"
      + "                        paths:\n"
      + "                        - \"test/spec\"\n"
      + "          stageOverrides:\n"
      + "            manifests:\n"
      + "            - manifest:\n"
      + "                identifier: \"qaOverride\"\n"
      + "                type: \"Values\"\n"
      + "                spec:\n"
      + "                  store:\n"
      + "                    type: \"Git\"\n"
      + "                    spec:\n"
      + "                      connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                      gitFetchType: \"Branch\"\n"
      + "                      branch: \"master\"\n"
      + "                      paths:\n"
      + "                      - \"test/qa/values_1.yaml\"\n"
      + "        infrastructure:\n"
      + "          environment:\n"
      + "            identifier: \"stagingInfra\"\n"
      + "            type: \"PreProduction\"\n"
      + "            name: null\n"
      + "            tags:\n"
      + "              cloud: \"AWS\"\n"
      + "              team: \"cdp\"\n"
      + "          infrastructureDefinition:\n"
      + "            type: \"KubernetesDirect\"\n"
      + "            spec:\n"
      + "              connectorRef: \"2JrX8ESYSTWbhBTPBu7slQ\"\n"
      + "              namespace: \"default\"\n"
      + "              releaseName: \"vaibhav\"\n"
      + "        execution:\n"
      + "          steps:\n"
      + "          - step:\n"
      + "              identifier: \"rolloutDeployment\"\n"
      + "              type: \"K8sRollingDeploy\"\n"
      + "              name: \"Rollout Deployment\"\n"
      + "              spec:\n"
      + "                timeout: 120000\n"
      + "                skipDryRun: false\n"
      + "          - step:\n"
      + "              identifier: \"http\"\n"
      + "              type: \"Http\"\n"
      + "              name: \"http\"\n"
      + "              spec:\n"
      + "                socketTimeoutMillis: 1000\n"
      + "                method: \"GET\"\n"
      + "                url: \"wrongUrl\"\n"
      + "          - step:\n"
      + "              identifier: \"shellScript\"\n"
      + "              type: \"ShellScript\"\n"
      + "              spec:\n"
      + "                executeOnDelegate: true\n"
      + "                connectionType: \"SSH\"\n"
      + "                scriptType: \"BASH\"\n"
      + "                scriptString: \"echo 'I should not execute'\"\n"
      + "          - parallel:\n"
      + "            - step:\n"
      + "                identifier: \"http-step-13\"\n"
      + "                type: \"http\"\n"
      + "                name: \"http step 13\"\n"
      + "                spec:\n"
      + "                  socketTimeoutMillis: 1000\n"
      + "                  method: \"GET\"\n"
      + "                  url: \"http://localhost:8080/temp-13.json\"\n"
      + "            - step:\n"
      + "                identifier: \"http-step-14\"\n"
      + "                type: \"http\"\n"
      + "                name: \"http step 14\"\n"
      + "                spec:\n"
      + "                  socketTimeoutMillis: 1000\n"
      + "                  method: \"GET\"\n"
      + "                  url: \"http://localhost:8080/temp-14.json\"\n"
      + "          rollbackSteps:\n"
      + "          - step:\n"
      + "              identifier: \"rollbackRolloutDeployment\"\n"
      + "              type: \"K8sRollingRollback\"\n"
      + "              name: \"Rollback Rollout Deployment\"\n"
      + "              spec:\n"
      + "                timeout: 120000\n"
      + "          - step:\n"
      + "              identifier: \"shellScript2\"\n"
      + "              type: \"ShellScript\"\n"
      + "              spec:\n"
      + "                executeOnDelegate: true\n"
      + "                connectionType: \"SSH\"\n"
      + "                scriptType: \"BASH\"\n"
      + "                scriptString: \"echo 'I should be executed during rollback'\"\n"
      + "  - stage:\n"
      + "      identifier: \"prodStage\"\n"
      + "      type: \"Deployment\"\n"
      + "      name: \"prod stage\"\n"
      + "      spec:\n"
      + "        service:\n"
      + "          identifier: \"manager\"\n"
      + "          serviceDefinition:\n"
      + "            type: \"Kubernetes\"\n"
      + "            spec:\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: \"Dockerhub\"\n"
      + "                  spec:\n"
      + "                    connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                    imagePath: \"library/nginx\"\n"
      + "                    tag: \"1.18\"\n"
      + "              manifests:\n"
      + "              - manifest:\n"
      + "                  identifier: \"baseValues\"\n"
      + "                  type: \"K8sManifest\"\n"
      + "                  spec:\n"
      + "                    store:\n"
      + "                      type: \"Git\"\n"
      + "                      spec:\n"
      + "                        connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                        gitFetchType: \"Branch\"\n"
      + "                        branch: \"master\"\n"
      + "                        paths:\n"
      + "                        - \"test/spec\"\n"
      + "          stageOverrides:\n"
      + "            manifests:\n"
      + "            - manifest:\n"
      + "                identifier: \"prodOverride\"\n"
      + "                type: \"Values\"\n"
      + "                spec:\n"
      + "                  store:\n"
      + "                    type: \"Git\"\n"
      + "                    spec:\n"
      + "                      connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                      gitFetchType: \"Branch\"\n"
      + "                      branch: \"master\"\n"
      + "                      paths:\n"
      + "                      - \"test/prod/values.yaml\"\n"
      + "        infrastructure:\n"
      + "          environment:\n"
      + "            identifier: \"stagingInfra\"\n"
      + "            type: \"PreProduction\"\n"
      + "            name: null\n"
      + "            tags:\n"
      + "            - key: \"cloud\"\n"
      + "              value: \"AWS\"\n"
      + "            - key: \"team\"\n"
      + "              value: \"cdp\"\n"
      + "          infrastructureDefinition:\n"
      + "            type: \"KubernetesDirect\"\n"
      + "            spec:\n"
      + "              connectorRef: \"2JrX8ESYSTWbhBTPBu7slQ\"\n"
      + "              namespace: \"default\"\n"
      + "              releaseName: \"vaibhav\"\n"
      + "        execution:\n"
      + "          steps:\n"
      + "          - step:\n"
      + "              identifier: \"rolloutDeployment\"\n"
      + "              type: \"K8sRollingDeploy\"\n"
      + "              name: \"Rollout Deployment\"\n"
      + "              spec:\n"
      + "                timeout: 120000\n"
      + "                skipDryRun: false\n"
      + "  - parallel:\n"
      + "    - stage:\n"
      + "        identifier: \"parallelStage1\"\n"
      + "        type: \"Deployment\"\n"
      + "        name: \"parallelStage1\"\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: \"manager\"\n"
      + "            serviceDefinition:\n"
      + "              type: \"Kubernetes\"\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  primary:\n"
      + "                    type: \"Dockerhub\"\n"
      + "                    spec:\n"
      + "                      connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                      imagePath: \"library/nginx\"\n"
      + "                      tag: \"1.19\"\n"
      + "                manifests:\n"
      + "                - manifest:\n"
      + "                    identifier: \"baseValues\"\n"
      + "                    type: \"K8sManifest\"\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: \"Git\"\n"
      + "                        spec:\n"
      + "                          connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                          gitFetchType: \"Branch\"\n"
      + "                          branch: \"master\"\n"
      + "                          paths:\n"
      + "                          - \"test/spec\"\n"
      + "            stageOverrides:\n"
      + "              manifests:\n"
      + "              - manifest:\n"
      + "                  identifier: \"qaOverride\"\n"
      + "                  type: \"Values\"\n"
      + "                  spec:\n"
      + "                    store:\n"
      + "                      type: \"Git\"\n"
      + "                      spec:\n"
      + "                        connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                        gitFetchType: \"Branch\"\n"
      + "                        branch: \"master\"\n"
      + "                        paths:\n"
      + "                        - \"test/qa/values_1.yaml\"\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: \"stagingInfra\"\n"
      + "              type: \"PreProduction\"\n"
      + "              name: null\n"
      + "              tags:\n"
      + "                cloud: \"AWS\"\n"
      + "                team: \"cdp\"\n"
      + "            infrastructureDefinition:\n"
      + "              type: \"KubernetesDirect\"\n"
      + "              spec:\n"
      + "                connectorRef: \"2JrX8ESYSTWbhBTPBu7slQ\"\n"
      + "                namespace: \"default\"\n"
      + "                releaseName: \"vaibhav\"\n"
      + "          execution:\n"
      + "            steps:\n"
      + "            - step:\n"
      + "                identifier: \"rolloutDeployment\"\n"
      + "                type: \"K8sRollingDeploy\"\n"
      + "                name: \"Rollout Deployment\"\n"
      + "                spec:\n"
      + "                  timeout: 120000\n"
      + "                  skipDryRun: false\n"
      + "            - step:\n"
      + "                identifier: \"http\"\n"
      + "                type: \"Http\"\n"
      + "                name: \"http\"\n"
      + "                spec:\n"
      + "                  socketTimeoutMillis: 1000\n"
      + "                  method: \"GET\"\n"
      + "                  url: \"wrongUrl\"\n"
      + "            - step:\n"
      + "                identifier: \"shellScript\"\n"
      + "                type: \"ShellScript\"\n"
      + "                spec:\n"
      + "                  executeOnDelegate: true\n"
      + "                  connectionType: \"SSH\"\n"
      + "                  scriptType: \"BASH\"\n"
      + "                  scriptString: \"echo 'I should not execute'\"\n"
      + "            - parallel:\n"
      + "              - step:\n"
      + "                  identifier: \"http-step-13\"\n"
      + "                  type: \"http\"\n"
      + "                  name: \"http step 13\"\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: \"GET\"\n"
      + "                    url: \"http://localhost:8080/temp-13.json\"\n"
      + "              - step:\n"
      + "                  identifier: \"http-step-14\"\n"
      + "                  type: \"http\"\n"
      + "                  name: \"http step 14\"\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: \"GET\"\n"
      + "                    url: \"http://localhost:8080/temp-14.json\"\n"
      + "            rollbackSteps:\n"
      + "            - step:\n"
      + "                identifier: \"rollbackRolloutDeployment\"\n"
      + "                type: \"K8sRollingRollback\"\n"
      + "                name: \"Rollback Rollout Deployment\"\n"
      + "                spec:\n"
      + "                  timeout: 120000\n"
      + "            - step:\n"
      + "                identifier: \"shellScript2\"\n"
      + "                type: \"ShellScript\"\n"
      + "                spec:\n"
      + "                  executeOnDelegate: true\n"
      + "                  connectionType: \"SSH\"\n"
      + "                  scriptType: \"BASH\"\n"
      + "                  scriptString: \"echo 'I should be executed during rollback'\"\n"
      + "    - stage:\n"
      + "        identifier: \"parallelStage2\"\n"
      + "        type: \"Deployment\"\n"
      + "        name: \"parallelStage2\"\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: \"manager\"\n"
      + "            serviceDefinition:\n"
      + "              type: \"Kubernetes\"\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  primary:\n"
      + "                    type: \"Dockerhub\"\n"
      + "                    spec:\n"
      + "                      connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                      imagePath: \"library/nginx\"\n"
      + "                      tag: \"1.19\"\n"
      + "                manifests:\n"
      + "                - manifest:\n"
      + "                    identifier: \"baseValues\"\n"
      + "                    type: \"K8sManifest\"\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: \"Git\"\n"
      + "                        spec:\n"
      + "                          connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                          gitFetchType: \"Branch\"\n"
      + "                          branch: \"master\"\n"
      + "                          paths:\n"
      + "                          - \"test/spec\"\n"
      + "            stageOverrides:\n"
      + "              manifests:\n"
      + "              - manifest:\n"
      + "                  identifier: \"qaOverride\"\n"
      + "                  type: \"Values\"\n"
      + "                  spec:\n"
      + "                    store:\n"
      + "                      type: \"Git\"\n"
      + "                      spec:\n"
      + "                        connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                        gitFetchType: \"Branch\"\n"
      + "                        branch: \"master\"\n"
      + "                        paths:\n"
      + "                        - \"test/qa/values_1.yaml\"\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: \"stagingInfra\"\n"
      + "              type: \"PreProduction\"\n"
      + "              name: null\n"
      + "              tags:\n"
      + "                cloud: \"AWS\"\n"
      + "                team: \"cdp\"\n"
      + "            infrastructureDefinition:\n"
      + "              type: \"KubernetesDirect\"\n"
      + "              spec:\n"
      + "                connectorRef: \"2JrX8ESYSTWbhBTPBu7slQ\"\n"
      + "                namespace: \"default\"\n"
      + "                releaseName: \"vaibhav\"\n"
      + "          execution:\n"
      + "            steps:\n"
      + "            - step:\n"
      + "                identifier: \"rolloutDeployment\"\n"
      + "                type: \"K8sRollingDeploy\"\n"
      + "                name: \"Rollout Deployment\"\n"
      + "                spec:\n"
      + "                  timeout: 120000\n"
      + "                  skipDryRun: false\n"
      + "            - step:\n"
      + "                identifier: \"http\"\n"
      + "                type: \"Http\"\n"
      + "                name: \"http\"\n"
      + "                spec:\n"
      + "                  socketTimeoutMillis: 1000\n"
      + "                  method: \"GET\"\n"
      + "                  url: \"wrongUrl\"\n"
      + "            - step:\n"
      + "                identifier: \"shellScript\"\n"
      + "                type: \"ShellScript\"\n"
      + "                spec:\n"
      + "                  executeOnDelegate: true\n"
      + "                  connectionType: \"SSH\"\n"
      + "                  scriptType: \"BASH\"\n"
      + "                  scriptString: \"echo 'I should not execute'\"\n"
      + "            - parallel:\n"
      + "              - step:\n"
      + "                  identifier: \"http-step-13\"\n"
      + "                  type: \"http\"\n"
      + "                  name: \"http step 13\"\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: \"GET\"\n"
      + "                    url: \"http://localhost:8080/temp-13.json\"\n"
      + "              - step:\n"
      + "                  identifier: \"http-step-14\"\n"
      + "                  type: \"http\"\n"
      + "                  name: \"http step 14\"\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: \"GET\"\n"
      + "                    url: \"http://localhost:8080/temp-14.json\"\n"
      + "            rollbackSteps:\n"
      + "            - step:\n"
      + "                identifier: \"rollbackRolloutDeployment\"\n"
      + "                type: \"K8sRollingRollback\"\n"
      + "                name: \"Rollback Rollout Deployment\"\n"
      + "                spec:\n"
      + "                  timeout: 120000\n"
      + "            - step:\n"
      + "                identifier: \"shellScript2\"\n"
      + "                type: \"ShellScript\"\n"
      + "                spec:\n"
      + "                  executeOnDelegate: true\n"
      + "                  connectionType: \"SSH\"\n"
      + "                  scriptType: \"BASH\"\n"
      + "                  scriptString: \"echo 'I should be executed during rollback'\"\n"
      + "  - stage:\n"
      + "      identifier: \"prodStage2\"\n"
      + "      type: \"Deployment\"\n"
      + "      name: \"prod stage2\"\n"
      + "      strategy:\n"
      + "        repeat:\n"
      + "          times: 2\n"
      + "      spec:\n"
      + "        service:\n"
      + "          identifier: \"manager\"\n"
      + "          serviceDefinition:\n"
      + "            type: \"Kubernetes\"\n"
      + "            spec:\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: \"Dockerhub\"\n"
      + "                  spec:\n"
      + "                    imagePath: \"library/nginx\"\n"
      + "                    tag: \"1.18\"\n"
      + "              manifests:\n"
      + "              - manifest:\n"
      + "                  identifier: \"baseValues\"\n"
      + "                  type: \"K8sManifest\"\n"
      + "                  spec:\n"
      + "                    store:\n"
      + "                      type: \"Git\"\n"
      + "                      spec:\n"
      + "                        connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                        gitFetchType: \"Branch\"\n"
      + "                        branch: \"master\"\n"
      + "                        paths:\n"
      + "                        - \"test/spec\"\n"
      + "          stageOverrides:\n"
      + "            manifests:\n"
      + "            - manifest:\n"
      + "                identifier: \"prodOverride\"\n"
      + "                type: \"Values\"\n"
      + "                spec:\n"
      + "                  store:\n"
      + "                    type: \"Git\"\n"
      + "                    spec:\n"
      + "                      connectorRef: \"BBPSsTiCQ3OnD87_SHrSmw\"\n"
      + "                      gitFetchType: \"Branch\"\n"
      + "                      branch: \"master\"\n"
      + "                      paths:\n"
      + "                      - \"test/prod/values.yaml\"\n"
      + "        infrastructure:\n"
      + "          environment:\n"
      + "            identifier: \"stagingInfra\"\n"
      + "            type: \"PreProduction\"\n"
      + "            name: null\n"
      + "          infrastructureDefinition:\n"
      + "            type: \"KubernetesDirect\"\n"
      + "            spec:\n"
      + "              connectorRef: \"2JrX8ESYSTWbhBTPBu7slQ\"\n"
      + "              namespace: \"a\"\n"
      + "              releaseName: \"vaibhav\"\n"
      + "        execution:\n"
      + "          steps:\n"
      + "          - step:\n"
      + "              identifier: \"rolloutDeployment\"\n"
      + "              type: \"K8sRollingDeploy\"\n"
      + "              name: \"Rollout Deployment\"\n"
      + "              spec:\n"
      + "                timeout: 120000\n"
      + "                skipDryRun: false\n";
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCheckParentNode() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();

    // Stage1 Service Node
    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();
    YamlField serviceSiblingNode = serviceNode.nextSiblingNodeFromParentObject("infrastructure");
    YamlNode infraNode = stage1Node.getField("spec").getNode().getField("infrastructure").getNode();
    assertThat(serviceSiblingNode.getNode().getUuid()).isEqualTo(infraNode.getUuid());
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();

    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    YamlField step1SiblingNode =
        step1Node.nextSiblingFromParentArray("step", Arrays.asList("step", "stepGroup", "parallel"));
    YamlNode step2Node = stepsNode.asArray().get(1).getField("step").getNode();
    assertThat(step2Node.getIdentifier()).isEqualTo(step1SiblingNode.getNode().getIdentifier());

    YamlField step2PreviousSiblingSiblingNode =
        step2Node.previousSiblingFromParentArray("step", Arrays.asList("step", "stepGroup", "parallel"));
    assertThat(step1Node.getIdentifier()).isEqualTo(step2PreviousSiblingSiblingNode.getNode().getIdentifier());

    // Stage2 Node
    YamlNode stage2Node = stagesNode.getNode().asArray().get(1).getField("stage").getNode();

    YamlField siblingOfStage1 = stage1Node.nextSiblingFromParentArray("stage", Arrays.asList("stage", "parallel"));
    assertThat(siblingOfStage1.getNode().getIdentifier()).isEqualTo(stage2Node.getIdentifier());

    YamlField prevSiblingOfStage2 =
        stage2Node.previousSiblingFromParentArray("stage", Arrays.asList("stage", "parallel"));
    assertThat(prevSiblingOfStage2.getNode().getIdentifier()).isEqualTo(stage1Node.getIdentifier());

    // parallel stages node
    YamlNode parallel1Node = stagesNode.getNode().asArray().get(2).getField("parallel").getNode();

    YamlField siblingOfStage2 = stage2Node.nextSiblingFromParentArray("stage", Arrays.asList("stage", "parallel"));
    assertThat(siblingOfStage2.getNode().asArray().get(0).getField("stage").getNode().getIdentifier())
        .isEqualTo(parallel1Node.asArray().get(0).getField("stage").getNode().getIdentifier());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testgetFullyQualifiedName() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();
    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();

    String stageFQN = YamlUtils.getFullyQualifiedName(stage1Node);
    assertThat(stageFQN).isEqualTo("pipeline.stages.qaStage");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(stage1Node)).isEqualTo("pipeline.stages.qaStage");
    // Stage1 Service Node
    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(serviceNode)).isEqualTo("pipeline.stages.qaStage.spec.service");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(serviceNode)).isEqualTo("pipeline.stages.qaStage.spec.service");

    // image Path qualified Name
    YamlNode imagePath = serviceNode.getField("serviceDefinition")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("artifacts")
                             .getNode()
                             .getField("primary")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("imagePath")
                             .getNode();
    assertThat(YamlUtils.getFullyQualifiedName(imagePath))
        .isEqualTo("pipeline.stages.qaStage.spec.service.serviceDefinition.spec.artifacts.primary.spec.imagePath");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(imagePath))
        .isEqualTo("pipeline.stages.qaStage.spec.service.serviceDefinition.spec.artifacts.primary.spec.imagePath");

    // infrastructure qualified name
    YamlNode infraNode = stage1Node.getField("spec").getNode().getField("infrastructure").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(infraNode)).isEqualTo("pipeline.stages.qaStage.spec.infrastructure");

    // step qualified name
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(step1Node))
        .isEqualTo("pipeline.stages.qaStage.spec.execution.steps.rolloutDeployment");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(step1Node))
        .isEqualTo("pipeline.stages.qaStage.spec.execution.steps.rolloutDeployment");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testgetFullyQualifiedNameWithStrategy() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();
    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlNode stage1Node = stagesNode.getNode().asArray().get(3).getField("stage").getNode();

    String stageFQN = YamlUtils.getFullyQualifiedName(stage1Node, true);
    assertThat(stageFQN).isEqualTo("pipeline.stages.prodStage2<+strategy.identifierPostFix>");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(stage1Node)).isEqualTo("pipeline.stages.prodStage2");
    // Stage1 Service Node
    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(serviceNode, true))
        .isEqualTo("pipeline.stages.prodStage2<+strategy.identifierPostFix>.spec.service");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(serviceNode))
        .isEqualTo("pipeline.stages.prodStage2.spec.service");

    // image Path qualified Name
    YamlNode imagePath = serviceNode.getField("serviceDefinition")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("artifacts")
                             .getNode()
                             .getField("primary")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("imagePath")
                             .getNode();
    assertThat(YamlUtils.getFullyQualifiedName(imagePath, true))
        .isEqualTo(
            "pipeline.stages.prodStage2<+strategy.identifierPostFix>.spec.service.serviceDefinition.spec.artifacts.primary.spec.imagePath");

    // infrastructure qualified name
    YamlNode infraNode = stage1Node.getField("spec").getNode().getField("infrastructure").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(infraNode, true))
        .isEqualTo("pipeline.stages.prodStage2<+strategy.identifierPostFix>.spec.infrastructure");

    // step qualified name
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    assertThat(YamlUtils.getFullyQualifiedName(step1Node, true))
        .isEqualTo("pipeline.stages.prodStage2<+strategy.identifierPostFix>.spec.execution.steps.rolloutDeployment");
    assertThat(YamlUtils.getFullyQualifiedNameTillRoot(step1Node))
        .isEqualTo("pipeline.stages.prodStage2.spec.execution.steps.rolloutDeployment");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testgetQNBetweenTwoFields() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();

    // Stage1 Service Node
    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();

    // image Path qualified Name
    YamlNode imagePath = serviceNode.getField("serviceDefinition")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("artifacts")
                             .getNode()
                             .getField("primary")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("imagePath")
                             .getNode();
    assertThat(YamlUtils.getQNBetweenTwoFields(imagePath, "stages", "service")).isEqualTo("stages.service");

    // step qualified name
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    assertThat(YamlUtils.getQNBetweenTwoFields(step1Node, "execution", "steps")).isEqualTo("execution.steps");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testQNTillGivenFieldName() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Stage1 Node
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();

    String stageFQN = YamlUtils.getQualifiedNameTillGivenField(stage1Node, "stage");
    assertThat(stageFQN).isEqualTo("qaStage");
    // Stage1 Service Node
    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();
    assertThat(YamlUtils.getQualifiedNameTillGivenField(serviceNode, "stage")).isEqualTo("qaStage.spec.service");

    // image Path qualified Name
    YamlNode imagePath = serviceNode.getField("serviceDefinition")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("artifacts")
                             .getNode()
                             .getField("primary")
                             .getNode()
                             .getField("spec")
                             .getNode()
                             .getField("imagePath")
                             .getNode();
    assertThat(YamlUtils.getQualifiedNameTillGivenField(imagePath, "service"))
        .isEqualTo("service.serviceDefinition.spec.artifacts.primary.spec.imagePath");

    // infrastructure qualified name
    YamlNode infraNode = stage1Node.getField("spec").getNode().getField("infrastructure").getNode();
    assertThat(YamlUtils.getQualifiedNameTillGivenField(infraNode, "infrastructure")).isEqualTo("infrastructure");

    // step qualified name
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    assertThat(YamlUtils.getQualifiedNameTillGivenField(step1Node, "execution"))
        .isEqualTo("execution.steps.rolloutDeployment");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStageIdentifierFromFqn() {
    String fqn = "pipeline.stages.qaStage.spec.execution.steps.rolloutDeployment";
    assertThat("qaStage").isEqualTo(YamlUtils.getStageIdentifierFromFqn(fqn));

    fqn = "pipeline.stages";
    assertThat(YamlUtils.getStageIdentifierFromFqn(fqn)).isNull();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetPipelineVariableNameFromFqn() {
    String fqn = "pipeline.variables.var1";
    assertThat("var1").isEqualTo(YamlUtils.getPipelineVariableNameFromFqn(fqn));

    fqn = "pipeline.variables";
    assertThat(YamlUtils.getPipelineVariableNameFromFqn(fqn)).isNull();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStageFqn() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // step qualified name
    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();
    YamlNode stepsNode =
        stage1Node.getField("spec").getNode().getField("execution").getNode().getField("steps").getNode();
    YamlNode step1Node = stepsNode.asArray().get(0).getField("step").getNode();
    assertThat(YamlUtils.getStageFqnPath(step1Node)).isEqualTo("pipeline.stages.qaStage");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRemoveInputsFromYaml() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline-with-inputs.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    assertThat(EXPECTED_YAML_WITHOUT_RUNTIME_INPUTS)
        .isEqualTo(YamlUtils.getYamlWithoutInputs(new YamlConfig(yamlContent)));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testRemoveInputsFromYamlForComplicatedScript() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline-with-complex-script.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    // method call to confirm that no exception is thrown
    YamlUtils.getYamlWithoutInputs(new YamlConfig(yamlContent));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testCoercionConfig() throws IOException {
    assertThat(YamlUtils.read("\"\"", LinkedHashMap.class)).isNull();
    assertThat(YamlUtils.read("\"\"", ArrayList.class)).isEmpty();
  }
}
