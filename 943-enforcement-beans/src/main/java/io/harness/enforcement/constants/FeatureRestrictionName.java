/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.constants;

import lombok.Getter;

/**
 * Please register all feature names here.
 */
public enum FeatureRestrictionName {
  // Test purpose
  TEST1("TEST1"),
  TEST2("TEST2"),
  TEST3("TEST3"),
  TEST4("TEST4"),
  TEST5("TEST5"),
  TEST6("TEST6"),
  TEST7("TEST7"),

  // CCM Features
  PERSPECTIVES("PERSPECTIVES"),
  CCM_K8S_CLUSTERS("CCM_K8S_CLUSTERS"),
  CCM_AUTOSTOPPING_RULES("CCM_AUTOSTOPPING_RULES"),

  // All Features
  MULTIPLE_ORGANIZATIONS("MULTIPLE_ORGANIZATIONS"),
  MULTIPLE_PROJECTS("MULTIPLE_PROJECTS"),
  MULTIPLE_SECRETS("MULTIPLE_SECRETS"),
  MULTIPLE_USER_GROUPS("MULTIPLE_USER_GROUPS"),
  MULTIPLE_USERS("MULTIPLE_USERS"),
  MULTIPLE_SERVICE_ACCOUNTS("MULTIPLE_SERVICE_ACCOUNTS"),
  MULTIPLE_VARIABLES("MULTIPLE_VARIABLES"),
  MULTIPLE_CONNECTORS("MULTIPLE_CONNECTORS"),
  MULTIPLE_API_KEYS("MULTIPLE_API_KEYS"),
  MULTIPLE_API_TOKENS("MULTIPLE_API_TOKENS"),
  INTEGRATED_APPROVALS_WITH_HARNESS_UI("INTEGRATED_APPROVALS_WITH_HARNESS_UI"),
  INTEGRATED_APPROVALS_WITH_CUSTOM_SCRIPT("INTEGRATED_APPROVALS_WITH_CUSTOM_SCRIPT"),
  INTEGRATED_APPROVALS_WITH_JIRA("INTEGRATED_APPROVALS_WITH_JIRA"),
  SECRET_MANAGERS("SECRET_MANAGERS"),
  DEPLOYMENTS("DEPLOYMENTS"),
  INITIAL_DEPLOYMENTS("INITIAL_DEPLOYMENTS"),
  DEPLOYMENTS_PER_MONTH("DEPLOYMENTS_PER_MONTH"),
  SERVICES("SERVICES"),
  BUILDS("BUILDS"),
  SAML_SUPPORT("SAML_SUPPORT"),
  OAUTH_SUPPORT("OAUTH_SUPPORT"),
  LDAP_SUPPORT("LDAP_SUPPORT"),
  TWO_FACTOR_AUTH_SUPPORT("TWO_FACTOR_AUTH_SUPPORT"),
  CUSTOM_ROLES("CUSTOM_ROLES"),
  CUSTOM_RESOURCE_GROUPS("CUSTOM_RESOURCE_GROUPS"),
  MAX_TOTAL_BUILDS("MAX_TOTAL_BUILDS"),
  MAX_BUILDS_PER_MONTH("MAX_BUILDS_PER_MONTH"),
  MAX_BUILDS_PER_DAY("MAX_BUILDS_PER_DAY"),
  ACTIVE_COMMITTERS("ACTIVE_COMMITTERS"),
  TEST_INTELLIGENCE("TEST_INTELLIGENCE"),
  TEMPLATE_SERVICE("Template Library"),
  CACHE_SIZE_ALLOWANCE("CACHE_SIZE_ALLOWANCE"),

  // CV Features
  SRM_SERVICES("SRM_SERVICES"),
  ANALYZE_DEPLOYMENT_STEP("ANALYZE_DEPLOYMENT_STEP"),

