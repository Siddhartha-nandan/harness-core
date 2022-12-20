/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.ImplementedBy;
import org.redisson.api.RLocalCachedMap;

@OwnedBy(DEL)
@ImplementedBy(DelegateRedissonCacheManagerImpl.class)
public interface DelegateRedissonCacheManager {
  <K, V> RLocalCachedMap<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType);

  <K, V> RLocalCachedMap<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType, String keyPrefix);
}
