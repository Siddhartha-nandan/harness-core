/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.handlermapping;

import io.harness.delegate.core.beans.AcquireTasksResponse;
import io.harness.delegate.core.beans.Secret;
import io.harness.delegate.core.beans.TaskPayload;
import io.harness.delegate.service.common.AcquireTaskHelper;
import io.harness.delegate.service.handlermapping.context.Context;
import io.harness.delegate.service.handlermapping.handlers.Handler;
import io.harness.delegate.service.secret.DecryptedSecrets;
import io.harness.delegate.service.secret.RunnerDecryptionService;
import io.harness.encryption.Scope;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class HandlerMappingServer {
  private final ThreadPoolExecutor taskExecutor;
  private final AcquireTaskHelper acquireTaskHelper;
  private final Context context; // TODO: Don't inject context
  private final Map<String, Handler> handlersMap;
  private final RunnerDecryptionService decryptionService;

  @Inject
  public HandlerMappingServer(@Named("taskExecutor") ThreadPoolExecutor taskExecutor,
      AcquireTaskHelper acquireTaskHelper, Context context, Map<String, Handler> handlersMap,
      RunnerDecryptionService decryptionService) {
    this.taskExecutor = taskExecutor;
    this.acquireTaskHelper = acquireTaskHelper;
    this.context = context;
    this.handlersMap = handlersMap;
    this.decryptionService = decryptionService;
  }

  public void serve(AcquireTasksResponse acquired) {
    final TaskPayload taskPayload = acquired.getTask(0);

    var handler = handlersMap.get(taskPayload.getEventType());
    if (Objects.isNull(handler)) {
      log.error("Handler not found for event {}", taskPayload.getEventType());
      return;
    }

    var handlerContext = context.deepCopy();
    handlerContext.set(Context.TASK_ID, taskPayload.getId());
    handlerContext.set(Context.ACCOUNT_ID, taskPayload.getAccountId());
    handlerContext.set(Context.ORG_ID, taskPayload.getOrgId());
    handlerContext.set(Context.PROJECT_ID, taskPayload.getProjectId());
    // TODO: add decrypted secrets here
    // secret decryption start
    List<Secret> binarySecretsList = taskPayload.getSecretsList();
    DecryptedSecrets decryptedSecrets = new DecryptedSecrets();
    for (Secret secret : binarySecretsList) {
      try {
        var decrypted = decryptionService.decryptProtoBytes(secret);
        decryptedSecrets.addDecryptedSecret(
            IdentifierRefHelper.getIdentifierRef(Scope.fromString(secret.getSecretRef().getScope().name()),
                secret.getSecretRef().getSecretId(), taskPayload.getAccountId(), taskPayload.getOrgId(),
                taskPayload.getProjectId(), null),
            decrypted);
      } catch (Exception ex) {
        // TODO: send task failed response
        log.error("exception occurred when decrypting the secret", ex);
      }
    }

    handler.handle(taskPayload.getRunnerType(), taskPayload, decryptedSecrets, handlerContext);
    log.info("Finished executing handler {}", taskPayload.getEventType());
  }
}
