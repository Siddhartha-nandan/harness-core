/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.queueservice.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.queueservice.infc.DelegateResourceCriteria;

import software.wings.beans.TaskType;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class FilterByDelegateHealthCriteria implements DelegateResourceCriteria {
  @Override
  public List<Delegate> getFilteredEligibleDelegateList(
      List<Delegate> delegateList, TaskType taskType, String accountId) {
    return delegateList;
  }

  private boolean isDelegateHealthy(Delegate delegate) {
    // TBD
    return true;
  }
}
