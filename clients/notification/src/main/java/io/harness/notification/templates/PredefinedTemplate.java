/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.templates;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(PL)
public enum PredefinedTemplate {
  EMAIL_TEST("templates/email_test.txt", "email_test"),
  SLACK_TEST("templates/slack_test.txt", "slack_test"),
  WEBHOOK_TEST("templates/webhook_test.txt", "webhook_test"),
  SLACK_VANILLA("templates/slack_vanilla.txt", "slack_vanilla"),
  PD_TEST("templates/pd_test.txt", "pd_test"),
  PD_VANILLA("templates/pd_vanilla.txt", "pd_vanilla"),
  MSTEAMS_TEST("templates/msteams_test.txt", "msteams_test"),
  EMAIL_INVITE("templates/email_invite.txt", "email_invite"),
  EMAIL_NOTIFY("templates/email_notify.txt", "email_notify"),
  NG_RESET_PASSWORD("templates/ng_reset_password.txt", "ng_reset_password"),
  NG_RESET_2FA("templates/reset_2fa.txt", "reset_2fa"),
  EMAIL_VERIFY("templates/email_verify.txt", "default_email_verify"),
  SIGNUP_CONFIRMATION("templates/signup_confirmation.txt", "default_signup_confirmation"),
  EMAIL_TEST_WITH_USER("templates/email_test2.txt", "email_test2"),
  SLACK_TEST_WITH_USER("templates/slack_test2.txt", "slack_test2"),
  PD_TEST_WITH_USER("templates/pd_test2.txt", "pd_test2"),
  MSTEAMS_TEST_WITH_USER("templates/msteams_test2.txt", "msteams_test2"),
  PIPELINE_PLAIN_SLACK("notification_templates/pipeline/slack/plain_text.txt", "pms_pipeline_slack_plain"),
  PIPELINE_PLAIN_WEBHOOK("notification_templates/pipeline/webhook/plain_text.txt", "pms_pipeline_webhook_plain"),
  PIPELINE_PLAIN_EMAIL("notification_templates/pipeline/email/plain_text.txt", "pms_pipeline_email_plain"),
  PIPELINE_PLAIN_PAGERDUTY("notification_templates/pipeline/pagerduty/plain_text.txt", "pms_pipeline_pagerduty_plain"),
  PIPELINE_PLAIN_MSTEAMS("notification_templates/pipeline/msteams/plain_text.txt", "pms_pipeline_msteams_plain"),
  STAGE_PLAIN_SLACK("notification_templates/stage/slack/plain_text.txt", "pms_stage_slack_plain"),
  STAGE_PLAIN_WEBHOOK("notification_templates/stage/webhook/plain_text.txt", "pms_stage_webhook_plain"),
  STAGE_PLAIN_EMAIL("notification_templates/stage/email/plain_text.txt", "pms_stage_email_plain"),
  STAGE_PLAIN_PAGERDUTY("notification_templates/stage/pagerduty/plain_text.txt", "pms_stage_pagerduty_plain"),
  STAGE_PLAIN_MSTEAMS("notification_templates/stage/msteams/plain_text.txt", "pms_stage_msteams_plain"),
  STEP_PLAIN_SLACK("notification_templates/step/slack/plain_text.txt", "pms_step_slack_plain"),
  STEP_PLAIN_WEBHOOK("notification_templates/step/webhook/plain_text.txt", "pms_step_webhook_plain"),
  STEP_PLAIN_EMAIL("notification_templates/step/email/plain_text.txt", "pms_step_email_plain"),
  STEP_PLAIN_PAGERDUTY("notification_templates/step/pagerduty/plain_text.txt", "pms_step_pagerduty_plain"),
  STEP_PLAIN_MSTEAMS("notification_templates/step/msteams/plain_text.txt", "pms_step_msteams_plain"),
  HARNESS_APPROVAL_NOTIFICATION_SLACK("notification_templates/approval/slack/plain_text.txt", "harness_approval_slack"),
  HARNESS_APPROVAL_NOTIFICATION_EMAIL("notification_templates/approval/email/plain_text.txt", "harness_approval_email"),
  HARNESS_APPROVAL_EXECUTION_NOTIFICATION_SLACK(
      "notification_templates/approval/slack/plain_text_execution.txt", "harness_approval_execution_slack"),
  HARNESS_APPROVAL_EXECUTION_NOTIFICATION_EMAIL(
      "notification_templates/approval/email/plain_text_execution.txt", "harness_approval_execution_email"),
  CVNG_SLO_SIMPLE_PROJECT_SLACK(
      "cvng_notification_templates/cvng_slo_simple_project_slack.txt", "cvng_slo_simple_project_slack"),
  CVNG_SLO_SIMPLE_PROJECT_EMAIL(
      "cvng_notification_templates/cvng_slo_simple_project_email.txt", "cvng_slo_simple_project_email"),
  CVNG_SLO_SIMPLE_PROJECT_PAGERDUTY(
      "cvng_notification_templates/cvng_slo_simple_project_pagerduty.txt", "cvng_slo_simple_project_pagerduty"),
  CVNG_SLO_SIMPLE_PROJECT_MSTEAMS(
      "cvng_notification_templates/cvng_slo_simple_project_msteams.txt", "cvng_slo_simple_project_msteams"),

