package io.harness.queueservice.infc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateCapacity;
import io.harness.queueservice.impl.DelegateCapacityManagementServiceImpl;

import com.google.inject.ImplementedBy;

@ImplementedBy(DelegateCapacityManagementServiceImpl.class)
@OwnedBy(HarnessTeam.DEL)
public interface DelegateCapacityManagementService {
  DelegateCapacity getDelegateCapacity(String delegateId, String accountId);
  DelegateCapacity getDelegateCapacity(Delegate delegate);
  void registerDelegateCapacity(String accountId, String delegateId, DelegateCapacity delegateCapacity);
  String getDefaultCapacityForTaskGroup(TaskType taskType);
  boolean hasCapacity(Delegate delegate);
}
