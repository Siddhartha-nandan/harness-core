/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.events.base;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.hsqs.client.HsqsServiceClient;
import io.harness.hsqs.client.model.AckRequest;
import io.harness.hsqs.client.model.DequeueRequest;
import io.harness.hsqs.client.model.DequeueResponse;
import io.harness.hsqs.client.model.UnAckRequest;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.sdk.execution.events.queue.plan.PlanExecutionAbstractQueueMessageListener;
import io.harness.queue.QueueController;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public abstract class PmsAbstractQueueConsumer<T extends PlanExecutionAbstractQueueMessageListener>
    implements Runnable {
  private static final int WAIT_TIME_IN_SECONDS = 10;
  private final String moduleName;
  private final int batchSize;
  private final HsqsServiceClient hsqsServiceClient;
  private final T messageListener;
  private final QueueController queueController;
  private AtomicBoolean shouldStop = new AtomicBoolean(false);

  public PmsAbstractQueueConsumer(String moduleName, int batchSize, HsqsServiceClient hsqsServiceClient,
      T messageListener, QueueController queueController) {
    this.moduleName = moduleName;
    this.batchSize = batchSize;
    this.hsqsServiceClient = hsqsServiceClient;
    this.messageListener = messageListener;

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
          sleep(ofSeconds(1));
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
    } catch (EventsFrameworkDownException e) {
      log.error("Events framework is down for " + this.getClass().getSimpleName() + " consumer. Retrying again...", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  @VisibleForTesting
  void pollAndProcessMessages() {
    try {
      Response<ResponseDTO<DequeueResponse[]>> messages = hsqsServiceClient
                                                              .dequeue(DequeueRequest.builder()
                                                                           .batchSize(batchSize)
                                                                           .consumerName(moduleName)
                                                                           .topic(moduleName)
                                                                           .maxWaitDuration(100)
                                                                           .build(),
                                                                  "sadlskd")
                                                              .execute();
      for (DequeueResponse message : messages.body().getData()) {
        String messageId = message.getItemId();
        boolean messageProcessed = processMessage(message);
        if (messageProcessed) {
          hsqsServiceClient.ack(AckRequest.builder().itemID(messageId).build(), "");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean processMessage(DequeueResponse message) {
    AtomicBoolean success = new AtomicBoolean(true);
    //        if (messageListener.isProcessable(message) && !isAlreadyProcessed(message)) {
    log.debug("Read message with message id {} from hsqs", message.getItemId());
    //            insertMessageInCache(message);
    if (!messageListener.handleMessage(message)) {
      success.set(false);
    } else {
      hsqsServiceClient.ack(
          AckRequest.builder().itemID(message.getItemId()).topic("").subTopic("").build(), "auth_key");
    }
    //        }
    return success.get();
  }

  //    @Override
  //    protected boolean processMessage(Message message) {
  //        AtomicBoolean success = new AtomicBoolean(true);
  //        if (messageListener.isProcessable(message) && !isAlreadyProcessed(message)) {
  //            log.debug("Read message with message id {} from redis", message.getId());
  //            insertMessageInCache(message);
  //            if (!messageListener.handleMessage(message)) {
  //                success.set(false);
  //            }
  //        }
  //        return success.get();
  //    }

  //    private void insertMessageInCache(Message message) {
  //        try {
  //            eventsCache.put(String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId()), 1);
  //        } catch (Exception ex) {
  //            log.error("Exception occurred while storing message id in cache", ex);
  //        }
  //    }
  //
  //    private boolean isAlreadyProcessed(Message message) {
  //        try {
  //            String key = String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId());
  //            boolean isProcessed = eventsCache.containsKey(key);
  //            if (isProcessed) {
  //                log.warn(String.format("Duplicate redis notification received to consumer [%s] with messageId [%s]",
  //                        this.getClass().getSimpleName(), message.getId()));
  //                Integer count = eventsCache.get(key);
  //                if (count != null) {
  //                    eventsCache.put(String.format(CACHE_KEY, this.getClass().getSimpleName(), message.getId()),
  //                    count + 1);
  //                }
  //            }
  //            return isProcessed;
  //        } catch (Exception ex) {
  //            log.error("Exception occurred while checking for duplicate notification", ex);
  //            return false;
  //        }
  //    }

  public void shutDown() {
    shouldStop.set(true);
  }
}
