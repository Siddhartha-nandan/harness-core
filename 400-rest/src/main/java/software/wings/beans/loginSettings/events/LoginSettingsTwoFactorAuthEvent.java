/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.loginSettings.events;

import static software.wings.beans.loginSettings.LoginSettingsConstants.TWO_FACTOR_AUTH_UPDATED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.PL)
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginSettingsTwoFactorAuthEvent extends LoginSettingsAbstractEvent {
  private TwoFactorAuthYamlDTO oldTwoFactorAuthYamlDTO;
  private TwoFactorAuthYamlDTO newTwoFactorAuthYamlDTO;

  @Override
  public String getEventType() {
    return TWO_FACTOR_AUTH_UPDATED;
  }
}
