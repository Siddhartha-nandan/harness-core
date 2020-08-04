package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceRequiredProvider;
import io.harness.timeout.TimeoutInstance.TimeoutInstanceKeys;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TimeoutEngine implements Handler<TimeoutInstance> {
  private static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  @Inject private TimeoutInstanceRepository timeoutInstanceRepository;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private SpringPersistenceRequiredProvider<TimeoutInstance> persistenceProvider;
  @Inject private Injector injector;

  private PersistenceIterator<TimeoutInstance> iterator;

  public TimeoutInstance registerTimeout(
      @NotNull TimeoutTracker timeoutTracker, @NotNull TimeoutCallback timeoutCallback) {
    Long expiryTime = timeoutTracker.getExpiryTime();
    TimeoutInstance timeoutInstance = TimeoutInstance.builder()
                                          .uuid(generateUuid())
                                          .tracker(timeoutTracker)
                                          .callback(timeoutCallback)
                                          .nextIteration(expiryTime == null ? Long.MAX_VALUE : expiryTime)
                                          .build();
    TimeoutInstance savedTimeoutInstance = timeoutInstanceRepository.save(timeoutInstance);
    if (iterator != null) {
      iterator.wakeup();
    }
    return savedTimeoutInstance;
  }

  public void registerIterators() {
    PersistenceIteratorFactory.PumpExecutorOptions options = PersistenceIteratorFactory.PumpExecutorOptions.builder()
                                                                 .interval(Duration.ofSeconds(10))
                                                                 .poolSize(5)
                                                                 .name("TimeoutEngineHandler")
                                                                 .build();
    iterator = persistenceIteratorFactory.createLoopIteratorWithDedicatedThreadPool(options, TimeoutInstance.class,
        MongoPersistenceIterator.<TimeoutInstance, SpringFilterExpander>builder()
            .clazz(TimeoutInstance.class)
            .fieldName(TimeoutInstanceKeys.nextIteration)
            .targetInterval(Duration.ofMinutes(2))
            .acceptableNoAlertDelay(Duration.ofSeconds(45))
            .acceptableExecutionTime(Duration.ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(TimeoutInstance timeoutInstance) {
    try (TimeoutInstanceLogContext ignore0 = new TimeoutInstanceLogContext(timeoutInstance.getUuid(), OVERRIDE_ERROR)) {
      final long now = System.currentTimeMillis();
      logger.info("TimeoutInstance handle started");

      TimeoutCallback callback = timeoutInstance.getCallback();
      injector.injectMembers(callback);
      try {
        callback.onTimeout(timeoutInstance);
        logger.info("TimeoutInstance callback finished");
      } catch (Exception ex) {
        // TODO(gpahal): What to do in case callback throws an exception. Should we retry?
        logger.error("TimeoutInstance callback failed", ex);
      }

      try {
        timeoutInstanceRepository.delete(timeoutInstance);
      } catch (Exception ex) {
        logger.error("TimeoutInstance delete failed", ex);
      }

      final long passed = System.currentTimeMillis() - now;
      if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
        logger.error(
            "TimeoutInstanceHandler: It took more than {} ms before we processed the callback. THIS IS VERY BAD!!!",
            MAX_CALLBACK_PROCESSING_TIME.toMillis());
      }
    }
  }
}
