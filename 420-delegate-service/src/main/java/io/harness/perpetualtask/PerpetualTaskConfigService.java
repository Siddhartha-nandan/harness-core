package io.harness.perpetualtask;

import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig;

public interface PerpetualTaskConfigService {
    PerpetualTaskConfig disableAccountPerpetualTask(String accountId, String perpetualTaskType);
    PerpetualTaskConfig resumeAccountPerpetualTask(String accountId, String perpetualTaskType);
}
