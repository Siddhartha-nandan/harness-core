/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.audittrails.events;

import io.harness.ccm.views.entities.RuleEnforcement;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor

public class RuleEnforcementUpdateEvent extends RuleEnforcementEvent {
  public static final String RULE_ENFORCEMENT_UPDATED = "RuleEnforcementUpdated";
  private RuleEnforcement oldRuleEnforcement;
  public RuleEnforcementUpdateEvent(
      String accountIdentifier, RuleEnforcement ruleEnforcement, RuleEnforcement oldRuleEnforcement) {
    super(accountIdentifier, ruleEnforcement);
    this.oldRuleEnforcement = oldRuleEnforcement;
  }

  @Override
  public String getEventType() {
    return RULE_ENFORCEMENT_UPDATED;
  }
}