  // CD Step Palette
  K8S_BG_SWAP_SERVICES("K8S_BG_SWAP_SERVICES"),
  K8S_BLUE_GREEN_DEPLOY("K8S_BLUE_GREEN_DEPLOY"),
  K8S_APPLY("K8S_APPLY"),
  K8S_DELETE("K8S_DELETE"),
  K8S_CANARY_DELETE("K8S_CANARY_DELETE"),
  K8S_ROLLING_DEPLOY("K8S_ROLLING_DEPLOY"),
  K8S_CANARY_DEPLOY("K8S_CANARY_DEPLOY"),
  K8S_SCALE("K8S_SCALE"),
  K8S_ROLLING_ROLLBACK("K8S_ROLLING_ROLLBACK"),
  TERRAFORM_APPLY("TERRAFORM_APPLY"),
  TERRAFORM_PLAN("TERRAFORM_PLAN"),
  TERRAFORM_DESTROY("TERRAFORM_DESTROY"),
  TERRAFORM_ROLLBACK("TERRAFORM_ROLLBACK"),
  INTEGRATED_APPROVALS_WITH_SERVICE_NOW("INTEGRATED_APPROVALS_WITH_SERVICE_NOW"),
  CREATE_STACK("CREATE_STACK"),
  DELETE_STACK("DELETE_STACK"),
  ROLLBACK_STACK("ROLLBACK_STACK"),
  COMMAND("COMMAND"),
  AZURE_SLOT_DEPLOYMENT("AZURE_SLOT_DEPLOYMENT"),
  AZURE_TRAFFIC_SHIFT("AZURE_TRAFFIC_SHIFT"),
  AZURE_SWAP_SLOT("AZURE_SWAP_SLOT"),
  AZURE_WEBAPP_ROLLBACK("AZURE_WEBAPP_ROLLBACK"),
  JENKINS_BUILD("JENKINS_BUILD"),
  AZURE_CREATE_ARM_RESOURCE("AZURE_CREATE_ARM_RESOURCE"),
  AZURE_CREATE_BP_RESOURCE("AZURE_CREATE_BP_RESOURCE"),
  AZURE_ROLLBACK_ARM_RESOURCE("AZURE_ROLLBACK_ARM_RESOURCE"),
  SHELL_SCRIPT_PROVISION("SHELL_SCRIPT_PROVISION"),
  K8S_DRY_RUN("K8S_DRY_RUN"),
  TERRAFORM_CLOUD_RUN("TERRAFORM_CLOUD_RUN"),
  TERRAFORM_CLOUD_ROLLBACK("TERRAFORM_CLOUD_ROLLBACK"),
  K8S_BLUE_GREEN_STAGE_SCALE_DOWN("K8S_BLUE_GREEN_STAGE_SCALE_DOWN"),

  // STO Features
  SECURITY("SECURITY"),

  // FF Features
  DEVELOPERS("DEVELOPERS"),
  MONTHLY_ACTIVE_USERS("MONTHLY_ACTIVE_USERS"),
  STRATEGY_MAX_CONCURRENT("STRATEGY_MAX_CONCURRENT"),

  // Pipeline limits
  MAX_PARALLEL_STEP_IN_A_PIPELINE("MAX_PARALLEL_STEP_IN_A_PIPELINE"),
  PIPELINE_EXECUTION_DATA_RETENTION_DAYS("PIPELINE_EXECUTION_DATA_RETENTION_DAYS"),
  MAX_PIPELINE_TIMEOUT_SECONDS("MAX_PIPELINE_TIMEOUT_SECONDS"),
  MAX_STAGE_TIMEOUT_SECONDS("MAX_STAGE_TIMEOUT_SECONDS"),
  MAX_STEP_TIMEOUT_SECONDS("MAX_STEP_TIMEOUT_SECONDS"),
  MAX_CONCURRENT_ACTIVE_PIPELINE_EXECUTIONS("MAX_CONCURRENT_ACTIVE_PIPELINE_EXECUTIONS"),

  // Chaos Features
  MAX_CHAOS_EXPERIMENT_RUNS_PER_MONTH("MAX_CHAOS_EXPERIMENT_RUNS_PER_MONTH"),
  MAX_CHAOS_INFRASTRUCTURES("MAX_CHAOS_INFRASTRUCTURES"),
  TERRAGRUNT_PLAN("TERRAGRUNT_PLAN"),
  TERRAGRUNT_APPLY("TERRAGRUNT_APPLY"),
  TERRAGRUNT_DESTROY("TERRAGRUNT_DESTROY"),
  TERRAGRUNT_ROLLBACK("TERRAGRUNT_ROLLBACK");

  FeatureRestrictionName(String displayName) {
    this.displayName = displayName;
  }

  @Getter private String displayName;
}
