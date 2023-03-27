/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell.ssh;

import io.harness.logging.LogCallback;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.ssh.connection.ExecCommandData;
import io.harness.shell.ssh.connection.ExecResponse;
import io.harness.shell.ssh.xfer.ScpCommandData;
import io.harness.shell.ssh.xfer.ScpResponse;

// external interface; created using SshFactory
public abstract class SshClient {
  private SshSessionConfig sshSessionConfig;
  private LogCallback logCallback;

  abstract ExecResponse exec(ExecCommandData commandData);
  abstract ScpResponse scp(ScpCommandData commandData);

  abstract HSshClient getSession(SshSessionConfig sshSessionConfig, LogCallback logCallback);
  abstract void close();
  abstract void configureProxy();
  private String getKeyPath(SshSessionConfig sshSessionConfig) {
    return null;
  }
  public void init(SshSessionConfig config, LogCallback logCallback) {
    this.sshSessionConfig = config;
    this.logCallback = logCallback;
  }
}
