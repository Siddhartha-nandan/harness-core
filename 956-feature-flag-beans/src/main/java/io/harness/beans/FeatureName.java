/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag.Scope;

import lombok.Getter;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
@OwnedBy(HarnessTeam.PL)
public enum FeatureName {
  NG_GITOPS,
  APPD_CV_TASK,
  ARGO_PHASE1,
  ARGO_PHASE2_MANAGED,
  ARTIFACT_PERPETUAL_TASK,
  ARTIFACT_PERPETUAL_TASK_MIGRATION,
  ARTIFACT_STREAM_REFACTOR,
  ARTIFACT_STREAM_DELEGATE_SCOPING,
  ARTIFACT_STREAM_DELEGATE_TIMEOUT,
  AUDIT_TRAIL_WEB_INTERFACE,
  AUTO_ACCEPT_SAML_ACCOUNT_INVITES,
  AZURE_US_GOV_CLOUD,
  AZURE_VMSS,
  AZURE_WEBAPP,
  AZURE_ARM,
  AUDIT_TRAIL_ENHANCEMENT,
  BIND_FETCH_FILES_TASK_TO_DELEGATE,
  BUSINESS_MAPPING,
  CCM_ENABLE_STATIC_SCHEDULES,
  CCM_SUSTAINABILITY,
  CCM_GCP_TLS_ENABLED,
  CDNG_ENABLED,
  CENG_ENABLED,
  CE_AS_KUBERNETES_ENABLED,
  CE_ANOMALY_DETECTION,
  CE_AS_GCP_VM_SUPPORT,
  CE_INVENTORY_DASHBOARD,
  CE_BILLING_DATA_PRE_AGGREGATION,
  CE_BILLING_DATA_HOURLY_PRE_AGGREGATION,
  CE_SAMPLE_DATA_GENERATION,
  CE_AZURE_SUPPORT,
  CE_HARNESS_ENTITY_MAPPING,
  CE_HARNESS_INSTANCE_QUERY,
  CFNG_ENABLED,
  CF_CUSTOM_EXTRACTION,
  CF_ROLLBACK_CONFIG_FILTER,
  CG_RBAC_EXCLUSION,
  CING_ENABLED,
  CI_INDIRECT_LOG_UPLOAD,
  CLOUD_FORMATION_CREATE_REFACTOR,
  CUSTOM_APM_24_X_7_CV_TASK,
  CUSTOM_APM_CV_TASK,
  CUSTOM_DASHBOARD,
  CUSTOM_DEPLOYMENT,
  NG_DEPLOYMENT_TEMPLATE,
  CUSTOM_MAX_PAGE_SIZE,
  CUSTOM_RESOURCEGROUP_SCOPE,
  CUSTOM_SECRETS_MANAGER,
  CVNG_ENABLED,
  CV_DEMO,
  CV_FEEDBACKS,
  CV_HOST_SAMPLING,
  CV_SUCCEED_FOR_ANOMALY,
  DEFAULT_ARTIFACT,
  DEPLOY_TO_SPECIFIC_HOSTS,
  ENABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC,
  DISABLE_LOGML_NEURAL_NET,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  DISABLE_SERVICEGUARD_LOG_ALERTS,
  DISABLE_WINRM_COMMAND_ENCODING,
  ECS_RECOMMENDATION,
  ENABLE_WINRM_ENV_VARIABLES,
  FF_PIPELINE,
  FF_GITSYNC,
  FFM_1513,
  FFM_1512,
  FFM_1827,
  FFM_1859,
  FFM_2134_FF_PIPELINES_TRIGGER,
  WINRM_COPY_CONFIG_OPTIMIZE,
  ECS_MULTI_LBS,
  ENTITY_AUDIT_RECORD,
  EXPORT_TF_PLAN,
  GCB_CI_SYSTEM,
  GCP_WORKLOAD_IDENTITY,
  GIT_ACCOUNT_SUPPORT,
  GIT_HTTPS_KERBEROS,
  GIT_HOST_CONNECTIVITY,
  GLOBAL_COMMAND_LIBRARY,
  GLOBAL_CV_DASH,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GRAPHQL_DEV,
  HARNESS_TAGS,
  HELM_CHART_AS_ARTIFACT,
  HELM_STEADY_STATE_CHECK_1_16,
  HELM_CHART_NAME_SPLIT,
  HELM_MERGE_CAPABILITIES,
  INLINE_SSH_COMMAND,
  IGNORE_PCF_CONNECTION_CONTEXT_CACHE,
  LIMIT_PCF_THREADS,
  OPA_FF_GOVERNANCE,
  OPA_GIT_GOVERNANCE,
  OPA_PIPELINE_GOVERNANCE,
  OPA_CONNECTOR_GOVERNANCE,
  OPA_SECRET_GOVERNANCE,
  PCF_OLD_APP_RESIZE,
  LOCAL_DELEGATE_CONFIG_OVERRIDE,
  LOGS_V2_247,
  MOVE_AWS_AMI_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_AMI_SPOT_INST_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_CODE_DEPLOY_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_LAMBDA_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_SSH_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_CONTAINER_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_PCF_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  PDC_PERPETUAL_TASK,
  NEW_INSTANCE_TIMESERIES,
  NEW_RELIC_CV_TASK,
  NEWRELIC_24_7_CV_TASK,
  NG_DASHBOARDS,
  CI_TI_DASHBOARDS_ENABLED,
  NODE_RECOMMENDATION_1,
  NODE_RECOMMENDATION_AGGREGATE,
  ON_NEW_ARTIFACT_TRIGGER_WITH_LAST_COLLECTED_FILTER,
  OUTAGE_CV_DISABLE,
  OVERRIDE_VALUES_YAML_FROM_HELM_CHART,
  PIPELINE_GOVERNANCE,
  PRUNE_KUBERNETES_RESOURCES,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  ROLLBACK_NONE_ARTIFACT,
  SCIM_INTEGRATION,
  SEARCH(Scope.GLOBAL),
  SEARCH_REQUEST,
  SEND_LOG_ANALYSIS_COMPRESSED,
  SEND_SLACK_NOTIFICATION_FROM_DELEGATE,
  SIDE_NAVIGATION,
  SKIP_SWITCH_ACCOUNT_REAUTHENTICATION,
  SLACK_APPROVALS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_SPOT_INST_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_CODE_DEPLOY_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_LAMBDA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_SSH_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PDC_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AZURE_INFRA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PCF_DEPLOYMENTS,
  SUPERVISED_TS_THRESHOLD,
  TEMPLATIZED_SECRET_MANAGER,
  THREE_PHASE_SECRET_DECRYPTION,
  TIME_RANGE_FREEZE_GOVERNANCE,
  TRIGGER_FOR_ALL_ARTIFACTS,
  TRIGGER_YAML,
  UI_ALLOW_K8S_V1,
  USE_NEXUS3_PRIVATE_APIS,
  WEEKLY_WINDOW,
  ENABLE_CVNG_INTEGRATION,
  DYNATRACE_MULTI_SERVICE,
  REFACTOR_STATEMACHINEXECUTOR,
  WORKFLOW_DATA_COLLECTION_ITERATOR,
  ENABLE_CERT_VALIDATION,
  RESOURCE_CONSTRAINT_MAX_QUEUE,
  RESOURCE_CONSTRAINT_SCOPE_PIPELINE_ENABLED,
  HIDE_SCOPE_COMMAND_OPTION,
  AWS_OVERRIDE_REGION,
  SHOW_TASK_SETUP_ABSTRACTIONS,
  CLEAN_UP_OLD_MANAGER_VERSIONS(Scope.PER_ACCOUNT),
  ECS_AUTOSCALAR_REDESIGN,
  SAVE_SHELL_SCRIPT_PROVISION_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_APPLY_SWEEPING_OUTPUT_TO_WORKFLOW,
  TRIGGER_PROFILE_SCRIPT_EXECUTION_WF,
  NEW_DEPLOYMENT_FREEZE,
  ECS_REGISTER_TASK_DEFINITION_TAGS,
  CUSTOM_DASHBOARD_INSTANCE_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_DEPLOYMENT_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_INSTANCE_AGGREGATION,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_DEPLOYMENT_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_INSTANCE_DATA_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_DEPLOYMENT_DATA_MIGRATION,
  SSH_SECRET_ENGINE,
  WHITELIST_PUBLIC_API,
  WHITELIST_GRAPHQL,
  TIMEOUT_FAILURE_SUPPORT,
  LOG_APP_DEFAULTS,
  ENABLE_LOGIN_AUDITS,
  CUSTOM_MANIFEST,
  WEBHOOK_TRIGGER_AUTHORIZATION,
  NG_HELM_SOURCE_REPO,
  ENHANCED_GCR_CONNECTIVITY_CHECK,
  USE_TF_CLIENT,
  SERVICE_DASHBOARD_NG,
  GITHUB_WEBHOOK_AUTHENTICATION,
  NG_SIGNUP(Scope.GLOBAL),
  NG_LICENSES_ENABLED,
  ECS_BG_DOWNSIZE,
  LIMITED_ACCESS_FOR_HARNESS_USER_GROUP,
  REMOVE_STENCIL_MANUAL_INTERVENTION,
  CI_OVERVIEW_PAGE,
  SKIP_BASED_ON_STACK_STATUSES,
  WF_VAR_MULTI_SELECT_ALLOWED_VALUES,
  LDAP_GROUP_SYNC_JOB_ITERATOR,
  PIPELINE_MONITORING,
  CF_CLI7,
  CF_APP_NON_VERSIONING_INACTIVE_ROLLBACK,
  CF_ALLOW_SPECIAL_CHARACTERS,
  HTTP_HEADERS_CAPABILITY_CHECK,
  AMI_IN_SERVICE_HEALTHY_WAIT,
  SETTINGS_OPTIMIZATION,
  CG_SECRET_MANAGER_DELEGATE_SELECTORS,
  ARTIFACT_COLLECTION_CONFIGURABLE,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  PLANS_ENABLED,
  FEATURE_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENABLED,
  VIEW_USAGE_ENABLED,
  SOCKET_HTTP_STATE_TIMEOUT,
  TERRAFORM_CONFIG_INSPECT_VERSION_SELECTOR,
  VALIDATE_PROVISIONER_EXPRESSION,
  WORKFLOW_PIPELINE_PERMISSION_BY_ENTITY,
  AMAZON_ECR_AUTH_REFACTOR,
  AMI_ASG_CONFIG_COPY,
  OPTIMIZED_GIT_FETCH_FILES,
  CVNG_VERIFY_STEP_DEMO,
  CVNG_MONITORED_SERVICE_DEMO,
  CVNG_VERIFY_STEP_LOGS_UI_V2,
  MANIFEST_INHERIT_FROM_CANARY_TO_PRIMARY_PHASE,
  USE_LATEST_CHARTMUSEUM_VERSION,
  KUBERNETES_EXPORT_MANIFESTS,
  NG_TEMPLATES,
  NEW_KUSTOMIZE_BINARY,
  KUSTOMIZE_PATCHES_CG,
  CVNG_VERIFY_STEP_TO_SINGLE_ACTIVITY,
  SSH_JSCH_LOGS,
  RESOLVE_DEPLOYMENT_TAGS_BEFORE_EXECUTION,
  LDAP_USER_ID_SYNC,
  NEW_KUBECTL_VERSION,
  CUSTOM_DASHBOARD_V2, // To be used only by ui to control flow from cg dashbaords to ng
  TIME_SCALE_CG_SYNC,
  DELEGATE_SELECTION_LOGS_DISABLED,
  CI_INCREASE_DEFAULT_RESOURCES,
  DISABLE_DEPLOYMENTS_SEARCH_AND_LIMIT_DEPLOYMENT_STATS,
  RATE_LIMITED_TOTP,
  USE_HELM_REPO_FLAGS,
  CLOSE_TIME_SCALE_SYNC_PROCESSING_ON_FAILURE(Scope.GLOBAL),
  RESOURCE_CENTER_ENABLED,
  USE_IMMUTABLE_DELEGATE,
  ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS,
  TERRAFORM_AWS_CP_AUTHENTICATION,
  CI_VM_INFRASTRUCTURE,
  SERVICENOW_NG_INTEGRATION,
  OPTIMIZED_TF_PLAN,
  SELF_SERVICE_ENABLED,
  NG_NATIVE_HELM,
  CHI_CUSTOM_HEALTH,
  CHI_CUSTOM_HEALTH_LOGS,
  AZURE_SAML_150_GROUPS_SUPPORT,
  CLOUDFORMATION_SKIP_WAIT_FOR_RESOURCES,
  CLOUDFORMATION_CHANGE_SET,
  FAIL_WORKFLOW_IF_SECRET_DECRYPTION_FAILS,
  ERROR_TRACKING_ENABLED,
  DEPLOY_TO_INLINE_HOSTS,
  HONOR_DELEGATE_SCOPING,
  CG_LICENSE_USAGE,
  RANCHER_SUPPORT,
  BYPASS_HELM_FETCH,
  FREEZE_DURING_MIGRATION,
  USE_ANALYTIC_MONGO_FOR_GRAPHQL_QUERY,
  DYNATRACE_APM_ENABLED,
  CUSTOM_POLICY_STEP,
  KEEP_PT_AFTER_K8S_DOWNSCALE,
  CCM_ANOMALY_DETECTION_NG,
  CCM_AS_DRY_RUN,
  DONT_RESTRICT_PARALLEL_STAGE_COUNT,
  NG_EXECUTION_INPUT,
  HELM_CHART_VERSION_STRICT_MATCH,
  SKIP_ADDING_TRACK_LABEL_SELECTOR_IN_ROLLING,
  EXTERNAL_USERID_BASED_LOGIN,
  LDAP_SYNC_WITH_USERID,
  DISABLE_HARNESS_SM,
  SECURITY,
  SECURITY_STAGE,
  STO_CI_PIPELINE_SECURITY,
  STO_CD_PIPELINE_SECURITY,
  GIT_SYNC_WITH_BITBUCKET,
  REFACTOR_ARTIFACT_SELECTION,
  CCM_DEV_TEST,
  CV_FAIL_ON_EMPTY_NODES,
  SHOW_REFINER_FEEDBACK,
  SHOW_NG_REFINER_FEEDBACK,
  NG_NEXUS_ARTIFACTORY,
  HELM_VERSION_3_8_0,
  DELETE_HELM_REPO_CACHE_DIR,
  DELEGATE_ENABLE_DYNAMIC_HANDLING_OF_REQUEST,
  YAML_GIT_CONNECTOR_NAME,
  STOP_SHOWING_RUNNING_EXECUTIONS,
  SSH_NG,
  ARTIFACT_STREAM_METADATA_ONLY,
  SERVICENOW_CREATE_UPDATE_NG,
  OUTCOME_GRAPHQL_WITH_INFRA_DEF,
  AUTO_REJECT_PREVIOUS_APPROVALS,
  ENABLE_K8S_AUTH_IN_VAULT,
  BIND_CUSTOM_VALUE_AND_MANIFEST_FETCH_TASK,
  AZURE_BLOB_SM,
  CONSIDER_ORIGINAL_STATE_VERSION,
  SINGLE_MANIFEST_SUPPORT,
  GIT_SYNC_PROJECT_CLEANUP,
  ENV_GROUP,
  POLLING_INTERVAL_CONFIGURABLE,
  REDUCE_DELEGATE_MEMORY_SIZE,
  NG_VARIABLES,
  PIPELINE_PER_ENV_DEPLOYMENT_PERMISSION,
  DISABLE_LOCAL_LOGIN,
  WINRM_KERBEROS_CACHE_UNIQUE_FILE,
  HIDE_ABORT,
  CUSTOM_ARTIFACT_NG,
  NG_TEMPLATE_REFERENCES_SUPPORT,
  APPLICATION_DROPDOWN_MULTISELECT,
  NG_AZURE,
  NG_GIT_EXPERIENCE,
  CLOUDFORMATION_NG,
  CIE_HOSTED_BUILDS,
  LDAP_SECRET_AUTH,
  WORKFLOW_EXECUTION_REFRESH_STATUS,
  SERVERLESS_SUPPORT,
  TF_MODULE_SOURCE_INHERIT_SSH,
  TRIGGERS_PAGE_PAGINATION,
  CVNG_NOTIFICATION_UI,
  STALE_FLAGS_FFM_1510,
  NG_SVC_ENV_REDESIGN,
  NEW_PIPELINE_STUDIO,
  AZURE_REPO_CONNECTOR,
  HELM_OCI_SUPPORT,
  HELP_PANEL,
  CHAOS_ENABLED,
  DEPLOYMENT_SUBFORMIK_APPLICATION_DROPDOWN,
  USAGE_SCOPE_RBAC,
  ALLOW_USER_TYPE_FIELDS_JIRA,
  HARD_DELETE_ENTITIES,
  PIPELINE_MATRIX,
  ACTIVITY_ID_BASED_TF_BASE_DIR,
  INHERITED_USER_GROUP,
  JDK11_UPGRADE_BANNER,
  DISABLE_CI_STAGE_DEL_SELECTOR,
  CLEANUP_INCOMPLETE_CANARY_DEPLOY_RELEASE,
  JENKINS_ARTIFACT,
  NG_CUSTOM_STAGE,
  ENABLE_DEFAULT_TIMEFRAME_IN_DEPLOYMENTS,
  CUSTOM_SECRET_MANAGER_NG,
  EXPORT_TF_PLAN_JSON_NG,
  JDK11_WATCHER,
  NG_FILE_STORE,
  ACCOUNT_BASIC_ROLE,
  CVNG_TEMPLATE_MONITORED_SERVICE,
  CVNG_TEMPLATE_VERIFY_STEP,
  WORKFLOW_EXECUTION_ZOMBIE_MONITOR,
  USE_PAGINATED_ENCRYPT_SERVICE, // To be only used by UI for safeguarding encrypt component changes in CG
  INFRA_MAPPING_BASED_ROLLBACK_ARTIFACT,
  DEPLOYMENT_SUBFORMIK_PIPELINE_DROPDOWN,
  DEPLOYMENT_SUBFORMIK_WORKFLOW_DROPDOWN,
  TI_DOTNET,
  TG_USE_AUTO_APPROVE_FLAG,
  CVNG_SPLUNK_METRICS,
  AUTO_FREE_MODULE_LICENSE,
  SRM_LICENSE_ENABLED,
  AZURE_WEBAPP_NG,
  ACCOUNT_BASIC_ROLE_ONLY,
  CCM_MICRO_FRONTEND,
  GITOPS_BYO_ARGO,
  NG_GIT_EXPERIENCE_IMPORT_FLOW,
  YAML_APIS_GRANULAR_PERMISSION;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
