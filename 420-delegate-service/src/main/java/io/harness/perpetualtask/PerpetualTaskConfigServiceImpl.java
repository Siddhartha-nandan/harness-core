package io.harness.perpetualtask;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskConfig;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;
import io.harness.persistence.HPersistence;

public class PerpetualTaskConfigServiceImpl implements PerpetualTaskConfigService{

    @Inject
    private HPersistence persistence;
    @Inject private PerpetualTaskService perpetualTaskService;


    @Override
    public PerpetualTaskConfig disableAccountPerpetualTask(String accountId, String perpetualTaskType) {

        //UpdateOperations updateOperations = persistence.createUpdateOperations(PerpetualTaskConfig.class);

     /*   updateOperations.set()
        persistence.update(persistence.createQuery(PerpetualTaskConfig.class)
                .field(PerpetualTaskConfigKeys.accountId)
                .equal(accountId)
                .field(PerpetualTaskConfigKeys.perpetualTaskType)
                .equal(perpetualTaskType)),
                updateOperations);

        Query<PerpetualTaskConfig> query =  ;


        persistence.update(query, updateOperations);*/
        perpetualTaskService.updateTasksState(accountId, perpetualTaskType, PerpetualTaskState.TASK_PAUSED);
        return null;
    }

    @Override
    public PerpetualTaskConfig resumeAccountPerpetualTask(String accountId, String perpetualTaskType) {
        return null;
    }
}
