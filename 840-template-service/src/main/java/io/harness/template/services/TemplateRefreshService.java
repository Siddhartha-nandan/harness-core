/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.template.beans.refresh.ValidateTemplateInputsResponseDTO;
import io.harness.template.beans.refresh.YamlDiffResponseDTO;
import io.harness.template.beans.refresh.YamlFullRefreshResponseDTO;

@OwnedBy(CDC)
public interface TemplateRefreshService {
  void refreshAndUpdateTemplate(
      String accountId, String orgId, String projectId, String templateIdentifier, String versionLabel);

  String refreshLinkedTemplateInputs(String accountId, String orgId, String projectId, String yaml);

  ValidateTemplateInputsResponseDTO validateTemplateInputsInTemplate(
      String accountId, String orgId, String projectId, String templateIdentifier, String versionLabel);

  ValidateTemplateInputsResponseDTO validateTemplateInputsForYaml(
      String accountId, String orgId, String projectId, String yaml);

  YamlDiffResponseDTO getYamlDiffOnRefreshingTemplate(
      String accountId, String orgId, String projectId, String templateIdentifier, String versionLabel);

  void recursivelyRefreshTemplates(
      String accountId, String orgId, String projectId, String templateIdentifier, String versionLabel);

  YamlFullRefreshResponseDTO recursivelyRefreshTemplatesForYaml(
      String accountId, String orgId, String projectId, String yaml);
}
