/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig.PerpetualTaskConfigKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;

public class PerpetualTaskConfigServiceImpl implements PerpetualTaskConfigService {
  @Inject private HPersistence persistence;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public PerpetualTaskConfig disableAccountPerpetualTask(String accountId, String perpetualTaskType) {
    UpdateOperations<PerpetualTaskConfig> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskConfig.class)
            .set(PerpetualTaskConfigKeys.accountId, accountId)
            .set(PerpetualTaskConfigKeys.perpetualTaskType, perpetualTaskType);
    PerpetualTaskConfig perpetualTaskConfig = persistence.upsert(persistence.createQuery(PerpetualTaskConfig.class)
                                                                     .field(PerpetualTaskConfigKeys.accountId)
                                                                     .equal(accountId)
                                                                     .field(PerpetualTaskConfigKeys.perpetualTaskType)
                                                                     .equal(perpetualTaskType),
        updateOperations, upsertReturnNewOptions);
    perpetualTaskService.updateTasksState(accountId, perpetualTaskType, PerpetualTaskState.TASK_PAUSED);
    return perpetualTaskConfig;
  }

  @Override
  public Boolean resumeAccountPerpetualTask(String accountId, String perpetualTaskType) {
    Query<PerpetualTaskConfig> deleteQuery = persistence.createQuery(PerpetualTaskConfig.class, excludeAuthority)
                                                 .filter(PerpetualTaskConfigKeys.accountId, accountId)
                                                 .filter(PerpetualTaskConfigKeys.perpetualTaskType, perpetualTaskType);

    return persistence.delete(deleteQuery);
  }
}
