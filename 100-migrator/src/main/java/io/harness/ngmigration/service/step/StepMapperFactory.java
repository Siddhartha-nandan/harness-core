/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step;

import io.harness.ngmigration.service.step.k8s.K8sApplyStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sBlueGreenDeployStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sCanaryDeployStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sDeleteStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sRollingRollbackStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sRollingStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sScaleStepMapperImpl;
import io.harness.ngmigration.service.step.k8s.K8sSwapServiceSelectorsStepMapperImpl;
import io.harness.ngmigration.service.step.terraform.TerraformApplyStepMapperImpl;
import io.harness.ngmigration.service.step.terraform.TerraformDestroyStepMapperImpl;
import io.harness.ngmigration.service.step.terraform.TerraformProvisionStepMapperImpl;
import io.harness.ngmigration.service.step.terraform.TerraformRollbackStepMapperImpl;

import software.wings.beans.GraphNode;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepMapperFactory {
  @Inject ShellScriptStepMapperImpl shellScriptStepMapper;
  @Inject K8sRollingStepMapperImpl k8sRollingStepMapper;
  @Inject HttpStepMapperImpl httpStepMapper;
  @Inject ApprovalStepMapperImpl approvalStepMapper;
  @Inject BarrierStepMapperImpl barrierStepMapper;
  @Inject K8sApplyStepMapperImpl k8sApplyStepMapper;
  @Inject K8sDeleteStepMapperImpl k8sDeleteStepMapper;
  @Inject EmailStepMapperImpl emailStepMapper;
  @Inject K8sRollingRollbackStepMapperImpl k8sRollingRollbackStepMapper;
  @Inject K8sCanaryDeployStepMapperImpl k8sCanaryDeployStepMapper;
  @Inject EmptyStepMapperImpl emptyStepMapper;
  @Inject K8sScaleStepMapperImpl k8sScaleStepMapper;
  @Inject JenkinsStepMapperImpl jenkinsStepMapper;
  @Inject K8sSwapServiceSelectorsStepMapperImpl k8sSwapServiceSelectorsStepMapper;
  @Inject K8sBlueGreenDeployStepMapperImpl k8sBlueGreenDeployStepMapper;
  @Inject JiraCreateUpdateStepMapperImpl jiraCreateUpdateStepMapper;
  @Inject CommandStepMapperImpl commandStepMapper;
  @Inject TerraformApplyStepMapperImpl terraformApplyStepMapper;
  @Inject TerraformProvisionStepMapperImpl terraformProvisionStepMapper;
  @Inject TerraformDestroyStepMapperImpl terraformDestroyStepMapper;
  @Inject TerraformRollbackStepMapperImpl terraformRollbackStepMapper;

  @Inject UnsupportedStepMapperImpl unsupportedStepMapper;

  public StepMapper getStepMapper(String stepType) {
    switch (stepType) {
      case "SHELL_SCRIPT":
        return shellScriptStepMapper;
      case "K8S_DEPLOYMENT_ROLLING":
        return k8sRollingStepMapper;
      case "HTTP":
        return httpStepMapper;
      case "APPROVAL":
        return approvalStepMapper;
      case "BARRIER":
        return barrierStepMapper;
      case "K8S_DELETE":
        return k8sDeleteStepMapper;
      case "K8S_APPLY":
        return k8sApplyStepMapper;
      case "K8S_SCALE":
        return k8sScaleStepMapper;
      case "EMAIL":
        return emailStepMapper;
      case "K8S_DEPLOYMENT_ROLLING_ROLLBACK":
        return k8sRollingRollbackStepMapper;
      case "K8S_CANARY_DEPLOY":
        return k8sCanaryDeployStepMapper;
      case "JENKINS":
        return jenkinsStepMapper;
      case "KUBERNETES_SWAP_SERVICE_SELECTORS":
        return k8sSwapServiceSelectorsStepMapper;
      case "K8S_BLUE_GREEN_DEPLOY":
        return k8sBlueGreenDeployStepMapper;
      case "JIRA_CREATE_UPDATE":
        return jiraCreateUpdateStepMapper;
      case "COMMAND":
        return commandStepMapper;
      case "TERRAFORM_PROVISION":
        return terraformProvisionStepMapper;
      case "TERRAFORM_APPLY":
        return terraformApplyStepMapper;
      case "TERRAFORM_DESTROY":
        return terraformDestroyStepMapper;
      case "TERRAFORM_ROLLBACK":
        return terraformRollbackStepMapper;
      case "ROLLING_NODE_SELECT":
      case "AWS_NODE_SELECT":
      case "AZURE_NODE_SELECT":
      case "DC_NODE_SELECT":
      case "ARTIFACT_COLLECTION":
      case "ARTIFACT_CHECK":
        return emptyStepMapper;
      default:
        return unsupportedStepMapper;
    }
  }

  public boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2) {
    if (!stepYaml1.getType().equals(stepYaml2.getType())) {
      return false;
    }
    try {
      return getStepMapper(stepYaml1.getType()).areSimilar(stepYaml1, stepYaml2);
    } catch (Exception e) {
      log.error("There was an error with finding similar steps", e);
      return false;
    }
  }
}
