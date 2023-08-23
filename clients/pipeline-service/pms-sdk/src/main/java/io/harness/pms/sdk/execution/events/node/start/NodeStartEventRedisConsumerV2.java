/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.SDK_NODE_START_CONSUMER;

import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;

public class NodeStartEventRedisConsumerV2 extends PmsAbstractRedisConsumer<NodeStartEventMessageListener> {
  @Inject
  public NodeStartEventRedisConsumerV2(@Named(SDK_NODE_START_CONSUMER) Consumer redisConsumer,
      NodeStartEventMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache,
      QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
