/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.lock.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;

@OwnedBy(PL)
@Value
@Builder
@Slf4j
public class RedisAcquiredLock implements AcquiredLock<RLock> {
  RLock lock;
  boolean isLeaseInfinite;

  @Override
  public void release() {
    try {
      if (lock != null && (lock.isHeldByCurrentThread() || isLeaseInfinite())) {
        log.info("[RedisAcquiredLock]: Unlocking lock for Thread ID: {}", Thread.currentThread().getId());
        lock.unlock();
        log.info("[RedisAcquiredLock]: Unlocked lock successfully for Thread ID {}", Thread.currentThread().getId());
      }
    } catch (Exception e) {
      log.error("[RedisAcquiredLock]: Exception while unlocking", e);
    }
  }

  @Override
  public void close() {
    release();
  }
}
