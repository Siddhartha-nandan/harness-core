/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ADITHYA;
import static io.harness.rule.OwnerRule.NAMAN;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.yaml.PipelineVersion;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class ValidateAndMergeHelperTest extends PipelineServiceTestBase {
  @InjectMocks ValidateAndMergeHelper validateAndMergeHelper;
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PMSInputSetService pmsInputSetService;
  @Mock GitSyncSdkService gitSyncSdkService;
  @Mock PMSExecutionService pmsExecutionService;

  private static final String accountId = "accountId";
  private static final String orgId = "orgId";
  private static final String projectId = "projectId";
  private static final String pipelineId = "Test_Pipline11";

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetPipelineTemplate() {
    String pipelineStart = "pipeline:\n"
        + "  stages:\n";
    String stage1 = "  - stage:\n"
        + "      identifier: \"s1\"\n"
        + "      myField: \"<+input>\"\n";
    String stage2 = "  - stage:\n"
        + "      identifier: \"s2\"\n"
        + "      myField: \"<+input>\"\n";
    String pipelineYaml = pipelineStart + stage1 + stage2;

    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).build();
    doReturn(Optional.of(pipelineEntity))
        .when(pmsPipelineService)
        .getPipeline(accountId, orgId, projectId, pipelineId, false, false);
    String pipelineTemplate = validateAndMergeHelper.getPipelineTemplate(accountId, orgId, projectId, pipelineId, null);
    assertThat(pipelineTemplate).isEqualTo(pipelineYaml);

    String s1Template = validateAndMergeHelper.getPipelineTemplate(
        accountId, orgId, projectId, pipelineId, Collections.singletonList("s1"));
    assertThat(s1Template).isEqualTo(pipelineStart + stage1);

    doReturn(Optional.empty())
        .when(pmsPipelineService)
        .getPipeline(accountId, orgId, projectId, pipelineId, false, false);
    assertThatThrownBy(() -> validateAndMergeHelper.getPipelineTemplate(accountId, orgId, projectId, pipelineId, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(format("Pipeline [%s] under Project[%s], Organization [%s] doesn't exist or has been deleted.",
            pipelineId, projectId, orgId));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInputSetTemplateResponseDTOWithNoRuntime() {
    doReturn(Optional.empty())
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, pipelineId, false);
    assertThatThrownBy(
        () -> validateAndMergeHelper.getInputSetTemplateResponseDTO(accountId, orgId, projectId, pipelineId, null))
        .isInstanceOf(InvalidRequestException.class);
    String pipelineYamlWithNoRuntime = getPipelineYamlWithNoRuntime();
    PipelineEntity pipelineEntityWithNoRuntime =
        PipelineEntity.builder().yaml(pipelineYamlWithNoRuntime).filters(Collections.singletonMap("pms", null)).build();
    doReturn(Optional.of(pipelineEntityWithNoRuntime))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, "no_runtime", false);
    doReturn(false).when(pmsInputSetService).checkForInputSetsForPipeline(accountId, orgId, projectId, "no_runtime");
    InputSetTemplateResponseDTOPMS responseWithNoRuntime =
        validateAndMergeHelper.getInputSetTemplateResponseDTO(accountId, orgId, projectId, "no_runtime", null);
    assertThat(responseWithNoRuntime.getHasInputSets()).isFalse();
    assertThat(responseWithNoRuntime.getModules()).containsExactly("pms");
    assertThat(responseWithNoRuntime.getReplacedExpressions()).isNull();
    assertThat(responseWithNoRuntime.getInputSetTemplateYaml()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetInputSetTemplateResponseDTOWithRuntime() {
    String pipelineYamlWithRuntime = getPipelineYamlWithRuntime();
    PipelineEntity pipelineEntityWithRuntime =
        PipelineEntity.builder().yaml(pipelineYamlWithRuntime).filters(Collections.singletonMap("pms", null)).build();
    doReturn(Optional.of(pipelineEntityWithRuntime))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, "has_runtime", false);
    doReturn(true).when(pmsInputSetService).checkForInputSetsForPipeline(accountId, orgId, projectId, "has_runtime");
    InputSetTemplateResponseDTOPMS responseWithNoRuntime =
        validateAndMergeHelper.getInputSetTemplateResponseDTO(accountId, orgId, projectId, "has_runtime", null);
    assertThat(responseWithNoRuntime.getHasInputSets()).isTrue();
    assertThat(responseWithNoRuntime.getModules()).containsExactly("pms");
    assertThat(responseWithNoRuntime.getReplacedExpressions()).isNull();
    assertThat(responseWithNoRuntime.getInputSetTemplateYaml()).isEqualTo(getRuntimeTemplate());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetMergeInputSetFromPipelineTemplate() {
    doReturn(false).when(gitSyncSdkService).isGitSyncEnabled(accountId, orgId, projectId);
    String pipelineYaml = "pipeline:\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      identifier: s1\n"
        + "      key: <+input>\n"
        + "  - stage:\n"
        + "      identifier: s2\n"
        + "      key1: <+input>\n"
        + "      key2: <+input>\n"
        + "      key3: <+input>";
    PipelineEntity pipeline = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.REMOTE).build();
    doReturn(Optional.of(pipeline))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, pipelineId, false);

    String yamlForS1 = "inputSet:\n"
        + "  pipeline:\n"
        + "    stages:\n"
        + "    - stage:\n"
        + "        identifier: s1\n"
        + "        key: s1Value1";
    InputSetEntity forS1 = InputSetEntity.builder()
                               .yaml(yamlForS1)
                               .inputSetEntityType(InputSetEntityType.INPUT_SET)
                               .storeType(StoreType.REMOTE)
                               .build();
    doReturn(Optional.of(forS1))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "forS1", false);

    String yamlForS1AndS2 = "inputSet:\n"
        + "  pipeline:\n"
        + "    stages:\n"
        + "    - stage:\n"
        + "        identifier: s1\n"
        + "        key: s1Value2\n"
        + "    - stage:\n"
        + "        identifier: s2\n"
        + "        key1: s2Value1\n"
        + "        key2: s2Value2\n"
        + "        key3: s2Value3";
    InputSetEntity forS1AndS2 = InputSetEntity.builder()
                                    .yaml(yamlForS1AndS2)
                                    .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                    .storeType(StoreType.REMOTE)
                                    .build();
    doReturn(Optional.of(forS1AndS2))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "forS1AndS2", false);

    String yamlForS2 = "inputSet:\n"
        + "  pipeline:\n"
        + "    stages:\n"
        + "    - stage:\n"
        + "        identifier: s2\n"
        + "        key1: s2Value2FromForS2\n";
    InputSetEntity forS2 = InputSetEntity.builder()
                               .yaml(yamlForS2)
                               .inputSetEntityType(InputSetEntityType.INPUT_SET)
                               .storeType(StoreType.REMOTE)
                               .build();
    doReturn(Optional.of(forS2))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "forS2", false);

    String mergedInputSet = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId, orgId, projectId,
        pipelineId, Arrays.asList("forS1", "forS1AndS2", "forS2"), null, null, Collections.singletonList("s2"));
    assertThat(mergedInputSet)
        .isEqualTo("pipeline:\n"
            + "  stages:\n"
            + "  - stage:\n"
            + "      identifier: \"s2\"\n"
            + "      key1: \"s2Value2FromForS2\"\n"
            + "      key2: \"s2Value2\"\n"
            + "      key3: \"s2Value3\"\n");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetMergeInputSetV1() {
    String inputSetId1 = "inputSet1";
    String inputSetId2 = "inputSet2";
    String inputSetId3 = "inputSet3";
    String inputSetId4 = "inputSet4";
    String overlayId = "overlayId";
    String pipelineYaml = "stages:\n"
        + "  - name: custom"
        + "    spec:"
        + "      type: Http:"
        + "      spec:"
        + "        url: google.com";

    InputSetEntity inputSet1 = InputSetEntity.builder()
                                   .identifier(inputSetId1)
                                   .yaml("inputs:\n"
                                       + "  image: alpine\n")
                                   .harnessVersion(PipelineVersion.V1)
                                   .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                   .storeType(StoreType.INLINE)
                                   .build();

    InputSetEntity inputSet2 = InputSetEntity.builder()
                                   .identifier(inputSetId1)
                                   .yaml("inputs:\n"
                                       + "  method: POST\n")
                                   .harnessVersion(PipelineVersion.V1)
                                   .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                   .storeType(StoreType.INLINE)
                                   .build();
    InputSetEntity inputSet3 = InputSetEntity.builder()
                                   .identifier(inputSetId3)
                                   .yaml("inputs:\n"
                                       + "  url: google.com\n")
                                   .harnessVersion(PipelineVersion.V1)
                                   .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                   .storeType(StoreType.INLINE)
                                   .build();
    InputSetEntity inputSet4 = InputSetEntity.builder()
                                   .identifier(inputSetId4)
                                   .yaml("inputs:\n"
                                       + "  timeout: 10h\n")
                                   .harnessVersion(PipelineVersion.V1)
                                   .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                   .storeType(StoreType.INLINE)
                                   .build();

    InputSetEntity overlay = InputSetEntity.builder()
                                 .identifier(overlayId)
                                 .inputSetReferences(Arrays.asList(inputSetId3, inputSetId4))
                                 .harnessVersion(PipelineVersion.V1)
                                 .inputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET)
                                 .storeType(StoreType.INLINE)
                                 .build();

    PipelineEntity pipeline = PipelineEntity.builder()
                                  .harnessVersion(PipelineVersion.V1)
                                  .yaml(pipelineYaml)
                                  .storeType(StoreType.INLINE)
                                  .build();
    doReturn(Optional.of(pipeline))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, pipelineId, false);

    doReturn(Optional.of(inputSet1))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, inputSetId1, false);
    doReturn(Optional.of(inputSet2))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, inputSetId2, false);
    doReturn(Optional.of(inputSet3))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, inputSetId3, false);
    doReturn(Optional.of(inputSet4))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, inputSetId4, false);
    doReturn(Optional.of(overlay))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, overlayId, false);

    String mergedInputSets = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(
        accountId, orgId, projectId, pipelineId, Arrays.asList(inputSetId1, inputSetId2, overlayId), null, null, null);
    assertThat(mergedInputSets)
        .isEqualTo("inputs:\n"
            + "  image: \"alpine\"\n"
            + "  method: \"POST\"\n"
            + "  url: \"google.com\"\n"
            + "  timeout: \"10h\"\n");
  }
  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testGetMergeInputSetFromPipelineTemplateWhenPipelineIsRemoteAndInputSetIsInline() {
    String pipelineYaml = "pipeline:\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      identifier: s1\n"
        + "      key: <+input>\n"
        + "  - stage:\n"
        + "      identifier: s2\n"
        + "      key1: <+input>";
    PipelineEntity pipeline = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.REMOTE).build();
    doReturn(Optional.of(pipeline))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, pipelineId, false);

    String yamlForS1 = "inputSet:\n"
        + "  pipeline:\n"
        + "    stages:\n"
        + "    - stage:\n"
        + "        identifier: s1\n"
        + "        key: s1Value1";
    InputSetEntity forS1 = InputSetEntity.builder()
                               .identifier("s1")
                               .yaml(yamlForS1)
                               .inputSetEntityType(InputSetEntityType.INPUT_SET)
                               .storeType(StoreType.INLINE)
                               .build();
    doReturn(Optional.of(forS1))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "forS1", false);

    assertThatThrownBy(()
                           -> validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId, orgId, projectId,
                               pipelineId, List.of("forS1"), null, null, Collections.singletonList("s2")))
        .isInstanceOf(WingsException.class)
        .hasMessage("Please move the input-set from inline to remote.");
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testGetMergeInputSetFromPipelineTemplateWhenPipelineIsRemoteAndOverlaidInputSetIsInline() {
    String pipelineYaml = "pipeline:\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      identifier: s1\n"
        + "      key: <+input>\n"
        + "  - stage:\n"
        + "      identifier: s2\n"
        + "      key1: <+input>";
    PipelineEntity pipeline = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.REMOTE).build();
    doReturn(Optional.of(pipeline))
        .when(pmsPipelineService)
        .getAndValidatePipeline(accountId, orgId, projectId, pipelineId, false);

    String yamlForS1 = "inputSet:\n"
        + "  pipeline:\n"
        + "    stages:\n"
        + "    - stage:\n"
        + "        identifier: s1\n"
        + "        key: s1Value1";

    InputSetEntity forS1 = InputSetEntity.builder()
                               .identifier("forS1")
                               .yaml(yamlForS1)
                               .inputSetEntityType(InputSetEntityType.INPUT_SET)
                               .storeType(StoreType.INLINE)
                               .build();

    doReturn(Optional.of(forS1))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "forS1", false);

    InputSetEntity overlaidIS = InputSetEntity.builder()
                                    .identifier("overlaidIS1")
                                    .yaml(yamlForS1)
                                    .storeType(StoreType.REMOTE)
                                    .inputSetReferences(Collections.singletonList("forS1"))
                                    .build();
    doReturn(Optional.of(overlaidIS))
        .when(pmsInputSetService)
        .getWithoutValidations(accountId, orgId, projectId, pipelineId, "overlaidIS1", false);

    assertThatThrownBy(()
                           -> validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId, orgId, projectId,
                               pipelineId, List.of("overlaidIS1"), null, null, Collections.singletonList("s2")))
        .isInstanceOf(WingsException.class)
        .hasMessage("Please move the input-set from inline to remote.");
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testCheckAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferentWhenStoreTypeIsNull() {
    PipelineEntity pipeline = PipelineEntity.builder().build();
    InputSetEntity inputSet = InputSetEntity.builder().build();

    assertDoesNotThrow(
        ()
            -> validateAndMergeHelper.checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(
                pipeline, inputSet));
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testCheckAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferentWhenStoreTypesAreSame() {
    PipelineEntity pipeline = PipelineEntity.builder().storeType(StoreType.REMOTE).build();
    InputSetEntity inputSet = InputSetEntity.builder().storeType(StoreType.REMOTE).build();
    assertDoesNotThrow(
        ()
            -> validateAndMergeHelper.checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(
                pipeline, inputSet));
  }

  @Test
  @Owner(developers = ADITHYA)
  @Category(UnitTests.class)
  public void testCheckAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferentWhenStoreTypesAreDifferent() {
    PipelineEntity pipeline = PipelineEntity.builder().storeType(StoreType.INLINE).build();
    InputSetEntity inputSet = InputSetEntity.builder().storeType(StoreType.REMOTE).build();
    assertThatThrownBy(
        ()
            -> validateAndMergeHelper.checkAndThrowExceptionWhenPipelineAndInputSetStoreTypesAreDifferent(
                pipeline, inputSet))
        .isInstanceOf(WingsException.class)
        .hasMessage("Please move the input-set from inline to remote.");
  }

  private String getPipelineYamlWithNoRuntime() {
    return "pipeline:\n"
        + "  name: no runtime\n"
        + "  identifier: no_runtime\n"
        + "  projectIdentifier: namantest\n"
        + "  orgIdentifier: default\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      name: a1\n"
        + "      identifier: a1\n"
        + "      type: Approval\n"
        + "      spec:\n"
        + "        execution:\n"
        + "          steps:\n"
        + "          - step:\n"
        + "              name: Approval\n"
        + "              identifier: approval\n"
        + "              type: HarnessApproval\n"
        + "              timeout: 1d\n"
        + "              spec:\n"
        + "                approvalMessage: Please review\n"
        + "                includePipelineExecutionHistory: true\n"
        + "                approvers:\n"
        + "                  minimumCount: 1\n"
        + "                  disallowPipelineExecutor: false\n"
        + "                  userGroups:\n"
        + "                  - account.Dashboards\n";
  }

  private String getPipelineYamlWithRuntime() {
    return "pipeline:\n"
        + "  name: has runtime\n"
        + "  identifier: has_runtime\n"
        + "  projectIdentifier: namantest\n"
        + "  orgIdentifier: default\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      name: a1\n"
        + "      identifier: a1\n"
        + "      type: Approval\n"
        + "      spec:\n"
        + "        execution:\n"
        + "          steps:\n"
        + "          - step:\n"
        + "              name: Approval\n"
        + "              identifier: approval\n"
        + "              type: HarnessApproval\n"
        + "              timeout: 1d\n"
        + "              spec:\n"
        + "                approvalMessage: <+input>\n"
        + "                includePipelineExecutionHistory: true\n"
        + "                approvers:\n"
        + "                  minimumCount: 1\n"
        + "                  disallowPipelineExecutor: false\n"
        + "                  userGroups:\n"
        + "                  - account.Dashboards\n";
  }

  private String getRuntimeTemplate() {
    return "pipeline:\n"
        + "  identifier: \"has_runtime\"\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      identifier: \"a1\"\n"
        + "      type: \"Approval\"\n"
        + "      spec:\n"
        + "        execution:\n"
        + "          steps:\n"
        + "          - step:\n"
        + "              identifier: \"approval\"\n"
        + "              type: \"HarnessApproval\"\n"
        + "              spec:\n"
        + "                approvalMessage: \"<+input>\"\n";
  }
}
