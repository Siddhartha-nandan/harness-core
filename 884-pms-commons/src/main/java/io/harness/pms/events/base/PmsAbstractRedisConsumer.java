/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.events.base;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.impl.redis.RedisAbstractConsumer;
import io.harness.eventsframework.impl.redis.RedisTraceConsumer;
import io.harness.queue.QueueController;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public abstract class PmsAbstractRedisConsumer<T extends PmsAbstractMessageListener>
    extends RedisTraceConsumer implements PmsRedisConsumer {
  private static final int WAIT_TIME_IN_SECONDS = 1;
  private static final int THREAD_SLEEP_TIME_IN_MILLIS = 200;
  private static final int THREAD_SLEEP_MILLIS_WHEN_CONSUMER_IS_BUSY =
      EmptyPredicate.isNotEmpty(System.getenv("THREAD_SLEEP_MILLIS_WHEN_CONSUMER_IS_BUSY"))
      ? Integer.parseInt(System.getenv("THREAD_SLEEP_MILLIS_WHEN_CONSUMER_IS_BUSY"))
      : 0;
  private static final int SLEEP_SECONDS = 10;
  private static final String CACHE_KEY = "%s_%s";
  private final Consumer redisConsumer;
  private final T messageListener;
  private final QueueController queueController;
  private AtomicBoolean shouldStop = new AtomicBoolean(false);
  private Cache<String, Integer> eventsCache;

  public PmsAbstractRedisConsumer(
      Consumer redisConsumer, T messageListener, Cache<String, Integer> eventsCache, QueueController queueController) {
    this.redisConsumer = redisConsumer;
    this.messageListener = messageListener;
    this.eventsCache = eventsCache;
    this.queueController = queueController;
  }

  @Override
  public void run() {
    log.info("Started the Consumer {}", this.getClass().getSimpleName());
    String threadName = this.getClass().getSimpleName() + "-handler-" + generateUuid();
    log.debug("Setting thread name to {}", threadName);
    Thread.currentThread().setName(threadName);

    try {
      preThreadHandler();
      do {
        while (getMaintenanceFlag()) {
          log.info("We are under maintenance, will try again after {} seconds", SLEEP_SECONDS);
          sleep(ofSeconds(SLEEP_SECONDS));
        }
        if (queueController.isNotPrimary()) {
          log.info(this.getClass().getSimpleName()
              + " is not running on primary deployment, will try again after some time...");
          TimeUnit.SECONDS.sleep(30);
          continue;
        }
        readEventsFrameworkMessages();
      } while (!Thread.currentThread().isInterrupted() && !shouldStop.get());
    } catch (Exception ex) {
      log.error("Consumer {} unexpectedly stopped", this.getClass().getSimpleName(), ex);
    } finally {
      postThreadCompletion();
    }
  }

  public void preThreadHandler() {}

  public void postThreadCompletion() {}

  protected void readEventsFrameworkMessages() throws InterruptedException {
    try {
      pollAndProcessMessages();
      // Adding thread sleep to allow queue clients to not overload connections and over submit events.
      // Removing the sleep from here. Because the sleep is added in the pollAndProcessMessages method.
      //      TimeUnit.MILLISECONDS.sleep(THREAD_SLEEP_TIME_IN_MILLIS);
    } catch (EventsFrameworkDownException e) {
      log.error("Events framework is down for " + this.getClass().getSimpleName() + " consumer. Retrying again...", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  @VisibleForTesting
  void pollAndProcessMessages() throws InterruptedException {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = redisConsumer.read(Duration.ofSeconds(WAIT_TIME_IN_SECONDS));
    for (Message message : messages) {
      messageId = message.getId();
      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        redisConsumer.acknowledge(messageId);
        log.info("[Brijesh-scale-test] Acknowledged the message for message-id: " + message.getId());
      }
    }
    // Only checking the size for SDK_RESPONSE_EVENT_BATCH_SIZE now for testing. Will take the correct batch-size for
    // the various events.
    if (messages.size() < ((RedisAbstractConsumer) this.redisConsumer).getBatchSize()) {
      // Adding thread sleep when the events read are less than the batch-size. This way when the load is high, consumer
      // will query the events quickly. And in case of low load, thread will sleep for sometime.
      TimeUnit.MILLISECONDS.sleep(THREAD_SLEEP_TIME_IN_MILLIS);
    } else if (THREAD_SLEEP_MILLIS_WHEN_CONSUMER_IS_BUSY > 0) {
      TimeUnit.MILLISECONDS.sleep(THREAD_SLEEP_MILLIS_WHEN_CONSUMER_IS_BUSY);
    }
  }

  @Override
  protected boolean processMessage(Message message) {
    AtomicBoolean success = new AtomicBoolean(true);
    log.info("[Brijesh-scale-test] Checking if message is processable or its already processed for message-id: "
        + message.getId());
    if (messageListener.isProcessable(message) && !isAlreadyProcessed(message)) {
      log.info("[Brijesh-scale-test] Checked if the message is already processed for message-id: " + message.getId());
      log.info("Read message with message id {} from redis", message.getId());
      insertMessageInCache(message);
      log.info("[Brijesh-scale-test] Inserted message into the cache for message-id: " + message.getId());
      if (!messageListener.handleMessage(message)) {
        success.set(false);
      }
      log.info("[Brijesh-scale-test] Handled the message for message-id: " + message.getId());
    }
    return success.get();
  }

  private void insertMessageInCache(Message message) {
    try {
      eventsCache.put(String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId()), 1);
    } catch (Exception ex) {
      log.error("Exception occurred while storing message id in cache", ex);
    }
  }

  private boolean isAlreadyProcessed(Message message) {
    try {
      String key = String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId());
      boolean isProcessed = eventsCache.containsKey(key);
      if (isProcessed) {
        log.warn(String.format("Duplicate redis notification received to consumer [%s] with messageId [%s]",
            this.getClass().getSimpleName(), message.getId()));
        Integer count = eventsCache.get(key);
        if (count != null) {
          eventsCache.put(String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId()), count + 1);
        }
      }
      return isProcessed;
    } catch (Exception ex) {
      log.error("Exception occurred while checking for duplicate notification", ex);
      return false;
    }
  }

  public void shutDown() {
    shouldStop.set(true);
  }
}
