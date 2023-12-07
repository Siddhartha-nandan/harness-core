/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.steps;

import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;

public interface Constants {
  String IDP_COOKIECUTTER = "IdpCookieCutter";
  String IDP_CREATE_REPO = "IdpCreateRepo";
  String IDP_CODE_PUSH = "IdpCodePush";
  String IDP_COOKIECUTTER_STEP_NODE = "IdpCookieCutterStepNode";
  String IDP_CREATE_REPO_STEP_NODE = "IdpCreateRepoStepNode";
  String REGISTER_CATALOG_STEP_NODE = "RegisterCatalogStepNode";
  String IDP_CODE_PUSH_STEP_NODE = "IdpCodePushStepNode";

  String REGISTER_CATALOG = "RegisterCatalog";
  StepType IDP_COOKIECUTTER_STEP_TYPE =
      StepType.newBuilder().setType(IDP_COOKIECUTTER).setStepCategory(StepCategory.STEP).build();
  StepType IDP_CREATE_REPO_STEP_TYPE =
      StepType.newBuilder().setType(IDP_CREATE_REPO).setStepCategory(StepCategory.STEP).build();
  StepType IDP_CODE_PUSH_STEP_TYPE =
      StepType.newBuilder().setType(IDP_CODE_PUSH).setStepCategory(StepCategory.STEP).build();

  StepType REGISTER_CATALOG_STEP_TYPE =
      StepType.newBuilder().setType(REGISTER_CATALOG).setStepCategory(StepCategory.STEP).build();
}
