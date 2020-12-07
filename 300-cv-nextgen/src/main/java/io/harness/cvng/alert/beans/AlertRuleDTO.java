package io.harness.cvng.alert.beans;

import io.harness.cvng.alert.util.ActivityType;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.ng.core.dto.NotificationSettingType;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AlertRuleDTOKeys")
public class AlertRuleDTO {
  String uuid;
  boolean enabled;
  String name;
  AlertCondition alertCondition;
  NotificationMethod notificationMethod;

  String identifier;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "AlertConditionKeys")
  public static class AlertCondition {
    List<String> services;
    List<String> environments;

    boolean enabledVerifications;
    VerificationsNotify verificationsNotify;

    boolean enabledRisk;
    RiskNotify notify;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "VerificationsNotifyKeys")
  public static class VerificationsNotify {
    List<ActivityType> activityTypes;
    List<VerificationStatus> verificationStatuses;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "RiskNotifyKeys")
  public static class RiskNotify {
    int threshold;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "NotificationMethodKeys")
  public static class NotificationMethod {
    NotificationSettingType notificationSettingType;

    String slackWebhook;
    String slackChannelName;

    String pagerDutyKey;

    List<String> emails;
  }
}
