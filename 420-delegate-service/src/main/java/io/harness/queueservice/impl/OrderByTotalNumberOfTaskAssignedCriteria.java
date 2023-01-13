/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.queueservice.impl;

import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.TaskGroup;
import io.harness.persistence.HPersistence;
import io.harness.queueservice.infc.DelegateResourceCriteria;
import io.harness.redis.intfc.DelegateRedissonCacheManager.CounterOperation;
import io.harness.redis.intfc.DelegateServiceCache;
import io.harness.service.intfc.DelegateCache;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class OrderByTotalNumberOfTaskAssignedCriteria implements DelegateResourceCriteria {
  @Inject private HPersistence persistence;
  @Inject private DelegateCache delegateCache;
  @Inject private DelegateServiceCache delegateServiceCache;
  private final Random random = new Random();

  final Comparator<Map.Entry<String, Integer>> valueComparator = Map.Entry.comparingByValue(Comparator.naturalOrder());

  final Comparator<Delegate> delegateComparator = new Comparator<Delegate>() {
    @Override
    public int compare(Delegate d1, Delegate d2) {
      int diff = d1.getNumberOfTaskAssigned() - d2.getNumberOfTaskAssigned();
      if (diff == 0) {
        return Math.max(random.nextInt(), 0);
      }
      return diff;
    }
  };

  @Inject
  public OrderByTotalNumberOfTaskAssignedCriteria(HPersistence persistence, DelegateCache delegateCache) {
    this.persistence = persistence;
    this.delegateCache = delegateCache;
  }

  @Override
  public List<Delegate> getFilteredEligibleDelegateList(
      List<Delegate> delegateList, TaskType taskType, String accountId) {
    return listOfDelegatesSortedByNumberOfTaskAssignedFromRedis(delegateList, accountId);
  }
  private List<Delegate> listOfDelegatesSortedByNumberOfTaskAssignedFromRedis(
      List<Delegate> delegateList, String accountId) {
    delegateList.forEach(delegate
        -> delegate.setNumberOfTaskAssigned(
            delegateServiceCache.delegateTaskCacheCounter(delegate.getUuid(), CounterOperation.GET)));
    delegateList.sort(delegateComparator);
    delegateList.forEach(delegate
        -> log.info(
            "delegate Id: {}, number of current task {}", delegate.getUuid(), delegate.getNumberOfTaskAssigned()));
    return delegateList;
  }

  private List<Delegate> listOfDelegatesSortedByNumberOfTaskAssigned(
      List<Delegate> delegateList, String accountId, TaskType taskType) {
    TreeMap<String, Integer> numberOfTaskAssigned = new TreeMap<>();
    delegateList.forEach(delegate -> {
      List<DelegateTask> delegateTaskList =
          getTotalNumberOfTaskAssignedInDelegate(accountId, taskType, delegate.getUuid());
      numberOfTaskAssigned.put(delegate.getUuid(), delegateTaskList.size());
    });

    return numberOfTaskAssigned.entrySet()
        .stream()
        .sorted(valueComparator)
        .map(entry -> updateDelegateWithNumberTaskAssigned(entry, accountId))
        .collect(Collectors.toList());
  }

  private Delegate updateDelegateWithNumberTaskAssigned(Map.Entry<String, Integer> entry, String accountId) {
    Delegate delegate = getDelegateFromCache(entry.getKey(), accountId);
    if (delegate == null) {
      return null;
    }
    delegate.setNumberOfTaskAssigned(entry.getValue());
    return delegate;
  }

  public Delegate getDelegateFromCache(String delegateId, String accountId) {
    return delegateCache.get(accountId, delegateId, false);
  }

  public List<DelegateTask> getTotalNumberOfTaskAssignedInDelegate(
      String accountId, TaskType taskType, String delegateId) {
    List<DelegateTask> delegateTaskList = persistence.createQuery(DelegateTask.class)
                                              .filter(DelegateTaskKeys.accountId, accountId)
                                              .filter(DelegateTaskKeys.status, STARTED)
                                              .filter(DelegateTaskKeys.delegateId, delegateId)
                                              .project(DelegateTaskKeys.delegateId, true)
                                              .project(DelegateTaskKeys.stageId, true)
                                              .asList();
    if (taskType.getTaskGroup().equals(TaskGroup.CI)) {
      return delegateTaskList.stream()
          .filter(delegateTask -> !isEmpty(delegateTask.getStageId()))
          .filter(distinctByKey(DelegateTask::getStageId))
          .collect(Collectors.toList());
    }
    return delegateTaskList;
    // delegateServiceCache.getDelegateCache(delegateId)
  }

  public static <T> Predicate<T> distinctByKey(Function<T, Object> function) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(function.apply(t));
  }
}