  CVNG_SLO_COMPOSITE_PROJECT_SLACK(
      "cvng_notification_templates/cvng_slo_composite_project_slack.txt", "cvng_slo_composite_project_slack"),
  CVNG_SLO_COMPOSITE_PROJECT_EMAIL(
      "cvng_notification_templates/cvng_slo_composite_project_email.txt", "cvng_slo_composite_project_email"),
  CVNG_SLO_COMPOSITE_PROJECT_PAGERDUTY(
      "cvng_notification_templates/cvng_slo_composite_project_pagerduty.txt", "cvng_slo_composite_project_pagerduty"),
  CVNG_SLO_COMPOSITE_PROJECT_MSTEAMS(
      "cvng_notification_templates/cvng_slo_composite_project_msteams.txt", "cvng_slo_composite_project_msteams"),
  CVNG_SLO_COMPOSITE_ACCOUNT_SLACK(
      "cvng_notification_templates/cvng_slo_composite_account_slack.txt", "cvng_slo_composite_account_slack"),
  CVNG_SLO_COMPOSITE_ACCOUNT_EMAIL(
      "cvng_notification_templates/cvng_slo_composite_account_email.txt", "cvng_slo_composite_account_email"),
  CVNG_SLO_COMPOSITE_ACCOUNT_PAGERDUTY(
      "cvng_notification_templates/cvng_slo_composite_account_pagerduty.txt", "cvng_slo_composite_account_pagerduty"),
  CVNG_SLO_COMPOSITE_ACCOUNT_MSTEAMS(
      "cvng_notification_templates/cvng_slo_composite_account_msteams.txt", "cvng_slo_composite_account_msteams"),
  CVNG_FIREHYDRANT_SLACK("cvng_notification_templates/cvng_firehydrant_slack.txt", "cvng_firehydrant_slack"),
  CVNG_FIREHYDRANT_WEBHOOK("cvng_notification_templates/cvng_firehydrant_webhook.txt", "cvng_firehydrant_webhook"),
  CVNG_MONITOREDSERVICE_SLACK(
      "cvng_notification_templates/cvng_monitoredservice_slack.txt", "cvng_monitoredservice_slack"),
  CVNG_MONITOREDSERVICE_ET_SLACK(
      "cvng_notification_templates/cvng_monitoredservice_et_slack.txt", "cvng_monitoredservice_et_slack"),

