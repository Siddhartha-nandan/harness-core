/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.execution.step.StepExecutionEntity;
import io.harness.pms.contracts.execution.Status;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.Map;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDP)
public interface StepExecutionEntityRepositoryCustom {
  UpdateResult update(Scope scope, String stageExecutionId, Map<String, Object> updates);

  UpdateResult updateStatus(Scope scope, String stepExecutionId, Status status);

  DeleteResult deleteAll(Criteria criteria);

  StepExecutionEntity findByStepExecutionId(String stepExecutionId, Scope scope);
}
