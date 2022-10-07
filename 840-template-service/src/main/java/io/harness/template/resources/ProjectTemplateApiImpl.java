/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.template.ProjectTemplateApi;
import io.harness.spec.server.template.model.GitCreateDetails;
import io.harness.spec.server.template.model.GitFindDetails;
import io.harness.spec.server.template.model.GitUpdateDetails;
import io.harness.spec.server.template.model.TemplateCreateRequestBody;
import io.harness.spec.server.template.model.TemplateFilterProperties;
import io.harness.spec.server.template.model.TemplateUpdateRequestBody;

import com.google.inject.Inject;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@NextGenManagerAuth
public class ProjectTemplateApiImpl implements ProjectTemplateApi {
  private final TemplateResourceApiUtils templateResourceApiUtils;
  @Override
  public Response createTemplatesProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      TemplateCreateRequestBody templateCreateRequestBody, @AccountIdentifier String account, Boolean isStable,
      String comments) {
    GitCreateDetails gitCreateDetails = templateCreateRequestBody.getGitDetails();
    String templateYaml = templateCreateRequestBody.getTemplateYaml();
    return templateResourceApiUtils.createTemplate(
        account, org, project, gitCreateDetails, templateYaml, isStable, comments);
  }

  @Override
  public Response deleteTemplateProject(@ProjectIdentifier String project,
      @ResourceIdentifier String templateIdentifier, @OrgIdentifier String org, String versionLabel,
      @AccountIdentifier String account, String comments) {
    return templateResourceApiUtils.deleteTemplate(account, org, project, templateIdentifier, versionLabel, comments);
  }

  @Override
  public Response getTemplateProject(@ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier,
      @OrgIdentifier String org, String versionLabel, GitFindDetails gitFindDetails, @AccountIdentifier String account,
      Boolean getInputYaml) {
    return templateResourceApiUtils.getTemplate(
        account, org, project, templateIdentifier, versionLabel, false, gitFindDetails, getInputYaml);
  }

  @Override
  public Response getTemplateStableProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String templateIdentifier, GitFindDetails gitFindDetails, @AccountIdentifier String account,
      Boolean getInputYaml) {
    return templateResourceApiUtils.getTemplate(
        account, org, project, templateIdentifier, null, false, gitFindDetails, getInputYaml);
  }

  @Override
  public Response getTemplatesListProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      TemplateFilterProperties templateFilterProperties, @AccountIdentifier String account, Integer page, Integer limit,
      String sort, String order, String searchTerm, String listType, Boolean recursive) {
    return templateResourceApiUtils.getTemplates(
        account, org, project, page, limit, sort, order, searchTerm, listType, recursive, templateFilterProperties);
  }

  @Override
  public Response updateTemplateProject(@ProjectIdentifier String project,
      @ResourceIdentifier String templateIdentifier, @OrgIdentifier String org, String versionLabel,
      TemplateUpdateRequestBody templateUpdateRequestBody, @AccountIdentifier String account, Boolean isStable,
      String comments) {
    GitUpdateDetails gitUpdateDetails = templateUpdateRequestBody.getGitDetails();
    String templateYaml = templateUpdateRequestBody.getTemplateYaml();
    return templateResourceApiUtils.updateTemplate(
        account, org, project, templateIdentifier, versionLabel, gitUpdateDetails, templateYaml, isStable, comments);
  }

  @Override
  public Response updateTemplateStableProject(@OrgIdentifier String org, @ProjectIdentifier String project,
      @ResourceIdentifier String templateIdentifier, String versionLabel, GitFindDetails gitFindDetails,
      @AccountIdentifier String account, String comments) {
    return templateResourceApiUtils.updateStableTemplate(
        account, org, project, templateIdentifier, versionLabel, gitFindDetails, comments);
  }
}