  CVNG_MONITOREDSERVICE_REPORT_SLACK(
      "cvng_notification_templates/cvng_monitoredservice_report_slack.txt", "cvng_monitoredservice_report_slack"),
  CVNG_MONITOREDSERVICE_REPORT_EMAIL(
      "cvng_notification_templates/cvng_monitoredservice_report_email.txt", "cvng_monitoredservice_report_email"),
  CVNG_MONITOREDSERVICE_REPORT_PAGERDUTY("cvng_notification_templates/cvng_monitoredservice_report_pagerduty.txt",
      "cvng_monitoredservice_report_pagerduty"),

  CVNG_MONITOREDSERVICE_REPORT_MSTEAMS(
      "cvng_notification_templates/cvng_monitoredservice_report_msteams.txt", "cvng_monitoredservice_report_msteams"),
  CVNG_MONITOREDSERVICE_EMAIL(
      "cvng_notification_templates/cvng_monitoredservice_email.txt", "cvng_monitoredservice_email"),
  CVNG_MONITOREDSERVICE_ET_EMAIL(
      "cvng_notification_templates/cvng_monitoredservice_et_email.txt", "cvng_monitoredservice_et_email"),
  CVNG_MONITOREDSERVICE_PAGERDUTY(
      "cvng_notification_templates/cvng_monitoredservice_pagerduty.txt", "cvng_monitoredservice_pagerduty"),
  CVNG_MONITOREDSERVICE_MSTEAMS(
      "cvng_notification_templates/cvng_monitoredservice_msteams.txt", "cvng_monitoredservice_msteams"),
  EMAIL_CCM_ANOMALY_ALERT("templates/email_ccm_anomaly_alert.txt", "email_ccm_anomaly_alert"),
  SLACK_CCM_ANOMALY_ALERT("templates/slack_ccm_anomaly_alert.txt", "slack_ccm_anomaly_alert"),
  EMAIL_CCM_BUDGET_ALERT("templates/email_ccm_budget_alert.txt", "email_ccm_budget_alert"),
  SLACK_CCM_BUDGET_ALERT("templates/slack_ccm_budget_alert.txt", "slack_ccm_budget_alert"),
  EMAIL_CCM_CLOUD_BILLING_DATA_READY("templates/email_ccm_cloud_data_ready.txt", "email_ccm_cloud_data_ready"),
  FREEZE_EMAIL_ALERT("notification_templates/email_test.txt", "freeze_email_alert"),
  FREEZE_SLACK_ALERT("notification_templates/slack_test.txt", "freeze_slack_alert"),
  FREEZE_PD_ALERT("notification_templates/pd_test.txt", "freeze_pagerduty_alert"),
  FREEZE_MSTEAMS_ALERT("notification_templates/msteams_test.txt", "freeze_msteams_alert"),
  FREEZE_ENABLED_EMAIL_ALERT("notification_templates/freeze_enabled_email_test.txt", "freeze_enabled_email_alert"),
  FREEZE_ENABLED_SLACK_ALERT("notification_templates/freeze_enabled_slack_test.txt", "freeze_enabled_slack_alert"),
  FREEZE_ENABLED_PD_ALERT("notification_templates/freeze_enabled_pd_test.txt", "freeze_enabled_pagerduty_alert"),
  FREEZE_ENABLED_MSTEAMS_ALERT(
      "notification_templates/freeze_enabled_msteams_test.txt", "freeze_enabled_msteams_alert"),
  EMAIL_SMP_LICENSE_ALERT("templates/email_smp_license_alert.txt", "email_smp_license_alert"),
  PIPELINE_REJECTED_EMAIL_ALERT(
      "notification_templates/pipeline_rejected_email_test.txt", "pipeline_rejected_email_alert"),
  PIPELINE_REJECTED_SLACK_ALERT(
      "notification_templates/pipeline_rejected_slack_test.txt", "pipeline_rejected_slack_alert"),
  PIPELINE_REJECTED_PD_ALERT(
      "notification_templates/pipeline_rejected_pd_test.txt", "pipeline_rejected_pagerduty_alert"),
  PIPELINE_REJECTED_MSTEAMS_ALERT(
      "notification_templates/pipeline_rejected_msteams_test.txt", "pipeline_rejected_msteams_alert"),
  HARNESS_APPROVAL_NOTIFICATION_MSTEAMS(
      "notification_templates/approval/msteams/plain_text.txt", "harness_approval_msteams"),
  HARNESS_APPROVAL_EXECUTION_NOTIFICATION_MSTEAMS(
      "notification_templates/approval/msteams/plain_text_execution.txt", "harness_approval_execution_msteams"),
  HARNESS_APPROVAL_ACTION_NOTIFICATION_SLACK(
      "notification_templates/approval/slack/plain_text_action.txt", "harness_approval_action_slack"),
  HARNESS_APPROVAL_ACTION_NOTIFICATION_EMAIL(
      "notification_templates/approval/email/plain_text_action.txt", "harness_approval_action_email"),
  HARNESS_APPROVAL_ACTION_NOTIFICATION_MSTEAMS(
      "notification_templates/approval/msteams/plain_text_action.txt", "harness_approval_action_msteams"),
  HARNESS_APPROVAL_ACTION_EXECUTION_NOTIFICATION_SLACK(
      "notification_templates/approval/slack/plain_text_action_execution.txt",
      "harness_approval_action_execution_slack"),
  HARNESS_APPROVAL_ACTION_EXECUTION_NOTIFICATION_EMAIL(
      "notification_templates/approval/email/plain_text_action_execution.txt",
      "harness_approval_action_execution_email"),
  HARNESS_APPROVAL_ACTION_EXECUTION_NOTIFICATION_MSTEAMS(
      "notification_templates/approval/msteams/plain_text_action_execution.txt",
      "harness_approval_action_execution_msteams"),
  SLACK_CCM_BUDGET_GROUP_ALERT("templates/slack_ccm_budget_group_alert.txt", "slack_ccm_budget_group_alert"),
  IDP_PLUGIN_REQUESTS_NOTIFICATION_SLACK(
      "notification_templates/slack/plugin_requests.txt", "idp_plugin_requests_notification_slack"),
  DELEGATE_DOWN_EMAIL("notificationtemplates/email/delegate_disconnected.txt", "delegate_disconnected_email"),
  DELEGATE_EXPIRED_EMAIL("notificationtemplates/email/delegate_expired.txt", "delegate_expired_email"),
  DELEGATE_ABOUT_EXPIRE_EMAIL(
      "notificationtemplates/email/delegate_about_to_expire.txt", "delegate_about_to_expire_email"),

