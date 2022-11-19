/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.client;

import io.harness.k8s.model.K8sLogStreamingDTO;
import io.harness.k8s.model.K8sSteadyStateDTO;

public interface K8sClient {
  boolean performSteadyStateCheck(K8sSteadyStateDTO steadyStateDTO) throws Exception;
  boolean streamLogs(K8sLogStreamingDTO k8SLogStreamingDTO) throws Exception;
}
