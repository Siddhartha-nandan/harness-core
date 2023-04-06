/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell.ssh.client.jsch;

import io.harness.shell.ssh.client.SshSession;

import com.jcraft.jsch.ChannelExec;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class JschExecSession extends SshSession {
  private ChannelExec channel;
  @Override
  public void close() throws Exception {
    channel.disconnect();
  }
}
