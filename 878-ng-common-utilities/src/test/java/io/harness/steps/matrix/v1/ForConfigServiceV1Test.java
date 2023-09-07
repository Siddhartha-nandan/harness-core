/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.matrix.v1;

import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.strategy.v1.StrategyConfigV1;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.utils.NGPipelineSettingsConstant;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class ForConfigServiceV1Test extends CategoryTest {
  private ForConfigServiceV1 forConfigServiceV1 = new ForConfigServiceV1();

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testFetchChildren() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline-with-strategy-v1.yaml");
    assertThat(testFile).isNotNull();
    String pipelineYaml = Resources.toString(testFile, Charsets.UTF_8);
    String pipelineYamlWithUuid = YamlUtils.injectUuid(pipelineYaml);
    YamlField pipelineYamlField = YamlUtils.readTree(pipelineYamlWithUuid);
    assertThat(pipelineYamlField).isNotNull();
    YamlField stagesYamlField = pipelineYamlField.getNode().getField("stages");
    assertThat(stagesYamlField).isNotNull();

    List<YamlNode> stageYamlNodes = stagesYamlField.getNode().asArray();

    YamlField StageYamlField = new YamlField(stageYamlNodes.get(2));

    YamlField strategyField = StageYamlField.getNode().getField("strategy");
    StrategyConfigV1 strategyConfig = YamlUtils.read(strategyField.getNode().toString(), StrategyConfigV1.class);

    Ambiance ambiance =
        Ambiance.newBuilder()
            .setMetadata(
                ExecutionMetadata.newBuilder()
                    .putSettingToValueMap(NGPipelineSettingsConstant.ENABLE_MATRIX_FIELD_NAME_SETTING.name(), "false")
                    .build())
            .build();
    List<ChildrenExecutableResponse.Child> children =
        forConfigServiceV1.fetchChildren(strategyConfig, "childNodeId", ambiance);
    assertThat(children.size()).isEqualTo(3);
  }
}
