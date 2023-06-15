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
  boolean isSentinelMode;

  @Override
  public void release() {
    try {
      if (isSentinelMode) {
        unlockAsync();
      } else {
        unlock();
      }
    } catch (Exception ex) {
      log.error(" Received a exception while releasing Redis lock ", ex);
    }
  }

  private void unlockAsync() {
    log.debug("[RedisSentinelMode]: Trying Async unlocked");
    if (lock != null) {
      lock.unlockAsync();
    }
    log.debug("[RedisSentinelMode]: Async unlocked successfully");
  }

  private void unlock() {
    if (lock != null && ((lock.isLocked() && lock.isHeldByCurrentThread()) || isLeaseInfinite)) {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    release();
  }
}
