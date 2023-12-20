/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.migration;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.RuleExecution;
import io.harness.ccm.views.entities.RuleExecution.RuleExecutionKeys;
import io.harness.migration.NGMigration;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class GovernanceRuleExecutionAddTtlMigration implements NGMigration {
  @Inject private HPersistence hPersistence;
  @Override
  public void migrate() {
    try {
      log.info(
          "Starting GovernanceRuleExecutionAddTtlMigration migration of all Rule Execution, adding default ttl of 90 days");
      final List<RuleExecution> ruleExecutionList =
          hPersistence.createQuery(RuleExecution.class, excludeAuthority).asList();
      for (final RuleExecution ruleExecution : ruleExecutionList) {
        // For every rule execution where ttl is null, we are defaulting it to 90 days
        try {
          migrateTtlForRuleExecutions(ruleExecution.getAccountId(), ruleExecution.getUuid());
        } catch (final Exception e) {
          log.error("Migration Failed for Account {}, ruleExecutionId {}, {}", ruleExecution.getAccountId(),
              ruleExecution.getUuid(), e);
        }
      }
      log.info("GovernanceRuleExecutionAddTtlMigration has been completed");
    } catch (final Exception e) {
      log.error("Failure occurred in GovernanceRuleExecutionAddTtlMigration", e);
    }
  }

  private void migrateTtlForRuleExecutions(final String accountId, final String ruleExecutionUuid) {
    Query query = hPersistence.createQuery(RuleExecution.class)
                      .field(RuleExecutionKeys.accountId)
                      .equal(accountId)
                      .field(RuleExecutionKeys.uuid)
                      .equal(ruleExecutionUuid);
    UpdateOperations<RuleExecution> updateOperations = hPersistence.createUpdateOperations(RuleExecution.class);

    updateOperations.set(RuleExecutionKeys.ttl, Instant.now().plus(Duration.ofDays(90)));
    hPersistence.update(query, updateOperations);
  }
}
