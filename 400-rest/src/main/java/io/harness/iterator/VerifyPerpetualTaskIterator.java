package io.harness.iterator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.delegate.utils.DelegateTaskMigrationHelper;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceRequiredProvider;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.perpetualtask.internal.PerpetualTaskRecordHandler;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import io.harness.workers.background.CrossEnvironmentAccountStatusBasedEntityProcessController;

import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.time.Duration;

public class VerifyPerpetualTaskIterator
    extends IteratorPumpAndRedisModeHandler implements MongoPersistenceIterator.Handler<PerpetualTaskRecord> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private DelegateService delegateService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private MorphiaPersistenceRequiredProvider<PerpetualTaskRecord> persistenceProvider;
  @Inject private AccountService accountService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateTaskMigrationHelper delegateTaskMigrationHelper;

  private static final Duration ACCEPTABLE_NO_ALERT_DELAY = ofSeconds(45);
  private static final Duration ACCEPTABLE_EXECUTION_TIME = ofSeconds(30);

  @Override
  protected void registerIterator(IteratorExecutionHandler iteratorExecutionHandler) {
    iterator =
        (MongoPersistenceIterator<PerpetualTaskRecord, MorphiaFilterExpander<PerpetualTaskRecord>>)
            persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(executorOptions,
                PerpetualTaskRecordHandler.class,
                MongoPersistenceIterator.<PerpetualTaskRecord, MorphiaFilterExpander<PerpetualTaskRecord>>builder()
                    .clazz(PerpetualTaskRecord.class)
                    .fieldName(PerpetualTaskRecord.PerpetualTaskRecordKeys.verifyIteration)
                    .targetInterval(targetInterval)
                    .acceptableNoAlertDelay(ACCEPTABLE_NO_ALERT_DELAY)
                    .acceptableExecutionTime(ACCEPTABLE_EXECUTION_TIME)
                    .handler(this::handle)
                    .filterExpander(query
                        -> query.filter(
                            PerpetualTaskRecord.PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_ASSIGNED))
                    .schedulingType(REGULAR)
                    .persistenceProvider(persistenceProvider)
                    .redistribute(true));
  }

  @Override
  protected void createAndStartIterator(
      PersistenceIteratorFactory.PumpExecutorOptions executorOptions, Duration targetInterval) {
        iterator =
                (MongoPersistenceIterator<PerpetualTaskRecord, MorphiaFilterExpander<PerpetualTaskRecord>>)
                        persistenceIteratorFactory.createRedisBatchIteratorWithDedicatedThreadPool(executorOptions,
                                PerpetualTaskRecordHandler.class,
                                MongoPersistenceIterator.<PerpetualTaskRecord, MorphiaFilterExpander<PerpetualTaskRecord>>builder()
                                        .clazz(PerpetualTaskRecord.class)
                                        .fieldName(PerpetualTaskRecord.PerpetualTaskRecordKeys.verifyIteration)
                                        .targetInterval(targetInterval)
                                        .acceptableNoAlertDelay(ACCEPTABLE_NO_ALERT_DELAY)
                                        .acceptableExecutionTime(ACCEPTABLE_EXECUTION_TIME)
                                        .handler(this::handle)
                                        .filterExpander(query
                                                -> query.filter(PerpetualTaskRecord.PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED)))
                                        .persistenceProvider(persistenceProvider));
  }

  @Override
  public void handle(PerpetualTaskRecord perpetualTaskRecord) {
    if (isNotEmpty(perpetualTaskRecord.getDelegateId())
      && !delegateService.checkDelegateConnected(
          perpetualTaskRecord.getAccountId(), perpetualTaskRecord.getDelegateId())) {

    }
  }
}