  DELEGATE_DOWN_SLACK("notificationtemplates/slack/delegate_disconnected.txt", "delegate_disconnected_slack"),
  DELEGATE_EXPIRED_SLACK("notificationtemplates/slack/delegate_expired.txt", "delegate_expired_slack"),
  DELEGATE_ABOUT_EXPIRE_SLACK(
      "notificationtemplates/slack/delegate_about_to_expire.txt", "delegate_about_to_expire_slack"),

  DELEGATE_DOWN_PAGERDUTY(
      "notificationtemplates/pagerduty/delegate_disconnected.txt", "delegate_disconnected_pagerduty"),
  DELEGATE_EXPIRED_PAGERDUTY("notificationtemplates/pagerduty/delegate_expired.txt", "delegate_expired_pagerduty"),
  DELEGATE_ABOUT_EXPIRE_PAGERDUTY(
      "notificationtemplates/pagerduty/delegate_about_to_expire.txt", "delegate_about_to_expire_pagerduty"),

  DELEGATE_DOWN_MSTEAMS("notificationtemplates/msteams/delegate_disconnected.txt", "delegate_disconnected_msteams"),
  DELEGATE_EXPIRED_MSTEAMS("notificationtemplates/msteams/delegate_expired.txt", "delegate_expired_msteams"),
  DELEGATE_ABOUT_EXPIRE_MSTEAMS(
      "notificationtemplates/msteams/delegate_about_to_expire.txt", "delegate_about_to_expire_msteams"),

