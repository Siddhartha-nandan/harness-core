/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.DelegateTaskRequest.DelegateTaskRequestBuilder;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.ScmChangedFilesEvaluationTaskParams;
import io.harness.delegate.task.scm.ScmChangedFilesEvaluationTaskResponse;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@OwnedBy(HarnessTeam.CI)
public class ScmChangedFilesEvaluatorOnDelegate extends ScmChangedFilesEvaluator {
  private TaskExecutionUtils taskExecutionUtils;
  private KryoSerializer kryoSerializer;
  @Inject @Named("referenceFalseKryoSerializer") private KryoSerializer referenceFalseKryoSerializer;
  private final TaskSetupAbstractionHelper taskSetupAbstractionHelper;

  @Override
  public ScmChangedFilesEvaluationTaskResponse execute(
      FilterRequestData filterRequestData, ConnectorDetails connectorDetails, ScmConnector scmConnector) {
    ScmChangedFilesEvaluationTaskParams params =
        getScmChangedFilesEvaluationTaskParams(filterRequestData, connectorDetails, scmConnector);

    DelegateTaskRequestBuilder delegateTaskRequestBuilder =
        DelegateTaskRequest.builder()
            .accountId(filterRequestData.getAccountId())
            .taskType(TaskType.SCM_CHANGED_FILES_EVALUATION_TASK.toString())
            .taskParameters(params)
            .executionTimeout(Duration.ofMinutes(1l))
            .taskSetupAbstraction(NG, "true");

    String owner = taskSetupAbstractionHelper.getOwner(
        filterRequestData.getAccountId(), connectorDetails.getOrgIdentifier(), connectorDetails.getProjectIdentifier());
    if (isNotEmpty(owner)) {
      delegateTaskRequestBuilder.taskSetupAbstraction(OWNER, owner);
    }

    if (connectorDetails.getOrgIdentifier() != null) {
      delegateTaskRequestBuilder.taskSetupAbstraction("orgIdentifier", connectorDetails.getOrgIdentifier());
    }

    if (connectorDetails.getProjectIdentifier() != null) {
      delegateTaskRequestBuilder.taskSetupAbstraction("projectIdentifier", connectorDetails.getProjectIdentifier());
    }

    if (connectorDetails.getDelegateSelectors() != null) {
      delegateTaskRequestBuilder.taskSelectors(connectorDetails.getDelegateSelectors());
    }

    ResponseData responseData = taskExecutionUtils.executeSyncTask(delegateTaskRequestBuilder.build());

    if (BinaryResponseData.class.isAssignableFrom(responseData.getClass())) {
      BinaryResponseData binaryResponseData = (BinaryResponseData) responseData;
      Object object = binaryResponseData.isUsingKryoWithoutReference()
          ? referenceFalseKryoSerializer.asInflatedObject(binaryResponseData.getData())
          : kryoSerializer.asInflatedObject(binaryResponseData.getData());
      if (object instanceof ScmChangedFilesEvaluationTaskResponse) {
        return (ScmChangedFilesEvaluationTaskResponse) object;
      } else if (object instanceof ErrorResponseData) {
        ErrorResponseData errorResponseData = (ErrorResponseData) object;
        throw new TriggerException(
            format("Failed to fetch changed files : {%s}", errorResponseData.getErrorMessage()), WingsException.SRE);
      }
    }

    return null;
  }
}
