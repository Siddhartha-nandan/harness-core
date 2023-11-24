/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plan;

import static io.harness.pms.contracts.plan.ExecutionMode.PIPELINE_ROLLBACK;
import static io.harness.pms.contracts.plan.ExecutionMode.POST_EXECUTION_ROLLBACK;
import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.exports.ExportConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.AdvisorObtainmentList;
import io.harness.pms.contracts.plan.ExecutionMode;
import io.harness.pms.contracts.plan.Export;
import io.harness.pms.contracts.plan.ExportValue;
import io.harness.pms.contracts.plan.ExpressionMode;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.expression.ExpressionModeMapper;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PlanNodeTest extends CategoryTest {
  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void shouldTestFromExpressionModeProto() {
    for (ExpressionMode expressionMode : ExpressionMode.values()) {
      if (expressionMode == ExpressionMode.UNRECOGNIZED || expressionMode == ExpressionMode.UNKNOWN_MODE) {
        assertThat(ExpressionModeMapper.fromExpressionModeProto(expressionMode))
            .isEqualTo(io.harness.expression.common.ExpressionMode.RETURN_NULL_IF_UNRESOLVED);
        continue;
      }
      io.harness.expression.common.ExpressionMode mappedExpressionMode =
          ExpressionModeMapper.fromExpressionModeProto(expressionMode);
      assertThat(mappedExpressionMode.name()).isEqualTo(expressionMode.name());
      assertThat(mappedExpressionMode.getIndex()).isEqualTo(expressionMode.getNumber());
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testBuildAdvisorObtainmentsForExecutionMode() {
    AdvisorObtainmentList basicList =
        AdvisorObtainmentList.newBuilder()
            .addAdviserObtainments(AdviserObtainment.newBuilder().setType(AdviserType.getDefaultInstance()).build())
            .build();
    Map<String, AdvisorObtainmentList> advisorObtainments = new HashMap<>();
    advisorObtainments.put(POST_EXECUTION_ROLLBACK.name(), basicList);
    advisorObtainments.put(PIPELINE_ROLLBACK.name(), basicList);
    Map<ExecutionMode, List<AdviserObtainment>> result =
        PlanNode.buildAdvisorObtainmentsForExecutionMode(advisorObtainments);
    assertThat(result).hasSize(2);
    assertThat(result).containsKeys(POST_EXECUTION_ROLLBACK, PIPELINE_ROLLBACK);
    assertThat(result.get(POST_EXECUTION_ROLLBACK)).hasSize(1);
    assertThat(result.get(PIPELINE_ROLLBACK)).hasSize(1);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetExportsConfigMap() {
    PlanNodeProto planNodeProto = PlanNodeProto.newBuilder()
                                      .putAllExports(Map.of("export_1",
                                          Export.newBuilder()
                                              .setDesc("desc1")
                                              .setValue(ExportValue.newBuilder().setStringValue("stringVal").build())
                                              .build(),
                                          "export_2",
                                          Export.newBuilder()
                                              .setDesc("desc2")
                                              .setValue(ExportValue.newBuilder().setNumberValue(2.0).build())
                                              .build(),
                                          "export_3",
                                          Export.newBuilder()
                                              .setDesc("desc3")
                                              .setValue(ExportValue.newBuilder().setBoolValue(true).build())
                                              .build()))
                                      .build();

    PlanNode planNode = PlanNode.fromPlanNodeProto(planNodeProto);

    assertThat(planNode.getExports())
        .isEqualTo(Map.of("export_1", ExportConfig.builder().value("stringVal").desc("desc1").build(), "export_2",
            ExportConfig.builder().value(2.0).desc("desc2").build(), "export_3",
            ExportConfig.builder().value(true).desc("desc3").build()));
  }
}
