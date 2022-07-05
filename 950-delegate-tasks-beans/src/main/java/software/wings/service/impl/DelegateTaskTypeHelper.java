package software.wings.service.impl;

import io.harness.delegate.TaskDetailsV2;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class DelegateTaskTypeHelper {
  @Inject Map<TaskType, TaskDetailsV2> taskTypeTaskDetailsMap;

  public Class<? extends TaskParameters> getRequest(TaskType taskType) {
    TaskDetailsV2 taskDetails = taskTypeTaskDetailsMap.get(taskType);
    if (taskDetails != null) {
      return taskDetails.getTaskRequest();
    }
    throw new InvalidRequestException(String.format("Task %s is not supported in new framework", taskType));
  }
  public Class<? extends DelegateResponseData> getResponse(TaskType taskType) {
    TaskDetailsV2 taskDetails = taskTypeTaskDetailsMap.get(taskType);
    if (taskDetails != null) {
      return taskDetails.getTaskResponse();
    }
    throw new InvalidRequestException(String.format("Task %s is not supported in new framework", taskType));
  }

  public boolean isUnsupported(TaskType taskType) {
    TaskDetailsV2 taskDetails = taskTypeTaskDetailsMap.get(taskType);
    if (taskDetails != null) {
      return taskDetails.isUnsupported();
    }
    return false;
  }
}