  DELEGATE_DOWN_WEBHOOK("notificationtemplates/webhook/delegate_disconnected.txt", "delegate_disconnected_webhook"),
  DELEGATE_EXPIRED_WEBHOOK("notificationtemplates/webhook/delegate_expired.txt", "delegate_expired_webhook"),
  DELEGATE_ABOUT_EXPIRE_WEBHOOK(
      "notificationtemplates/webhook/delegate_about_to_expire.txt", "delegate_about_to_expire_webhook"),
  //  Chaos experiment mail templates
  CHAOS_EXPERIMENT_STARTED_EMAIL(
      "notification_templates/chaos/email/chaos_experiment_started.txt", "chaos_experiment_started_email"),
  CHAOS_EXPERIMENT_COMPLETED_EMAIL(
      "notification_templates/chaos/email/chaos_experiment_completed.txt", "chaos_experiment_completed_email"),
  CHAOS_EXPERIMENT_STOPPED_EMAIL(
      "notification_templates/chaos/email/chaos_experiment_stopped.txt", "chaos_experiment_stopped_email"),
  //  Chaos experiment msteams templates
  CHAOS_EXPERIMENT_STARTED_MSTEAMS(
      "notification_templates/chaos/msteams/chaos_experiment_started.txt", "chaos_experiment_started_msteams"),
  CHAOS_EXPERIMENT_COMPLETED_MSTEAMS(
      "notification_templates/chaos/msteams/chaos_experiment_completed.txt", "chaos_experiment_completed_msteams"),
  CHAOS_EXPERIMENT_STOPPED_MSTEAMS(
      "notification_templates/chaos/msteams/chaos_experiment_stopped.txt", "chaos_experiment_stopped_msteams"),
  //  Chaos experiment pagerduty templates
  CHAOS_EXPERIMENT_STARTED_PAGERDUTY(
      "notification_templates/chaos/pagerduty/chaos_experiment_started.txt", "chaos_experiment_started_pagerduty"),
  CHAOS_EXPERIMENT_COMPLETED_PAGERDUTY(
      "notification_templates/chaos/pagerduty/chaos_experiment_completed.txt", "chaos_experiment_completed_pagerduty"),
  CHAOS_EXPERIMENT_STOPPED_PAGERDUTY(
      "notification_templates/chaos/pagerduty/chaos_experiment_stopped.txt", "chaos_experiment_stopped_pagerduty"),
  //  Chaos experiment slack templates
  CHAOS_EXPERIMENT_STARTED_SLACK(
      "notification_templates/chaos/slack/chaos_experiment_started.txt", "chaos_experiment_started_slack"),
  CHAOS_EXPERIMENT_COMPLETED_SLACK(
      "notification_templates/chaos/slack/chaos_experiment_completed.txt", "chaos_experiment_completed_slack"),
  CHAOS_EXPERIMENT_STOPPED_SLACK(
      "notification_templates/chaos/slack/chaos_experiment_stopped.txt", "chaos_experiment_stopped_slack"),
  //  Chaos experiment webhook templates
  CHAOS_EXPERIMENT_STARTED_WEBHOOK(
      "notification_templates/chaos/webhook/chaos_experiment_started.txt", "chaos_experiment_started_webhook"),
  CHAOS_EXPERIMENT_COMPLETED_WEBHOOK(
      "notification_templates/chaos/webhook/chaos_experiment_completed.txt", "chaos_experiment_completed_webhook"),
  CHAOS_EXPERIMENT_STOPPED_WEBHOOK(
      "notification_templates/chaos/webhook/chaos_experiment_stopped.txt", "chaos_experiment_stopped_webhook");

  private String path;
  private String identifier;

  PredefinedTemplate(String path, String identifier) {
    this.path = path;
    this.identifier = identifier;
  }

  public String getPath() {
    return path;
  }

  public String getIdentifier() {
    return identifier;
  }
}
