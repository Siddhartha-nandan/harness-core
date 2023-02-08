/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import static io.harness.cdng.ssh.SshWinRmConstants.FILE_STORE_SCRIPT_ERROR_MSG;
import static io.harness.rule.OwnerRule.IVAN;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileReference;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.ssh.CommandStepInfo;
import io.harness.cdng.ssh.CommandStepNode;
import io.harness.cdng.ssh.CommandUnitSpecType;
import io.harness.cdng.ssh.CommandUnitWrapper;
import io.harness.cdng.ssh.ScriptCommandUnitSpec;
import io.harness.cdng.ssh.SshWinRmConfigFileHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.SetupMetadata;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.rule.Owner;
import io.harness.steps.shellscript.HarnessFileStoreSource;
import io.harness.steps.shellscript.ShellScriptBaseSource;
import io.harness.steps.shellscript.ShellScriptSourceWrapper;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
public class CDPMSCommandStepFilterJsonCreatorTest extends CDNGTestBase {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_ID = "orgId";
  private static final String PROJECT_ID = "projectId";

  @Mock private SshWinRmConfigFileHelper sshWinRmConfigFileHelper;

  @InjectMocks private CDPMSCommandStepFilterJsonCreator cdpmsCommandStepFilterJsonCreator;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHandleNodeWithEmptyScriptFromHarnessFileStore() {
    FilterCreationContext context =
        FilterCreationContext.builder().currentField(new YamlField("Command", new YamlNode(null))).build();
    context.setSetupMetadata(
        SetupMetadata.newBuilder().setAccountId(ACCOUNT_ID).setOrgId(ORG_ID).setProjectId(PROJECT_ID).build());
    String scopedFilePath = "account:/folder1/folder2/emptyScript";
    ShellScriptSourceWrapper shellScriptSourceWrapper =
        ShellScriptSourceWrapper.builder()
            .type(ShellScriptBaseSource.HARNESS)
            .spec(HarnessFileStoreSource.builder().file(ParameterField.createValueField(scopedFilePath)).build())
            .build();
    CommandUnitWrapper commandUnitWrapper =
        CommandUnitWrapper.builder()
            .type(CommandUnitSpecType.SCRIPT)
            .spec(ScriptCommandUnitSpec.builder().source(shellScriptSourceWrapper).build())
            .build();

    CommandStepNode commandStepNode = new CommandStepNode();
    commandStepNode.setCommandStepInfo(
        CommandStepInfo.infoBuilder().commandUnits(Collections.singletonList(commandUnitWrapper)).build());

    when(sshWinRmConfigFileHelper.fetchFileContent(FileReference.of(scopedFilePath, ACCOUNT_ID, ORG_ID, PROJECT_ID)))
        .thenReturn("");

    assertThatThrownBy(() -> cdpmsCommandStepFilterJsonCreator.handleNode(context, commandStepNode))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(format(FILE_STORE_SCRIPT_ERROR_MSG, scopedFilePath));
  }
}
