/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface SshSessionConfigMapperFields {
  String getAccountId();
  String getExecutionId();
  String getHost();
  String getWorkingDirectory();
  SSHKeySpecDTO getSshKeySpecDTO();
  List<EncryptedDataDetail> getEncryptionDetails();
}
