/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.utils;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleType;
import io.harness.cvng.notification.channelDetails.CVNGNotificationChannelType;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NotificationRuleCommonUtils {
  public static final Duration COOL_OFF_DURATION = Duration.ofHours(1);

  public static long getDurationInMillis(String duration) {
    long lookBackDurationInMillis = 0;
    if (duration != null) {
      Preconditions.checkState(!duration.isEmpty(), "duration can not be empty");
      if (duration.charAt(duration.length() - 1) != 'm') {
        throw new IllegalArgumentException("duration should end with m, ex: 5m, 10m etc.");
      }
      duration = duration.substring(0, duration.length() - 1);
      try {
        long lookBackDurationInLong = Long.parseLong(duration);
        lookBackDurationInMillis = TimeUnit.MINUTES.toMillis(lookBackDurationInLong);
      } catch (NumberFormatException numberFormatException) {
        throw new IllegalArgumentException(
            "can not parse duration please check format for duration., ex: 5m, 10m etc.", numberFormatException);
      }
    }
    return lookBackDurationInMillis;
  }

  public static String getDurationAsString(long duration) {
    return TimeUnit.MILLISECONDS.toMinutes(duration) + "m";
  }

  public static String getNotificationTemplateId(
      NotificationRuleType notificationRuleType, CVNGNotificationChannelType notificationChannelType) {
    return String.format("cvng_%s_%s", notificationRuleType.getTemplateSuffixIdentifier().toLowerCase(),
        notificationChannelType.getTemplateSuffixIdentifier().toLowerCase());
  }

  public static Map<String, String> getNotificationTemplateData(
      ProjectParams projectParams, String identifierName, String identifierValue) {
    return new HashMap<String, String>() {
      {
        put(identifierName, identifierValue);
        put("accountIdentifier", projectParams.getAccountIdentifier());
        put("orgIdentifier", projectParams.getOrgIdentifier());
        put("projectIdentifier", projectParams.getProjectIdentifier());
      }
    };
  }
}
