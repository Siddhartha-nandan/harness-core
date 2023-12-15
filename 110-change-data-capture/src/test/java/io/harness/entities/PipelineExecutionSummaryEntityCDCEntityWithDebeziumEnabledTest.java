/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.cf.client.api.CfClient;
import io.harness.changehandlers.ApprovalStageExecutionDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryCIStageChangeDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryCdChangeDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew;
import io.harness.changehandlers.PlanExecutionSummaryChangeDataHandler;
import io.harness.changehandlers.PlanExecutionSummaryChangeDataHandlerAllStages;
import io.harness.changehandlers.RuntimeInputsInfoCDChangeDataHandler;
import io.harness.changehandlers.TagsInfoNGCDChangeDataHandler;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.io.IOException;
import java.sql.SQLException;
import org.joor.Reflect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabledTest extends CategoryTest {
  @Mock CfClient cfClient;
  @Mock private PlanExecutionSummaryChangeDataHandler planExecutionSummaryChangeDataHandler;
  @Mock private PlanExecutionSummaryCdChangeDataHandler planExecutionSummaryCdChangeDataHandler;
  @Mock private PlanExecutionSummaryCIStageChangeDataHandler planExecutionSummaryCIStageChangeDataHandler;
  @Mock private PlanExecutionSummaryChangeDataHandlerAllStages planExecutionSummaryChangeDataHandlerAllStages;
  @Mock private TagsInfoNGCDChangeDataHandler tagsInfoNGCDChangeDataHandler;
  @Mock private RuntimeInputsInfoCDChangeDataHandler runtimeInputsInfoCDChangeDataHandler;
  @Mock private ApprovalStageExecutionDataHandler approvalStageExecutionDataHandler;
  @Mock
  private PlanExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew
      planExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew;
  @InjectMocks PipelineExecutionSummaryEntityCDCEntity pipelineExecutionSummaryEntityCDCEntity;
  @InjectMocks
  PipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled;
  private AutoCloseable mocks;
  private final ClassLoader classLoader = this.getClass().getClassLoader();

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    Reflect.on(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled)
        .set("pipelineExecutionSummaryEntityCDCEntity", pipelineExecutionSummaryEntityCDCEntity);
  }

  @After
  public void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testGetChangeHandler() throws IOException, SQLException {
    doReturn(false).when(cfClient).boolVariation(eq(FeatureName.DEBEZIUM_ENABLED.toString()), any(), anyBoolean());
    doReturn(true).when(cfClient).boolVariation(
        eq(FeatureName.USE_CDC_FOR_PIPELINE_HANDLER.toString()), any(), anyBoolean());
    assertThat(
        pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("PipelineExecutionSummaryEntity"))
        .isEqualTo(planExecutionSummaryChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityAllStages"))
        .isEqualTo(planExecutionSummaryChangeDataHandlerAllStages);
    assertThat(
        pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("PipelineExecutionSummaryEntityCD"))
        .isEqualTo(planExecutionSummaryCdChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityServiceAndInfra"))
        .isEqualTo(planExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCIStage"))
        .isEqualTo(planExecutionSummaryCIStageChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("TagsInfoNGCD"))
        .isEqualTo(tagsInfoNGCDChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("ApprovalStage"))
        .isEqualTo(approvalStageExecutionDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("RuntimeInputsInfo"))
        .isEqualTo(runtimeInputsInfoCDChangeDataHandler);
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testGetChangeHandlerDebeziumEnabled() throws IOException, SQLException {
    doReturn(true).when(cfClient).boolVariation(eq(FeatureName.DEBEZIUM_ENABLED.toString()), any(), anyBoolean());
    doReturn(false).when(cfClient).boolVariation(
        eq(FeatureName.USE_CDC_FOR_PIPELINE_HANDLER.toString()), any(), anyBoolean());
    assertThat(
        pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("PipelineExecutionSummaryEntity"))
        .isEqualTo(planExecutionSummaryChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityAllStages"))
        .isNull();
    assertThat(
        pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("PipelineExecutionSummaryEntityCD"))
        .isNull();
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityServiceAndInfra"))
        .isEqualTo(planExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCIStage"))
        .isEqualTo(planExecutionSummaryCIStageChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("TagsInfoNGCD"))
        .isEqualTo(tagsInfoNGCDChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("ApprovalStage"))
        .isEqualTo(approvalStageExecutionDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("RuntimeInputsInfo"))
        .isEqualTo(runtimeInputsInfoCDChangeDataHandler);
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testGetChangeHandlerDebeziumEnabledPartialSync() throws IOException, SQLException {
    doReturn(true).when(cfClient).boolVariation(eq(FeatureName.DEBEZIUM_ENABLED.toString()), any(), anyBoolean());
    doReturn(false).when(cfClient).boolVariation(
        eq(FeatureName.USE_CDC_FOR_PIPELINE_HANDLER.toString()), any(), anyBoolean());
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntity", true))
        .isEqualTo(planExecutionSummaryChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityAllStages", true))
        .isEqualTo(planExecutionSummaryChangeDataHandlerAllStages);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCD", true))
        .isEqualTo(planExecutionSummaryCdChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityServiceAndInfra", true))
        .isEqualTo(planExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCIStage", true))
        .isEqualTo(planExecutionSummaryCIStageChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("TagsInfoNGCD", true))
        .isEqualTo(tagsInfoNGCDChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("ApprovalStage", true))
        .isEqualTo(approvalStageExecutionDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("RuntimeInputsInfo", true))
        .isEqualTo(runtimeInputsInfoCDChangeDataHandler);
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testGetChangeHandlerDebeziumEnabledNoPartialSync() throws IOException, SQLException {
    doReturn(true).when(cfClient).boolVariation(eq(FeatureName.DEBEZIUM_ENABLED.toString()), any(), anyBoolean());
    doReturn(false).when(cfClient).boolVariation(
        eq(FeatureName.USE_CDC_FOR_PIPELINE_HANDLER.toString()), any(), anyBoolean());
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntity", false))
        .isEqualTo(planExecutionSummaryChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityAllStages", false))
        .isNull();
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCD", false))
        .isNull();
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityServiceAndInfra", false))
        .isEqualTo(planExecutionSummaryCdChangeServiceInfraChangeDataHandlerNew);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler(
                   "PipelineExecutionSummaryEntityCIStage", false))
        .isEqualTo(planExecutionSummaryCIStageChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("TagsInfoNGCD", false))
        .isEqualTo(tagsInfoNGCDChangeDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("ApprovalStage", false))
        .isEqualTo(approvalStageExecutionDataHandler);
    assertThat(pipelineExecutionSummaryEntityCDCEntityWithDebeziumEnabled.getChangeHandler("RuntimeInputsInfo", false))
        .isEqualTo(runtimeInputsInfoCDChangeDataHandler);
  }
}
